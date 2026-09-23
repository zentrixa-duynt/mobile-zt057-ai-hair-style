package com.example.aihair.core.data.repository

import android.content.Context
import android.net.Uri
import com.example.aihair.BuildConfig
import com.example.aihair.R
import com.example.aihair.core.data.remote.OpenAIApi
import com.example.aihair.core.data.remote.dto.generation.GenerationState
import com.example.aihair.core.data.remote.dto.generation.GenerationConfig
import com.example.aihair.core.data.remote.dto.generation.GenerationRequest
import com.example.aihair.core.data.remote.UploadApi
import com.example.aihair.core.utils.compressImageFile
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class GenerationException(val errorState: GenerationState.GenerationError) : Exception()

@Singleton
class GenerationRepository @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val uploadApi: UploadApi,
    @ApplicationContext private val context: Context
): BaseRepository(context) {

    fun startGeneration(
        localImageUri: String,
        styleName: String?,
        isColorMode: Boolean,
        isFemale: Boolean
    ): Flow<GenerationState> = flow {
        try {
            // 1. Uploading State
            emit(GenerationState.Uploading)
            var downloadUrl: String? = null
            try {
                val file = context.compressImageFile(localImageUri) ?: throw Exception("Cannot read or compress image")
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = uploadApi.uploadImage(body)
                if (response.success && response.baseUrl != null && response.filename != null) {
                    downloadUrl = response.baseUrl + response.filename
                } else {
                    throw Exception("Upload failed: ${response.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw GenerationException(GenerationState.GenerationError.StorageError(context.getString(R.string.msg_error_unknown)))
            }

            // 2. Submitting State
            emit(GenerationState.Submitting)
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val model = remoteConfig.getString("generation_model").takeIf { it.isNotEmpty() }
                ?: "gpt-image-1.5"
            val background =
                remoteConfig.getString("generation_background").takeIf { it.isNotEmpty() }
                    ?: "opaque"
            val quality =
                remoteConfig.getString("generation_quality").takeIf { it.isNotEmpty() } ?: "low"
            val size =
                remoteConfig.getString("generation_size").takeIf { it.isNotEmpty() } ?: "1024x1536"

            val genderStr = if (isFemale) "women's" else "men's"
            val prompt = if (isColorMode) {
                "Change the person's hair color to $styleName. Keep the original face, lighting, background, and composition exactly the same."
            } else {
                "Change the person's hair style to a $genderStr $styleName. Keep the original face, lighting, background, and composition exactly the same."
            }

            val request = GenerationRequest(
                model = model,
                prompt = prompt,
                config = GenerationConfig(
                    background = background,
                    quality = quality,
                    size = size,
                    images = listOfNotNull(downloadUrl)
                )
            )

            val submitResponse = if (!BuildConfig.isProduction) {
                openAIApi.submitTaskDebug(request)
            } else {
                openAIApi.submitTaskRelease(request)
            }

            if (!submitResponse.isSuccessful || submitResponse.body()?.taskId.isNullOrEmpty()) {
                val errorMsg = submitResponse.errorBody()?.string()
                    ?: context.getString(R.string.msg_error_unknown)
                emit(
                    GenerationState.Error(
                        GenerationState.GenerationError.SubmitError(
                            submitResponse.code(),
                            errorMsg
                        )
                    )
                )
                return@flow
            }

            val taskId = submitResponse.body()!!.taskId!!

            // 3. Processing / Polling State
            emit(GenerationState.Processing)

            delay(5000) // Đợi 5s trước lần hỏi đầu tiên

            val MAX_POLL_COUNT = 30 // Giới hạn 30 lần (~150 giây)
            var pollCount = 0

            while (true) {
                if (pollCount >= MAX_POLL_COUNT) {
                    emit(
                        GenerationState.Error(
                            GenerationState.GenerationError.TimeoutError(
                                context.getString(
                                    R.string.msg_error_timeout
                                )
                            )
                        )
                    )
                    break
                }
                pollCount++

                val pollResponse = try {
                    if (!BuildConfig.isProduction) {
                        openAIApi.getTaskDebug(taskId)
                    } else {
                        openAIApi.getTaskRelease(taskId)
                    }
                } catch (e: Exception) {
                    // Network error during polling, we can retry or fail
                    e.printStackTrace()
                    null
                }

                if (pollResponse != null && pollResponse.isSuccessful) {
                    val body = pollResponse.body()
                    if (body != null) {
                        when (body.status) {
                            "SUCCESS" -> {
                                val url = body.taskResult?.url
                                if (!url.isNullOrEmpty()) {
                                    emit(GenerationState.Success(url))
                                } else {
                                    emit(
                                        GenerationState.Error(
                                            GenerationState.GenerationError.TaskFailed(
                                                context.getString(R.string.msg_error_no_result_image_url)
                                            )
                                        )
                                    )
                                }
                                break // Thoát vòng lặp
                            }

                            "FAILURE" -> {
                                emit(
                                    GenerationState.Error(
                                        GenerationState.GenerationError.TaskFailed(
                                            body.errorReason?.message
                                                ?: "Quá trình tạo ảnh thất bại"
                                        )
                                    )
                                )
                                break // Thoát vòng lặp
                            }

                            "PROCESSING", "IN_PROGRESS" -> {
                                // Vẫn đang xử lý, tiếp tục chờ
                                delay(5000)
                            }

                            else -> {
                                delay(5000)
                            }
                        }
                    } else {
                        delay(5000)
                    }
                } else if (pollResponse != null && !pollResponse.isSuccessful) {
                    val errorMsg = pollResponse.errorBody()?.string()
                        ?: context.getString(R.string.msg_error_unknown)
                    emit(
                        GenerationState.Error(
                            GenerationState.GenerationError.PollingError(
                                pollResponse.code(),
                                errorMsg
                            )
                        )
                    )
                    break // Thoát vòng lặp khi gặp lỗi HTTP
                } else {
                    // pollResponse == null, có thể do Exception (mất mạng)
                    delay(5000) // Tạm thời cứ delay và thử lại
                }
            }

        } catch (e: GenerationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw GenerationException(
                GenerationState.GenerationError.NetworkError(
                    e.message ?: context.getString(R.string.msg_error_unknown)
                )
            )
        }
    }.retryWhen { cause, attempt ->
        if (cause is GenerationException && attempt < 2) {
            val error = cause.errorState
            if (error is GenerationState.GenerationError.NetworkError || error is GenerationState.GenerationError.StorageError) {
                delay(1000)
                return@retryWhen true
            }
        }
        false
    }.catch { cause ->
        if (cause is GenerationException) {
            emit(GenerationState.Error(cause.errorState))
        } else {
            emit(GenerationState.Error(GenerationState.GenerationError.NetworkError(cause.message ?: "")))
        }
    }
}
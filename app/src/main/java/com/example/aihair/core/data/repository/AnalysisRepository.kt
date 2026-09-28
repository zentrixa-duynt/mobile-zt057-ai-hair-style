package com.example.aihair.core.data.repository

import android.content.Context
import android.net.Uri
import com.example.aihair.BuildConfig
import com.example.aihair.R
import com.example.aihair.core.data.remote.dto.analysis.AnalysisState
import com.example.aihair.core.data.remote.dto.analysis.AnalysisContent
import com.example.aihair.core.data.remote.dto.analysis.AnalysisMessage
import com.example.aihair.core.data.remote.dto.analysis.AnalysisRequest
import com.example.aihair.core.data.remote.dto.analysis.ChatCompletionChunk
import com.example.aihair.core.data.remote.dto.analysis.FaceAnalysisData
import com.example.aihair.core.data.remote.dto.analysis.ImageUrl
import com.example.aihair.core.data.remote.dto.analysis.MakeupAnalysisData
import com.example.aihair.core.data.remote.dto.analysis.OpenAIErrorResponse
import com.example.aihair.core.data.remote.OpenAIApi
import com.example.aihair.core.data.repository.BaseRepository
import com.example.aihair.core.utils.compressImageFile
import com.example.aihair.core.data.remote.UploadApi
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await
import java.lang.StringBuilder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class AnalysisException(val errorState: AnalysisState.AnalysisError) : Exception()

enum class PortraitStatus {
    VALID,
    MULTIPLE_PEOPLE,
    INVALID_FACE
}

@Singleton
class AnalysisRepository @Inject constructor(
    private val openAIApi: OpenAIApi,
    private val uploadApi: UploadApi,
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : BaseRepository(context) {

    fun startFaceAnalysis(
        localImageUri: String,
        targetLanguage: String = "Vietnamese",
        localHairStyles: List<String> = emptyList() // Pass your actual list here
    ): Flow<AnalysisState> = flow {
        val hairStylesString = if (localHairStyles.isNotEmpty()) {
            localHairStyles.joinToString(", ", "[", "]")
        } else {
            "[Blunt Bob, Textured Lob, Wolf Cut, Shag Cut, Curtain Bangs, Butterfly Cut, Pixie Cut, Dutch Braids, High Ponytails, Sleek Low Bun, Wavy Ponytail, French Bob, French Crop, Buzz Cut, Pompadour, Undercut Slick, Two Block, Middle Part, Textured Quiff, Ivy League, Messy Fringe, Wavy Medium, Caesar Cut, Modern Mullet]"
        }

        val prompt = """
            Analyze the person's face in the provided image. Focus on facial structure and hair. 
            IMPORTANT VERIFICATION: First, verify if the image is a real, standard human portrait photo. If it is a cartoon, drawing, animal, object, landscape, heavily distorted/fake face, or does not clearly show a real human face, YOU MUST RETURN exactly this JSON: {"error": "INVALID_PORTRAIT"} and stop.
            
            Otherwise, return ONLY a valid, raw JSON object (no markdown code blocks, no extra text) with the following exact keys and data types:
            
            - `hair_style` (string): 1-3 words describing the current hair style.
            - `hair_color` (string): The current hair color.
            - `face_shape` (string): The shape of the face (e.g., Oval, Round, Square, Heart, Diamond).
            - `hairstyle_fit_rate` (integer): A score from 0 to 100 representing how well the current hairstyle fits the face.
            - `golden_ratio` (double): A score from 1.0 to 5.0 evaluating facial proportion balance.
            - `golden_ratio_short_description` (string): A short sentence (1-2 lines) describing their facial proportions.
            - `chin` (string): 1-2 words describing the chin (e.g., V-shaped, Pointed, Broad).
            - `cheekbone` (string): 1-2 words describing the cheekbones (e.g., High, Prominent, Flat).
            - `temple` (string): 1-2 words describing the temples (e.g., Narrow, Wide, Hollow).
            - `apple_cheeks` (string): 1-2 words describing the apple cheeks (e.g., Plump, Flat, Full).
            - `golden_ratio_keywords` (array of strings): Exactly 5 keywords describing the face's harmony.
            - `suitable_hair_styles` (array of strings): Choose at least 5 recommended hair styles that perfectly suit this face shape. 
            
            IMPORTANT MULTI-LANGUAGE INSTRUCTION: 
            Translate ALL string values (hair_color, face_shape, golden_ratio_short_description, chin, cheekbone, temple, apple_cheeks, golden_ratio_keywords) into natural, human-friendly $targetLanguage. 
            HOWEVER, you MUST NOT translate `hair_style` and `suitable_hair_styles`. They MUST remain in English. The values in `suitable_hair_styles` MUST ONLY be exact English string matches from this predefined list: $hairStylesString.
        """.trimIndent()

        emitAllFlow(localImageUri, prompt, isFaceAnalysis = true)
    }.withRetryAndCatch()

    fun startMakeupAnalysis(
        localImageUri: String,
        targetLanguage: String = "Vietnamese"
    ): Flow<AnalysisState> = flow {
        val prompt = """
            Analyze the person's face, skin tone, and features in the provided image to provide makeup advice.
            IMPORTANT VERIFICATION: First, verify if the image is a real, standard human portrait photo. If it is a cartoon, drawing, animal, object, landscape, heavily distorted/fake face, or does not clearly show a real human face, YOU MUST RETURN exactly this JSON: {"error": "INVALID_PORTRAIT"} and stop.
            
            Otherwise, return ONLY a valid, raw JSON object (no markdown code blocks, no extra text) with the following exact keys:
            
            - `overall_makeup_score` (integer): A score from 0 to 100 evaluating the current facial harmony and makeup potential.
            - `balance_scores` (object): Provide an integer score (0-100) for each of these keys representing feature balance: `brow`, `eyes`, `skin`, `cheeks`, `lips`, `harmony`.
            - `recommendations` (object): Provide an array of 2 to 3 practical makeup advices (as short, actionable bullet points, max 15 words each) for each of these keys: `brows`, `eyes`, `nose`, `skin`, `blush`, `lips`.
            - `color_palette` (object): Provide an array of 2 to 4 suitable HEX color codes (e.g., ["#D2B48C", "#F4A460"]) for each of these makeup categories: `best_neutrals` (base/foundation), `best_blush_lip` (blush and lipstick colors), `best_eyes_shadow` (eyeshadows), `best_liner_mascara` (eyeliner and mascara).
            
            IMPORTANT MULTI-LANGUAGE INSTRUCTION: 
            Translate ALL string values inside the `recommendations` object into natural, human-friendly $targetLanguage. Do NOT translate the JSON keys themselves or the HEX color codes, only translate the advice ideas.
        """.trimIndent()

        emitAllFlow(localImageUri, prompt, isFaceAnalysis = false)
    }.withRetryAndCatch()

    suspend fun verifyPortrait(downloadUrl: String): PortraitStatus {
        val prompt = "This is a pre-validation step. If the image passes the system rules (it is exactly one real human face), return exactly `{\"status\": \"APPROVED\"}`. If it violates the rules, do not return the status, just return the specific error JSON as instructed in the system prompt."
        val (result, _) = submitAnalysisAndReadStreamWithFallback(prompt, downloadUrl)
        return when {
            result?.contains("APPROVED", ignoreCase = true) == true -> PortraitStatus.VALID
            result?.contains("MULTIPLE_PEOPLE", ignoreCase = true) == true -> PortraitStatus.MULTIPLE_PEOPLE
            else -> PortraitStatus.INVALID_FACE
        }
    }

    private suspend fun FlowCollector<AnalysisState>.emitAllFlow(
        localImageUri: String,
        prompt: String,
        isFaceAnalysis: Boolean
    ) {
        try {
            emit(AnalysisState.Uploading)
            val downloadUrl = uploadImageOrFallback(localImageUri)

            emit(AnalysisState.Submitting)
            val (jsonStr, errorInfo) = submitAnalysisAndReadStreamWithFallback(prompt, downloadUrl)
            
            if (jsonStr.isNullOrEmpty()) {
                throw AnalysisException(AnalysisState.AnalysisError.SubmitError(errorInfo.first, errorInfo.second ?: context.getString(R.string.msg_error_unknown)))
            }

            parseAndEmitResult(jsonStr, isFaceAnalysis)
        } catch (e: AnalysisException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            throw AnalysisException(AnalysisState.AnalysisError.NetworkError(e.message ?: context.getString(R.string.msg_error_unknown)))
        }
    }

    private suspend fun uploadImageOrFallback(localImageUri: String): String {
        return try {
            val file = context.compressImageFile(localImageUri) ?: throw Exception("Cannot read or compress image")
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = uploadApi.uploadImage(body)
            if (response.success && response.baseUrl != null && response.filename != null) {
                response.baseUrl + response.filename
            } else {
                throw Exception("Upload failed: ${response.message}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw AnalysisException(AnalysisState.AnalysisError.StorageError(context.getString(R.string.msg_error_unknown)))
        }
    }

    private suspend fun submitAnalysisAndReadStreamWithFallback(
        prompt: String,
        downloadUrl: String
    ): Pair<String?, Pair<Int, String?>> {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val modelsString = remoteConfig.getString("analysis_models").takeIf { it.isNotEmpty() } ?: "gpt-5-nano,gpt-4o-mini,gpt-4o"
        val modelsList = modelsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        var finalJsonResult: String? = null
        var finalErrorMsg: String? = null
        var finalErrorCode = -1

        for (modelName in modelsList) {
            val request = AnalysisRequest(
                model = modelName,
                messages = listOf(
                    AnalysisMessage(
                        role = "system",
                        content = "You are an expert image analyst. You must analyze human portrait photographs.\n" +
                                "CRITICAL RULES:\n" +
                                "1. If the image contains EXACTLY ONE real human face, YOU MUST PROCEED and follow the user's instructions. Be lenient with real photos; if it is a real person, accept it.\n" +
                                "2. If the image contains MULTIPLE people, YOU MUST REJECT it by returning exactly `{\"error\": \"MULTIPLE_PEOPLE\"}`.\n" +
                                "3. If the image is CLEARLY a cartoon, drawing, animal, object, or completely lacks a human face, YOU MUST REJECT it by returning exactly `{\"error\": \"INVALID_PORTRAIT\"}`."
                    ),
                    AnalysisMessage(
                        role = "user",
                        content = listOf(
                            AnalysisContent(type = "text", text = prompt),
                            AnalysisContent(type = "image_url", imageUrl = ImageUrl(downloadUrl))
                        )
                    )
                )
            )

            val submitResponse = if (!BuildConfig.isProduction) {
                openAIApi.submitAnalysisStreamDebug(request)
            } else {
                openAIApi.submitAnalysisStreamRelease(request)
            }

            if (submitResponse.isSuccessful) {
                val body = submitResponse.body()
                if (body != null) {
                    try {
                        val streamResult = readStreamContent(body)
                        if (streamResult.isNotEmpty()) {
                            // Validate if the JSON is complete and valid before accepting it
                            gson.fromJson(streamResult, com.google.gson.JsonObject::class.java)
                            finalJsonResult = streamResult
                            break
                        } else {
                            finalErrorMsg = context.getString(R.string.msg_error_empty_response)
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: ""
                        finalErrorMsg = errorMsg
                        if (errorMsg.contains("unavailable", ignoreCase = true) || 
                            errorMsg.contains("rate limit", ignoreCase = true) ||
                            errorMsg.contains("Stream ended prematurely", ignoreCase = true)) {
                            continue
                        } else {
                            break
                        }
                    }
                }
            } else {
                val errorBodyString = submitResponse.errorBody()?.string()
                val errorMsg = try {
                    val errorResp = gson.fromJson(errorBodyString, OpenAIErrorResponse::class.java)
                    val rawMsg = errorResp?.error?.message ?: errorBodyString ?: context.getString(R.string.msg_error_unknown)
                    if (rawMsg.contains("temporarily unavailable", ignoreCase = true) ||
                        rawMsg.contains("not available", ignoreCase = true) ||
                        rawMsg.contains("rate limit", ignoreCase = true)) {
                        context.getString(R.string.msg_error_server_busy)
                    } else {
                        rawMsg
                    }
                } catch (e: Exception) {
                    errorBodyString ?: context.getString(R.string.msg_error_unknown)
                }

                finalErrorCode = submitResponse.code()
                finalErrorMsg = errorMsg

                if (errorMsg == context.getString(R.string.msg_error_server_busy)) {
                    continue
                } else {
                    break
                }
            }
        }
        return Pair(finalJsonResult, Pair(finalErrorCode, finalErrorMsg))
    }

    private fun readStreamContent(responseBody: okhttp3.ResponseBody): String {
        val stringBuilder = StringBuilder()
        var isDone = false
        responseBody.byteStream().bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.startsWith("data: ")) {
                    val dataStr = line.substring(6).trim()
                    if (dataStr == "[DONE]") {
                        isDone = true
                        break
                    }
                    if (dataStr.contains("\"error\":")) {
                        try {
                            val errorResp = gson.fromJson(dataStr, OpenAIErrorResponse::class.java)
                            throw Exception(errorResp?.error?.message ?: "Stream API Error")
                        } catch (e: Exception) {
                            if (e.message != null && e.message != "Stream API Error") {
                                throw e
                            }
                        }
                    }
                    try {
                        val chunk = gson.fromJson(dataStr, ChatCompletionChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            stringBuilder.append(content)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (!isDone) {
            throw Exception("Stream ended prematurely without [DONE] marker.")
        }
        val rawResult = stringBuilder.toString()
        val startIndex = rawResult.indexOf('{')
        val endIndex = rawResult.lastIndexOf('}')
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return rawResult.substring(startIndex, endIndex + 1)
        }
        return rawResult.replace("```json", "").replace("```", "").trim()
    }

    private suspend fun FlowCollector<AnalysisState>.parseAndEmitResult(
        jsonStr: String,
        isFaceAnalysis: Boolean
    ) {
        if (jsonStr.isNotEmpty()) {
            if (jsonStr.contains("MULTIPLE_PEOPLE", ignoreCase = true)) {
                emit(AnalysisState.Error(AnalysisState.AnalysisError.ParseError(context.getString(R.string.msg_error_multiple_people))))
                return
            }
            if (jsonStr.contains("no image", ignoreCase = true) ||
                jsonStr.contains("no face", ignoreCase = true) ||
                jsonStr.contains("not visible", ignoreCase = true) ||
                jsonStr.contains("INVALID_PORTRAIT", ignoreCase = true)) {
                emit(AnalysisState.Error(AnalysisState.AnalysisError.ParseError(context.getString(R.string.msg_error_no_face))))
                return
            }

            try {
                if (isFaceAnalysis) {
                    val data = gson.fromJson(jsonStr, FaceAnalysisData::class.java)
                    emit(AnalysisState.FaceSuccess(data))
                } else {
                    val data = gson.fromJson(jsonStr, MakeupAnalysisData::class.java)
                    emit(AnalysisState.MakeupSuccess(data))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emit(AnalysisState.Error(AnalysisState.AnalysisError.ParseError(context.getString(R.string.msg_error_parse, e.message))))
            }
        } else {
            emit(AnalysisState.Error(AnalysisState.AnalysisError.TaskFailed(context.getString(R.string.msg_error_no_result_image_url))))
        }
    }

    private fun Flow<AnalysisState>.withRetryAndCatch(): Flow<AnalysisState> = this
        .retryWhen { cause, attempt ->
            if (cause is AnalysisException && attempt < 2) {
                val error = cause.errorState
                if (error is AnalysisState.AnalysisError.NetworkError || error is AnalysisState.AnalysisError.StorageError) {
                    kotlinx.coroutines.delay(1000)
                    return@retryWhen true
                }
            }
            false
        }.catch { cause ->
            if (cause is AnalysisException) {
                emit(AnalysisState.Error(cause.errorState))
            } else {
                emit(AnalysisState.Error(AnalysisState.AnalysisError.NetworkError(cause.message ?: "")))
            }
        }
}
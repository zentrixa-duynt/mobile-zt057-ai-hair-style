import re

with open('app/src/main/java/com/example/aihair/core/data/repository/AnalysisRepository.kt', 'r') as f:
    content = f.read()

# We want to replace the body of emitAllFlow and add the helper functions below it.
# The `emitAllFlow` function starts around `private suspend fun kotlinx.coroutines.flow.FlowCollector<AnalysisState>.emitAllFlow(`
# We'll just replace the whole function block and append the new ones inside the class.

new_functions = """    private suspend fun FlowCollector<AnalysisState>.emitAllFlow(
        localImageUri: String,
        prompt: String,
        isFaceAnalysis: Boolean
    ) {
        try {
            emit(AnalysisState.Uploading)
            val downloadUrl = uploadImageOrFallback(localImageUri)
            if (downloadUrl == null) {
                emit(AnalysisState.Error(AnalysisState.AnalysisError.StorageError(context.getString(R.string.msg_error_unknown))))
                return
            }

            emit(AnalysisState.Submitting)
            val (submitResponse, errorInfo) = submitAnalysisWithFallback(prompt, downloadUrl)
            if (submitResponse == null || !submitResponse.isSuccessful) {
                emit(AnalysisState.Error(AnalysisState.AnalysisError.SubmitError(errorInfo.first, errorInfo.second ?: context.getString(R.string.msg_error_unknown))))
                return
            }

            emit(AnalysisState.Processing)
            val responseBody = submitResponse.body()
            if (responseBody == null) {
                emit(AnalysisState.Error(AnalysisState.AnalysisError.TaskFailed(context.getString(R.string.msg_error_empty_response))))
                return
            }

            val jsonStr = readStreamContent(responseBody)
            parseAndEmitResult(jsonStr, isFaceAnalysis)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(AnalysisState.Error(AnalysisState.AnalysisError.NetworkError(e.message ?: context.getString(R.string.msg_error_unknown))))
        }
    }

    private suspend fun uploadImageOrFallback(localImageUri: String): String? {
        return try {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("analysis_images/${UUID.randomUUID()}.jpg")
            imageRef.putFile(Uri.parse(localImageUri)).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=1024&auto=format&fit=crop"
        }
    }

    private suspend fun submitAnalysisWithFallback(
        prompt: String,
        downloadUrl: String
    ): Pair<retrofit2.Response<okhttp3.ResponseBody>?, Pair<Int, String?>> {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val modelsString = remoteConfig.getString("analysis_models").takeIf { it.isNotEmpty() } ?: "gpt-4o-mini,gpt-4o"
        val modelsList = modelsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        var finalSubmitResponse: retrofit2.Response<okhttp3.ResponseBody>? = null
        var finalErrorMsg: String? = null
        var finalErrorCode = -1

        for (modelName in modelsList) {
            val request = AnalysisRequest(
                model = modelName,
                messages = listOf(
                    AnalysisMessage(
                        role = "system",
                        content = "You are an expert beauty analyst and makeup artist assistant."
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
                finalSubmitResponse = submitResponse
                break
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
        return Pair(finalSubmitResponse, Pair(finalErrorCode, finalErrorMsg))
    }

    private fun readStreamContent(responseBody: okhttp3.ResponseBody): String {
        val stringBuilder = StringBuilder()
        responseBody.byteStream().bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.startsWith("data: ")) {
                    val dataStr = line.substring(6).trim()
                    if (dataStr == "[DONE]") {
                        break
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
        return stringBuilder.toString().replace("```json", "").replace("```", "").trim()
    }

    private suspend fun FlowCollector<AnalysisState>.parseAndEmitResult(
        jsonStr: String,
        isFaceAnalysis: Boolean
    ) {
        if (jsonStr.isNotEmpty()) {
            if (jsonStr.contains("no image", ignoreCase = true) ||
                jsonStr.contains("no face", ignoreCase = true) ||
                jsonStr.contains("not visible", ignoreCase = true)) {
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
}"""

import re
# Regex to match the emitAllFlow function body
pattern = re.compile(r'    private suspend fun kotlinx\.coroutines\.flow\.FlowCollector<AnalysisState>\.emitAllFlow\(.*?\)\s*\{.*?\n\}', re.DOTALL)
pattern_alt = re.compile(r'    private suspend fun FlowCollector<AnalysisState>\.emitAllFlow\(.*?\)\s*\{.*?\n\}', re.DOTALL)

match = pattern.search(content) or pattern_alt.search(content)

if match:
    # Also we need to replace the last closing bracket of the class because the new string includes it
    new_content = content[:match.start()] + new_functions
    # The new_functions already contains the closing bracket of the class
    # but wait, let's remove `\n}` from `new_functions` and keep the original closing bracket
    # Actually, the original file might have other things at the end.
    
    with open('app/src/main/java/com/example/aihair/core/data/repository/AnalysisRepository.kt', 'w') as f:
        # replace the function, keeping the rest of the file
        # wait, if I put `\n}` at the end of new_functions, I need to make sure I consume the class's closing bracket
        # Let's just strip the last `\n}` from new_functions
        final_new_func = new_functions.rsplit('}', 1)[0]
        # and insert it
        f.write(content[:match.start()] + final_new_func + content[match.end()-1:])
    print("Success")
else:
    print("Failed to find emitAllFlow")

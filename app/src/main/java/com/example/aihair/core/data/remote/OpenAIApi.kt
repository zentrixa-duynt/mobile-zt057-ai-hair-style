package com.example.aihair.core.data.remote

import com.example.aihair.core.data.remote.dto.analysis.AnalysisRequest
import com.example.aihair.core.data.remote.dto.generation.GenerationRequest
import com.example.aihair.core.data.remote.dto.generation.GenerationResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface OpenAIApi {

    @POST("/task/submit")
    suspend fun submitTaskDebug(
        @Body request: GenerationRequest
    ): Response<GenerationResponse>

    @POST("/firebase/appcheck/task/submit")
    suspend fun submitTaskRelease(
        @Body request: GenerationRequest
    ): Response<GenerationResponse>

    @POST("/task/submit")
    suspend fun submitAnalysisTaskDebug(
        @Body request: AnalysisRequest
    ): Response<GenerationResponse>

    @POST("/firebase/appcheck/task/submit")
    suspend fun submitAnalysisTaskRelease(
        @Body request: AnalysisRequest
    ): Response<GenerationResponse>

    @Streaming
    @POST("/chat/completions?stream=true")
    suspend fun submitAnalysisStreamDebug(
        @Body request: AnalysisRequest
    ): Response<ResponseBody>

    @Streaming
    @POST("/firebase/appcheck/chat/completions?stream=true")
    suspend fun submitAnalysisStreamRelease(
        @Body request: AnalysisRequest
    ): Response<ResponseBody>

    @GET("/task/{taskId}")
    suspend fun getTaskDebug(
        @Path("taskId") taskId: String
    ): Response<GenerationResponse>

    @GET("/firebase/appcheck/task/{taskId}")
    suspend fun getTaskRelease(
        @Path("taskId") taskId: String
    ): Response<GenerationResponse>
}
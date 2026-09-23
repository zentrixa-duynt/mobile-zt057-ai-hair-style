package com.example.aihair.di

import com.example.aihair.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response

class AppCheckInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // Only append headers for production build
        if (BuildConfig.isProduction) {
            try {
                // Get the App Check token synchronously (blocking OkHttp's background thread)
                val tokenResult = runBlocking {
                    FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                }

                val token = tokenResult.token

                // Get the Firebase Project ID from options
                val projectId = FirebaseApp.getInstance().options.projectId ?: ""

                if (token.isNotEmpty()) {
                    request = request.newBuilder()
                        .addHeader("X-Firebase-AppCheck", token)
                        .addHeader("X-Firebase-ProjectID", projectId)
                        .build()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return chain.proceed(request)
    }
}
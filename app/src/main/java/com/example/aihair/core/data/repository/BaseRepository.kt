package com.example.aihair.core.data.repository

import android.content.Context
import com.example.aihair.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

abstract class BaseRepository(private val context: Context) {
    /**
     * Executes an API call in the IO dispatcher and handles common network exceptions.
     * The lambda receives a CoroutineScope to allow concurrent operations like async {}.
     */
    suspend fun <T> safeApiCall(apiCall: suspend CoroutineScope.() -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(apiCall())
            } catch (e: HttpException) {
                // Handle HTTP errors specifically (e.g. 401, 404, 500)
                Result.failure(Exception(context.getString(R.string.msg_error_server, e.code(), e.message())))
            } catch (e: IOException) {
                // Handle network failures (no internet, timeout)
                Result.failure(Exception(context.getString(R.string.msg_error_network_short)))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Handle any other unknown errors
                Result.failure(e)
            }
        }
    }

    /**
     * Executes a Local Database/Cache call in the IO dispatcher and handles exceptions.
     */
    suspend fun <T> safeLocalCall(localCall: suspend CoroutineScope.() -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                Result.success(localCall())
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }
}

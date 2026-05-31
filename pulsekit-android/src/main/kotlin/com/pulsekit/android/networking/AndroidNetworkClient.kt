package com.pulsekit.android.networking

import com.pulsekit.android.lifecycle.PulseKitLifecycleObserver
import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.logging.PulseKitLogger
import com.pulsekit.core.api.networking.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal class AndroidNetworkClient(
    private val config: PulseKitConfig,
) : NetworkClient {

    override suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        if (!PulseKitLifecycleObserver.isAppInForeground()) {
            throw IllegalStateException("App is in background — skip network call")
        }
        val endpoint = "${config.baseUrl.trimEnd('/')}$url"
        try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            config.apiKey?.let { key ->
                connection.setRequestProperty("Authorization", "Bearer $key")
            }
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw RuntimeException("Flag fetch failed: $errorBody")
            }
        } catch (e: Exception) {
            if (config.enableDebugLogging) {
                PulseKitLogger.log("PulseKit.Network", "GET $url failed: ${e.message}")
            }
            throw e
        }
    }

    override suspend fun post(url: String, body: String): String = withContext(Dispatchers.IO) {
        if (!PulseKitLifecycleObserver.isAppInForeground()) {
            throw IllegalStateException("App is in background — skip network call")
        }
        val endpoint = "${config.baseUrl.trimEnd('/')}$url"
        try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            config.apiKey?.let { key ->
                connection.setRequestProperty("Authorization", "Bearer $key")
            }
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw RuntimeException("POST failed: $errorBody")
            }
        } catch (e: Exception) {
            if (config.enableDebugLogging) {
                PulseKitLogger.log("PulseKit.Network", "POST $url failed: ${e.message}")
            }
            throw e
        }
    }
}

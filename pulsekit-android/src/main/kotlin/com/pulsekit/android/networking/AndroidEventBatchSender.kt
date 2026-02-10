package com.pulsekit.android.networking

import com.pulsekit.core.api.config.PulseKitConfig
import com.pulsekit.core.api.logging.PulseKitLogger
import com.pulsekit.core.api.networking.EventBatchSender
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of [EventBatchSender] using HttpURLConnection.
 * POSTs batch JSON to config.baseUrl + /v1/events.
 */
internal class AndroidEventBatchSender(
    private val config: PulseKitConfig
) : EventBatchSender {

    override suspend fun sendBatch(jsonPayload: String): Boolean = withContext(Dispatchers.IO) {
        val endpoint = "${config.baseUrl.trimEnd('/')}/v1/events"
        try {
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            config.apiKey?.let { key ->
                conn.setRequestProperty("Authorization", "Bearer $key")
            }
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val ok = code in 200..299
            if (config.enableDebugLogging) {
                PulseKitLogger.log("PulseKit", "Batch send: $code (${if (ok) "ok" else "fail"})")
            }
            ok
        } catch (e: Exception) {
            if (config.enableDebugLogging) {
                PulseKitLogger.log("PulseKit", "Batch send error: ${e.message}")
            }
            false
        }
    }
}

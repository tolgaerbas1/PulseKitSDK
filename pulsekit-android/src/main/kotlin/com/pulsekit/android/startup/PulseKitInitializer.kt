package com.pulsekit.android.startup

import android.content.Context
import androidx.startup.Initializer
import com.pulsekit.android.PulseKitAndroid
import com.pulsekit.core.api.config.PulseKitConfig

/**
 * AndroidX Startup initializer for automatic PulseKit initialization.
 * 
 * To enable auto-initialization, add this to your AndroidManifest.xml:
 * ```xml
 * <meta-data
 *     android:name="com.pulsekit.android.startup.PulseKitInitializer"
 *     android:value="androidx.startup" />
 * ```
 * 
 * You can also customize the configuration by providing metadata:
 * ```xml
 * <meta-data
 *     android:name="com.pulsekit.android.startup.PulseKitInitializer"
 *     android:value="androidx.startup">
 *     <meta-data
 *         android:name="com.pulsekit.api_key"
 *         android:value="your-api-key" />
 *     <meta-data
 *         android:name="com.pulsekit.enable_debug"
 *         android:value="true" />
 * </meta-data>
 * ```
 */
public class PulseKitInitializer : Initializer<Unit> {
    
    override fun create(context: Context) {
        // Extract configuration from manifest metadata
        val config = extractConfigFromManifest(context)
        
        // Initialize PulseKit with the extracted configuration
        PulseKitAndroid.initialize(context, config)
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies on other initializers
        return emptyList()
    }
    
    private fun extractConfigFromManifest(context: Context): PulseKitConfig {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        
        val metaData = appInfo.metaData ?: return PulseKitConfig()
        
        val configBuilder = com.pulsekit.core.api.config.PulseKitConfigBuilder()
        
        // Extract API key
        val apiKey = metaData.getString("com.pulsekit.api_key")
        if (apiKey != null) {
            configBuilder.apiKey = apiKey
        }
        
        // Extract debug flag
        val enableDebug = metaData.getBoolean("com.pulsekit.enable_debug", false)
        configBuilder.enableDebugLogging = enableDebug
        
        // Extract base URL
        val baseUrl = metaData.getString("com.pulsekit.base_url")
        if (baseUrl != null) {
            configBuilder.baseUrl = baseUrl
        }
        
        // Extract queue size
        val maxQueueSize = metaData.getInt("com.pulsekit.max_queue_size", -1)
        if (maxQueueSize > 0) {
            configBuilder.maxQueueSize = maxQueueSize
        }
        
        return configBuilder.build()
    }
}

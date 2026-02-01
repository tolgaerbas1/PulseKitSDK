package com.pulsekit.core.api.storage

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic database driver interface.
 * 
 * This allows for different implementations on different platforms
 * (JVM, Android, iOS, etc.) while maintaining a common API.
 */
internal interface DatabaseDriver {
    
    /**
     * Initialize the database and create schema if needed.
     */
    suspend fun initialize()
    
    /**
     * Close the database and clean up resources.
     */
    suspend fun close()
    
    /**
     * Insert a new event into the database.
     * 
     * @param event The event to insert
     * @return The inserted event with generated ID
     */
    suspend fun insertEvent(event: StoredEvent): StoredEvent
    
    /**
     * Get events by their IDs.
     * 
     * @param eventIds List of event IDs to retrieve
     * @return List of stored events
     */
    suspend fun getEventsByIds(eventIds: List<String>): List<StoredEvent>
    
    /**
     * Get a batch of events for processing.
     * 
     * @param limit Maximum number of events to return
     * @param excludeExpired Whether to exclude expired events
     * @return List of events ordered by queue time
     */
    suspend fun getEventBatch(limit: Int, excludeExpired: Boolean = true): List<StoredEvent>
    
    /**
     * Delete events by their IDs.
     * 
     * @param eventIds List of event IDs to delete
     * @return Number of events deleted
     */
    suspend fun deleteEvents(eventIds: List<String>): Int
    
    /**
     * Update retry count for events.
     * 
     * @param eventIds List of event IDs to update
     * @param retryCount New retry count
     * @return Number of events updated
     */
    suspend fun updateRetryCount(eventIds: List<String>, retryCount: Int): Int
    
    /**
     * Delete expired events.
     * 
     * @param currentTime Current time to check against
     * @return Number of events deleted
     */
    suspend fun deleteExpiredEvents(currentTime: kotlinx.datetime.Instant): Int
    
    /**
     * Get total count of events in database.
     * 
     * @param excludeExpired Whether to exclude expired events from count
     * @return Total event count
     */
    suspend fun getEventCount(excludeExpired: Boolean = true): Int
    
    /**
     * Get database statistics.
     * 
     * @return Database statistics
     */
    suspend fun getDatabaseStats(): DatabaseStats
    
    /**
     * Observe changes to the event count.
     * 
     * @return Flow of event count updates
     */
    fun observeEventCount(): Flow<Int>
    
    /**
     * Clear all events from the database.
     */
    suspend fun clearAllEvents()
}

/**
 * Database statistics for monitoring and debugging.
 */
internal data class DatabaseStats(
    val totalEvents: Int,
    val expiredEvents: Int,
    val databaseSizeBytes: Long,
    val oldestEventAge: kotlinx.datetime.Duration?,
    val newestEventAge: kotlinx.datetime.Duration?,
    val averageRetryCount: Double
)

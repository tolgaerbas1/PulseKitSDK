package com.pulsekit.core.api.storage

import kotlinx.datetime.Instant
import java.util.Collections.emptyList
import kotlin.to

/**
 * Database schema for PulseKit event persistence.
 * 
 * This defines the structure for storing events in SQLite database
 * with support for cross-platform usage.
 */
internal object DatabaseSchema {
    
    const val DATABASE_NAME = "pulsekit_events.db"
    const val DATABASE_VERSION = 1
    
    // Events table
    object EventsTable {
        const val TABLE_NAME = "events"
        
        const val COLUMN_ID = "id"
        const val COLUMN_EVENT_ID = "event_id"
        const val COLUMN_EVENT_TYPE = "event_type"
        const val COLUMN_EVENT_DATA = "event_data"
        const val COLUMN_QUEUED_AT = "queued_at"
        const val COLUMN_RETRY_COUNT = "retry_count"
        const val COLUMN_EXPIRES_AT = "expires_at"
        
        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVENT_ID TEXT UNIQUE NOT NULL,
                $COLUMN_EVENT_TYPE TEXT NOT NULL,
                $COLUMN_EVENT_DATA TEXT NOT NULL,
                $COLUMN_QUEUED_AT INTEGER NOT NULL,
                $COLUMN_RETRY_COUNT INTEGER DEFAULT 0,
                $COLUMN_EXPIRES_AT INTEGER NOT NULL
            )
        """
        
        const val CREATE_INDEX_EVENT_ID = """
            CREATE INDEX idx_event_id ON $TABLE_NAME($COLUMN_EVENT_ID)
        """
        
        const val CREATE_INDEX_QUEUED_AT = """
            CREATE INDEX idx_queued_at ON $TABLE_NAME($COLUMN_QUEUED_AT)
        """
        
        const val CREATE_INDEX_EXPIRES_AT = """
            CREATE INDEX idx_expires_at ON $TABLE_NAME($COLUMN_EXPIRES_AT)
        """
        
        const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
    
    /**
     * Migration definitions for database version upgrades.
     */
    object Migrations {

    }
}

/**
 * Represents a stored event in the database.
 */
internal data class StoredEvent(
    val id: Long = 0,
    val eventId: String,
    val eventType: String,
    val eventData: String, // JSON serialized event
    val queuedAt: Instant,
    val retryCount: Int = 0,
    val expiresAt: Instant
)

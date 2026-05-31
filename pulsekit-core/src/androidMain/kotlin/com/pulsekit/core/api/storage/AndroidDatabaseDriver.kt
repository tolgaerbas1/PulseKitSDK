package com.pulsekit.core.api.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Android implementation of DatabaseDriver using Android's SQLite APIs.
 *
 * This implementation uses Android's built-in SQLite support for optimal
 * performance and battery efficiency on Android devices.
 */
internal class AndroidDatabaseDriver(
    private val context: Context,
) : DatabaseDriver {

    private var dbHelper: DatabaseHelper? = null
    private var database: SQLiteDatabase? = null
    private val _eventCountFlow = MutableSharedFlow<Int>(replay = 1)
    private val eventCountFlow = _eventCountFlow.asSharedFlow()

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            dbHelper = DatabaseHelper(context)
            database = dbHelper?.writableDatabase
            emitEventCount()
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            dbHelper?.close()
            dbHelper = null
            database = null
        }
    }

    override suspend fun insertEvent(event: StoredEvent): StoredEvent {
        return withContext(Dispatchers.IO) {
            val db = requireDatabase()

            val values = android.content.ContentValues().apply {
                put(DatabaseSchema.EventsTable.COLUMN_EVENT_ID, event.eventId)
                put(DatabaseSchema.EventsTable.COLUMN_EVENT_TYPE, event.eventType)
                put(DatabaseSchema.EventsTable.COLUMN_EVENT_DATA, event.eventData)
                put(DatabaseSchema.EventsTable.COLUMN_QUEUED_AT, event.queuedAt.epochSeconds)
                put(DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT, event.retryCount)
                put(DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT, event.expiresAt.epochSeconds)
            }

            val id = db.insert(DatabaseSchema.EventsTable.TABLE_NAME, null, values)
            if (id == -1L) {
                throw IllegalStateException("Failed to insert event into database")
            }

            emitEventCount()
            event.copy(id = id)
        }
    }

    override suspend fun getEventsByIds(eventIds: List<String>): List<StoredEvent> {
        return withContext(Dispatchers.IO) {
            if (eventIds.isEmpty()) return@withContext emptyList()

            val db = requireDatabase()
            val selection = "${DatabaseSchema.EventsTable.COLUMN_EVENT_ID} IN (${eventIds.joinToString(",") { "?" }})"
            val selectionArgs = eventIds.toTypedArray()
            val sortOrder = "${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT} ASC"

            db.query(
                DatabaseSchema.EventsTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
            ).use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { mapCursorToStoredEvent(it) }
                    .toList()
            }
        }
    }

    override suspend fun getEventBatch(limit: Int, excludeExpired: Boolean): List<StoredEvent> {
        return withContext(Dispatchers.IO) {
            val db = requireDatabase()

            val (selection, selectionArgs) = if (excludeExpired) {
                "${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} > ?" to arrayOf(Clock.System.now().epochSeconds.toString())
            } else {
                null to null
            }

            val sortOrder = "${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT} ASC LIMIT $limit"

            db.query(
                DatabaseSchema.EventsTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
            ).use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor else null }
                    .map { mapCursorToStoredEvent(it) }
                    .toList()
            }
        }
    }

    override suspend fun deleteEvents(eventIds: List<String>): Int {
        return withContext(Dispatchers.IO) {
            if (eventIds.isEmpty()) return@withContext 0

            val db = requireDatabase()
            val whereClause = "${DatabaseSchema.EventsTable.COLUMN_EVENT_ID} IN (${eventIds.joinToString(",") { "?" }})"
            val whereArgs = eventIds.toTypedArray()

            val deletedCount = db.delete(DatabaseSchema.EventsTable.TABLE_NAME, whereClause, whereArgs)
            emitEventCount()
            deletedCount
        }
    }

    override suspend fun updateRetryCount(eventIds: List<String>, retryCount: Int): Int {
        return withContext(Dispatchers.IO) {
            if (eventIds.isEmpty()) return@withContext 0

            val db = requireDatabase()
            val values = android.content.ContentValues().apply {
                put(DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT, retryCount)
            }
            val whereClause = "${DatabaseSchema.EventsTable.COLUMN_EVENT_ID} IN (${eventIds.joinToString(",") { "?" }})"
            val whereArgs = eventIds.toTypedArray()

            db.update(DatabaseSchema.EventsTable.TABLE_NAME, values, whereClause, whereArgs)
        }
    }

    override suspend fun deleteExpiredEvents(currentTime: Instant): Int {
        return withContext(Dispatchers.IO) {
            val db = requireDatabase()
            val whereClause = "${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} <= ?"
            val whereArgs = arrayOf(currentTime.epochSeconds.toString())

            val deletedCount = db.delete(DatabaseSchema.EventsTable.TABLE_NAME, whereClause, whereArgs)
            emitEventCount()
            deletedCount
        }
    }

    override suspend fun getEventCount(excludeExpired: Boolean): Int {
        return withContext(Dispatchers.IO) {
            val db = requireDatabase()

            val (selection, selectionArgs) = if (excludeExpired) {
                "${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} > ?" to arrayOf(Clock.System.now().epochSeconds.toString())
            } else {
                null to null
            }

            db.query(
                DatabaseSchema.EventsTable.TABLE_NAME,
                arrayOf("COUNT(*)"),
                selection,
                selectionArgs,
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getInt(0)
                } else {
                    0
                }
            }
        }
    }

    override suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            val db = requireDatabase()
            val now = Clock.System.now()

            // Get total count
            val totalEvents = getEventCount(false)

            // Get expired count
            val expiredCount = db.query(
                DatabaseSchema.EventsTable.TABLE_NAME,
                arrayOf("COUNT(*)"),
                "${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} <= ?",
                arrayOf(now.epochSeconds.toString()),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }

            // Get age statistics
            val ageStats = db.query(
                DatabaseSchema.EventsTable.TABLE_NAME,
                arrayOf(
                    "MIN(${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}) as oldest",
                    "MAX(${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}) as newest",
                    "AVG(CAST(${DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT} AS REAL)) as avg_retry",
                ),
                null,
                null,
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val oldestEpoch = cursor.getLong(cursor.getColumnIndexOrThrow("oldest"))
                    val newestEpoch = cursor.getLong(cursor.getColumnIndexOrThrow("newest"))
                    val avgRetry = if (cursor.isNull(cursor.getColumnIndexOrThrow("avg_retry"))) {
                        0.0
                    } else {
                        cursor.getDouble(cursor.getColumnIndexOrThrow("avg_retry"))
                    }

                    val oldestAge = if (oldestEpoch > 0) now - Instant.fromEpochSeconds(oldestEpoch) else null
                    val newestAge = if (newestEpoch > 0) now - Instant.fromEpochSeconds(newestEpoch) else null

                    Triple(oldestAge, newestAge, avgRetry)
                } else {
                    Triple(null, null, 0.0)
                }
            }

            // Get database size
            val databasePath = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
            val databaseSize = if (databasePath.exists()) {
                databasePath.length()
            } else {
                0L
            }

            DatabaseStats(
                totalEvents = totalEvents,
                expiredEvents = expiredCount,
                databaseSizeBytes = databaseSize,
                oldestEventAge = ageStats.first,
                newestEventAge = ageStats.second,
                averageRetryCount = ageStats.third,
            )
        }
    }

    override fun observeEventCount(): Flow<Int> = eventCountFlow

    override suspend fun clearAllEvents() {
        withContext(Dispatchers.IO) {
            val db = requireDatabase()
            db.delete(DatabaseSchema.EventsTable.TABLE_NAME, null, null)
            emitEventCount()
        }
    }

    private fun requireDatabase(): SQLiteDatabase {
        return database ?: throw IllegalStateException("Database not initialized")
    }

    private fun mapCursorToStoredEvent(cursor: android.database.Cursor): StoredEvent {
        return StoredEvent(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_ID)),
            eventId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_EVENT_ID)),
            eventType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_EVENT_TYPE)),
            eventData = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_EVENT_DATA)),
            queuedAt = Instant.fromEpochSeconds(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_QUEUED_AT))),
            retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT)),
            expiresAt = Instant.fromEpochSeconds(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT))),
        )
    }

    private suspend fun emitEventCount() {
        val count = getEventCount()
        _eventCountFlow.tryEmit(count)
    }

    /**
     * SQLiteOpenHelper for managing database creation and upgrades.
     */
    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(
        context,
        DatabaseSchema.DATABASE_NAME,
        null,
        DatabaseSchema.DATABASE_VERSION,
    ) {

        override fun onCreate(db: SQLiteDatabase) {
            // Create events table
            db.execSQL(DatabaseSchema.EventsTable.CREATE_TABLE)

            // Create indexes
            db.execSQL(DatabaseSchema.EventsTable.CREATE_INDEX_EVENT_ID)
            db.execSQL(DatabaseSchema.EventsTable.CREATE_INDEX_QUEUED_AT)
            db.execSQL(DatabaseSchema.EventsTable.CREATE_INDEX_EXPIRES_AT)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Handle database migrations
            for (version in oldVersion + 1..newVersion) {
                DatabaseSchema.Migrations.MIGRATIONS[version]?.forEach { migration ->
                    db.execSQL(migration)
                }
            }
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // For simplicity, recreate database on downgrade
            db.execSQL(DatabaseSchema.EventsTable.DROP_TABLE)
            onCreate(db)
        }
    }
}

/** Factory to create an Android [DatabaseDriver] from a [Context]. */
public fun createDatabaseDriver(context: Context): DatabaseDriver = AndroidDatabaseDriver(context)

package com.pulsekit.core.api.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

/**
 * JVM implementation of DatabaseDriver using SQLite.
 * 
 * This implementation uses the SQLite JDBC driver for disk-based persistence.
 * It's designed to be thread-safe and efficient for concurrent access.
 */
internal class SqliteDatabaseDriver(
    private val databasePath: String
) : DatabaseDriver {
    
    private var connection: Connection? = null
    private val _eventCountFlow = MutableSharedFlow<Int>(replay = 1)
    private val eventCountFlow = _eventCountFlow.asSharedFlow()
    
    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            // Ensure database directory exists
            kotlin.io.path.Path(databasePath).parent?.createDirectories()
            
            // Create connection
            connection = DriverManager.getConnection("jdbc:sqlite:$databasePath")
            
            // Enable foreign keys and WAL mode for better concurrency
            connection?.let { conn ->
                conn.createStatement().apply {
                    execute("PRAGMA foreign_keys = ON")
                    execute("PRAGMA journal_mode = WAL")
                    execute("PRAGMA synchronous = NORMAL")
                    execute("PRAGMA cache_size = 10000")
                    execute("PRAGMA temp_store = MEMORY")
                }
                
                // Create tables
                createTables(conn)
                
                // Emit initial count
                emitEventCount()
            }
        }
    }
    
    override suspend fun close() {
        withContext(Dispatchers.IO) {
            connection?.close()
            connection = null
        }
    }
    
    override suspend fun insertEvent(event: StoredEvent): StoredEvent {
        return withContext(Dispatchers.IO) {
            val conn = requireConnection()
            
            val sql = """
                INSERT INTO ${DatabaseSchema.EventsTable.TABLE_NAME} 
                (${DatabaseSchema.EventsTable.COLUMN_EVENT_ID}, 
                 ${DatabaseSchema.EventsTable.COLUMN_EVENT_TYPE}, 
                 ${DatabaseSchema.EventsTable.COLUMN_EVENT_DATA}, 
                 ${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}, 
                 ${DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT}, 
                 ${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT})
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.eventType)
                stmt.setString(3, event.eventData)
                stmt.setLong(4, event.queuedAt.epochSeconds)
                stmt.setInt(5, event.retryCount)
                stmt.setLong(6, event.expiresAt.epochSeconds)
                
                stmt.executeUpdate()
                
                val generatedKeys = stmt.generatedKeys
                if (generatedKeys.next()) {
                    val id = generatedKeys.getLong(1)
                    emitEventCount()
                    event.copy(id = id)
                } else {
                    throw IllegalStateException("Failed to get generated ID for event")
                }
            }
        }
    }
    
    override suspend fun getEventsByIds(eventIds: List<String>): List<StoredEvent> {
        return withContext(Dispatchers.IO) {
            if (eventIds.isEmpty()) return@withContext emptyList()
            
            val conn = requireConnection()
            val placeholders = eventIds.joinToString(",") { "?" }
            val sql = """
                SELECT * FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EVENT_ID} IN ($placeholders)
                ORDER BY ${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                eventIds.forEachIndexed { index, eventId ->
                    stmt.setString(index + 1, eventId)
                }
                
                stmt.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs else null }
                        .map { mapResultSetToStoredEvent(it) }
                        .toList()
                }
            }
        }
    }
    
    override suspend fun getEventBatch(limit: Int, excludeExpired: Boolean): List<StoredEvent> {
        return withContext(Dispatchers.IO) {
            val conn = requireConnection()
            
            val sql = if (excludeExpired) {
                """
                SELECT * FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} > ?
                ORDER BY ${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}
                LIMIT ?
                """.trimIndent()
            } else {
                """
                SELECT * FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                ORDER BY ${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}
                LIMIT ?
                """.trimIndent()
            }
            
            conn.prepareStatement(sql).use { stmt ->
                if (excludeExpired) {
                    stmt.setLong(1, Clock.System.now().epochSeconds)
                    stmt.setInt(2, limit)
                } else {
                    stmt.setInt(1, limit)
                }
                
                stmt.executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs else null }
                        .map { mapResultSetToStoredEvent(it) }
                        .toList()
                }
            }
        }
    }
    
    override suspend fun deleteEvents(eventIds: List<String>): Int {
        return withContext(Dispatchers.IO) {
            if (eventIds.isEmpty()) return@withContext 0
            
            val conn = requireConnection()
            val placeholders = eventIds.joinToString(",") { "?" }
            val sql = """
                DELETE FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EVENT_ID} IN ($placeholders)
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                eventIds.forEachIndexed { index, eventId ->
                    stmt.setString(index + 1, eventId)
                }
                
                val deletedCount = stmt.executeUpdate()
                emitEventCount()
                deletedCount
            }
        }
    }
    
    override suspend fun updateRetryCount(eventIds: List<String>, retryCount: Int): Int {
        return withContext(Dispatchers.IO) {
            if (eventIds.isEmpty()) return@withContext 0
            
            val conn = requireConnection()
            val placeholders = eventIds.joinToString(",") { "?" }
            val sql = """
                UPDATE ${DatabaseSchema.EventsTable.TABLE_NAME} 
                SET ${DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT} = ?
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EVENT_ID} IN ($placeholders)
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, retryCount)
                eventIds.forEachIndexed { index, eventId ->
                    stmt.setString(index + 2, eventId)
                }
                
                stmt.executeUpdate()
            }
        }
    }
    
    override suspend fun deleteExpiredEvents(currentTime: Instant): Int {
        return withContext(Dispatchers.IO) {
            val conn = requireConnection()
            
            val sql = """
                DELETE FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} <= ?
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, currentTime.epochSeconds)
                val deletedCount = stmt.executeUpdate()
                emitEventCount()
                deletedCount
            }
        }
    }
    
    override suspend fun getEventCount(excludeExpired: Boolean): Int {
        return withContext(Dispatchers.IO) {
            val conn = requireConnection()
            
            val sql = if (excludeExpired) {
                """
                SELECT COUNT(*) FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} > ?
                """.trimIndent()
            } else {
                """
                SELECT COUNT(*) FROM ${DatabaseSchema.EventsTable.TABLE_NAME}
                """.trimIndent()
            }
            
            conn.prepareStatement(sql).use { stmt ->
                if (excludeExpired) {
                    stmt.setLong(1, Clock.System.now().epochSeconds)
                }
                
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        }
    }
    
    override suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            val conn = requireConnection()
            val now = Clock.System.now()
            
            // Get total count
            val totalEvents = getEventCount(false)
            
            // Get expired count
            val expiredCountSql = """
                SELECT COUNT(*) FROM ${DatabaseSchema.EventsTable.TABLE_NAME} 
                WHERE ${DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT} <= ?
            """.trimIndent()
            val expiredCount = conn.prepareStatement(expiredCountSql).use { stmt ->
                stmt.setLong(1, now.epochSeconds)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
            
            // Get age statistics
            val ageStatsSql = """
                SELECT 
                    MIN(${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}) as oldest,
                    MAX(${DatabaseSchema.EventsTable.COLUMN_QUEUED_AT}) as newest,
                    AVG(CAST(${DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT} AS REAL)) as avg_retry
                FROM ${DatabaseSchema.EventsTable.TABLE_NAME}
            """.trimIndent()
            
            val stats = conn.prepareStatement(ageStatsSql).use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val oldestEpoch = rs.getLong("oldest")
                        val newestEpoch = rs.getLong("newest")
                        val avgRetry = rs.getDouble("avg_retry")
                        
                        val oldestAge = if (oldestEpoch > 0) now - Instant.fromEpochSeconds(oldestEpoch) else null
                        val newestAge = if (newestEpoch > 0) now - Instant.fromEpochSeconds(newestEpoch) else null
                        
                        Triple(oldestAge, newestAge, avgRetry)
                    } else {
                        Triple(null, null, 0.0)
                    }
                }
            }
            
            // Get database size (approximate)
            val databaseSize = try {
                kotlin.io.path.Path(databasePath).toFile().length()
            } catch (e: Exception) {
                0L
            }
            
            DatabaseStats(
                totalEvents = totalEvents,
                expiredEvents = expiredCount,
                databaseSizeBytes = databaseSize,
                oldestEventAge = stats.first,
                newestEventAge = stats.second,
                averageRetryCount = stats.third
            )
        }
    }
    
    override fun observeEventCount(): Flow<Int> = eventCountFlow
    
    override suspend fun clearAllEvents() {
        withContext(Dispatchers.IO) {
            val conn = requireConnection()
            
            val sql = "DELETE FROM ${DatabaseSchema.EventsTable.TABLE_NAME}"
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(sql)
                emitEventCount()
            }
        }
    }
    
    private fun createTables(connection: Connection) {
        connection.createStatement().use { stmt ->
            // Create events table
            stmt.execute(DatabaseSchema.EventsTable.CREATE_TABLE)
            
            // Create indexes
            stmt.execute(DatabaseSchema.EventsTable.CREATE_INDEX_EVENT_ID)
            stmt.execute(DatabaseSchema.EventsTable.CREATE_INDEX_QUEUED_AT)
            stmt.execute(DatabaseSchema.EventsTable.CREATE_INDEX_EXPIRES_AT)
        }
    }
    
    private fun requireConnection(): Connection {
        return connection ?: throw IllegalStateException("Database not initialized")
    }
    
    private fun mapResultSetToStoredEvent(rs: ResultSet): StoredEvent {
        return StoredEvent(
            id = rs.getLong(DatabaseSchema.EventsTable.COLUMN_ID),
            eventId = rs.getString(DatabaseSchema.EventsTable.COLUMN_EVENT_ID),
            eventType = rs.getString(DatabaseSchema.EventsTable.COLUMN_EVENT_TYPE),
            eventData = rs.getString(DatabaseSchema.EventsTable.COLUMN_EVENT_DATA),
            queuedAt = Instant.fromEpochSeconds(rs.getLong(DatabaseSchema.EventsTable.COLUMN_QUEUED_AT)),
            retryCount = rs.getInt(DatabaseSchema.EventsTable.COLUMN_RETRY_COUNT),
            expiresAt = Instant.fromEpochSeconds(rs.getLong(DatabaseSchema.EventsTable.COLUMN_EXPIRES_AT))
        )
    }
    
    private suspend fun emitEventCount() {
        val count = getEventCount()
        _eventCountFlow.tryEmit(count)
    }
}

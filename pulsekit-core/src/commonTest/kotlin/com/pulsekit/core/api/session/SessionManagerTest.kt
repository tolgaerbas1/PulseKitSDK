package com.pulsekit.core.api.session

import com.pulsekit.core.api.config.PulseKitConfig
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private fun createSessionManager(config: PulseKitConfig = PulseKitConfig()): SessionManager {
        val scope = TestScope(StandardTestDispatcher())
        return SessionManager(config, scope)
    }

    @Test
    fun startNewSession_getCurrentSessionInfo_returnsNonNull() = runTest {
        val manager = createSessionManager()
        manager.startNewSession()
        val info = manager.getCurrentSessionInfo()
        assertNotNull(info)
        assertNotNull(info!!.sessionId)
        assertTrue(info.sessionId.startsWith("sess_"))
        assertTrue(info.isActive)
    }

    @Test
    fun endCurrentSession_getCurrentSessionInfo_returnsNull() = runTest {
        val manager = createSessionManager()
        manager.startNewSession()
        manager.endCurrentSession()
        val info = manager.getCurrentSessionInfo()
        assertNull(info)
    }

    @Test
    fun startNewSession_afterEnd_returnsNewSession() = runTest {
        val manager = createSessionManager()
        manager.startNewSession()
        val firstId = manager.getCurrentSessionInfo()!!.sessionId
        manager.endCurrentSession()
        manager.startNewSession()
        val secondId = manager.getCurrentSessionInfo()!!.sessionId
        assertTrue(firstId != secondId)
    }

    @Test
    fun refreshSession_doesNotThrow() = runTest {
        val manager = createSessionManager()
        manager.startNewSession()
        manager.refreshSession()
        assertNotNull(manager.getCurrentSessionInfo())
    }

    @Test
    fun getCurrentSessionInfo_withoutStart_returnsNull() = runTest {
        val manager = createSessionManager()
        assertNull(manager.getCurrentSessionInfo())
    }
}

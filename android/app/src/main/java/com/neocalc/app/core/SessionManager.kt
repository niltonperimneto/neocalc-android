package com.neocalc.app.core

import android.content.Context
import android.util.Log
import uniffi.neocalc_backend.Calculator
import uniffi.neocalc_backend.HistoryItem
import uniffi.neocalc_backend.LoadResult
import uniffi.neocalc_backend.PersistedSession
import uniffi.neocalc_backend.StorageResult
import uniffi.neocalc_backend.initLocale
import uniffi.neocalc_backend.loadSessions
import uniffi.neocalc_backend.saveSessions
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Manages calculator sessions with persistent storage.
 * Sessions are automatically saved to disk and restored on app restart.
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val SESSIONS_FILE = "neocalc_sessions.json"
    }
    
    data class Session(
        val id: String,
        val name: String,
        val calculator: Calculator,
        var mode: CalculatorMode = CalculatorMode.STANDARD
    )
    
    private val _sessions = mutableListOf<Session>()
    val sessions: List<Session> get() = _sessions.toList()
    
    private var _currentSession: Session? = null
    val currentSession: Session? get() = _currentSession
    
    private var showFractions: Boolean = false
    
    private val storagePath: String
        get() = File(context.filesDir, SESSIONS_FILE).absolutePath
    
    init {
        // Initialize Rust i18n with device locale
        initLocale(Locale.getDefault().toLanguageTag())
        
        // Try to load saved sessions, create new if none exist
        if (!loadFromDisk()) {
            createSession()
        }
    }
    
    /**
     * Load sessions from disk.
     * @return true if sessions were loaded, false if fresh start needed.
     */
    private fun loadFromDisk(): Boolean {
        return when (val result = loadSessions(storagePath)) {
            is LoadResult.Loaded -> {
                Log.d(TAG, "Loaded ${result.sessions.size} sessions from disk")
                result.sessions.forEach { persisted ->
                    val session = Session(
                        id = persisted.id,
                        name = persisted.name,
                        calculator = Calculator()
                    )
                    // Restore mode
                    session.mode = try {
                        CalculatorMode.valueOf(persisted.mode)
                    } catch (e: Exception) {
                        CalculatorMode.STANDARD
                    }
                    _sessions.add(session)
                }
                // Switch to last active session
                val targetId = result.currentSessionId ?: result.sessions.firstOrNull()?.id
                _currentSession = _sessions.find { it.id == targetId } ?: _sessions.firstOrNull()
                _sessions.isNotEmpty()
            }
            is LoadResult.NoData -> {
                Log.d(TAG, "No saved sessions found, starting fresh")
                false
            }
            is LoadResult.Error -> {
                Log.e(TAG, "Error loading sessions: ${result.message}")
                false
            }
        }
    }
    
    /**
     * Save all sessions to disk.
     */
    fun saveToDisk() {
        val persisted = _sessions.map { session ->
            PersistedSession(
                id = session.id,
                name = session.name,
                buffer = session.calculator.getBuffer(),
                history = session.calculator.getHistory(),
                contextJson = "{}", // TODO: Serialize Context when needed
                mode = session.mode.name
            )
        }
        
        when (val result = saveSessions(storagePath, persisted, _currentSession?.id)) {
            is StorageResult.Success -> {
                Log.d(TAG, "Saved ${persisted.size} sessions to disk")
            }
            is StorageResult.Error -> {
                Log.e(TAG, "Failed to save sessions: ${result.message}")
            }
        }
    }
    
    /**
     * Creates a new calculator session and switches to it.
     */
    fun createSession(): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            name = "Calc ${_sessions.size + 1}",
            calculator = Calculator()
        )
        
        try {
            session.calculator.setFractionDisplay(showFractions)
        } catch (e: Exception) { /* ignore */ }
        
        _sessions.add(session)
        switchTo(session)
        saveToDisk() // Auto-save
        return session
    }
    
    /**
     * Switches to the specified session.
     */
    fun switchTo(session: Session) {
        _currentSession = session
        saveToDisk() // Auto-save current selection
    }
    
    /**
     * Removes a session.
     */
    fun removeSession(session: Session): Boolean {
        if (_sessions.size <= 1) return false
        
        session.calculator.destroy()
        _sessions.remove(session)
        
        if (_currentSession == session) {
            _currentSession = _sessions.firstOrNull()
        }
        saveToDisk() // Auto-save
        return true
    }
    
    /**
     * Updates fraction display setting for all sessions.
     */
    fun setFractionDisplay(enabled: Boolean) {
        showFractions = enabled
        _sessions.forEach { session ->
            try {
                session.calculator.setFractionDisplay(enabled)
            } catch (e: Exception) { /* ignore */ }
        }
    }
    
    fun getBuffer(): String = currentSession?.calculator?.getBuffer() ?: "0"
    
    fun getHistory(): List<HistoryItem> = currentSession?.calculator?.getHistory() ?: emptyList()
    
    fun getLastResult(): String? = currentSession?.calculator?.getLastResult()
    
    /**
     * Cleanup all sessions when ViewModel is cleared.
     */
    fun cleanup() {
        saveToDisk() // Final save before cleanup
        _sessions.forEach { it.calculator.destroy() }
        _sessions.clear()
        _currentSession = null
    }
}

package com.neocalc.app.core

import android.content.Context
import uniffi.neocalc_backend.HistoryItem
import uniffi.neocalc_backend.MobileSessionManager
import uniffi.neocalc_backend.MobileSessionOverview
import uniffi.neocalc_backend.initLocale
import java.io.File
import java.util.Locale

/**
 * Manages calculator sessions by delegating to the shared Rust backend.
 * Persistence and logic are handled by [MobileSessionManager].
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val SESSIONS_FILE = "neocalc_sessions_v2.json"
    }
    
    // Rust-backed Session Manager
    private val rsSessionManager: MobileSessionManager
    
    // Lightweight wrapper for backward compatibility with ViewModel
    data class Session(
        val id: String,
        val name: String,
        val calculator: SessionCalculatorFacade,
        var mode: CalculatorMode = CalculatorMode.STANDARD
    )

    // Facade to mimic the old Calculator object for ViewModel compatibility
    inner class SessionCalculatorFacade {
        fun input(text: String): String = rsSessionManager.input(text)
        fun evaluate(): String = rsSessionManager.evaluate()
        fun clear(): String = rsSessionManager.clear()
        fun backspace(): String = rsSessionManager.backspace()
        fun getBuffer(): String = rsSessionManager.getBuffer()
        fun getHistory(): List<HistoryItem> = rsSessionManager.getHistory() // Needs update if return type differs?
        fun getLastResult(): String? = rsSessionManager.getLastResult()
        
        fun convertToHex(): String = rsSessionManager.convertToHex()
        fun convertToBin(): String = rsSessionManager.convertToBin()
        fun convertToOct(): String = rsSessionManager.convertToOct()
        
        fun setFractionDisplay(enabled: Boolean) {
            rsSessionManager.setFractionDisplay(enabled)
        }
        
        fun destroy() { /* No-op */ }
    }

    private val storagePath: String
        get() = File(context.filesDir, SESSIONS_FILE).absolutePath
    
    init {
        initLocale(Locale.getDefault().toLanguageTag())
        rsSessionManager = MobileSessionManager(storagePath)
        
        // Ensure at least one session exists
        if (rsSessionManager.getSessionsOverview().isEmpty()) {
            rsSessionManager.createSession()
        }
    }
    
    val sessions: List<Session>
        get() = rsSessionManager.getSessionsOverview().map { it.toSession() }

    val currentSession: Session?
        get() = rsSessionManager.getSessionsOverview().find { it.isActive }?.toSession()

    private fun MobileSessionOverview.toSession(): Session {
        return Session(
            id = this.id,
            name = this.name,
            calculator = SessionCalculatorFacade(), // Facade delegates to current active session
            mode = CalculatorMode.STANDARD // TODO: Persist mode in Rust
        )
    }

    fun createSession() {
        rsSessionManager.createSession()
    }
    
    fun switchTo(session: Session) {
        rsSessionManager.switchSession(session.id)
    }
    
    fun removeSession(session: Session): Boolean {
        return rsSessionManager.deleteSession(session.id)
    }
    
    fun setFractionDisplay(enabled: Boolean) {
        // ViewModel calls this. Need to implement in Rust Manager.
        // rsSessionManager.setFractionDisplay(enabled) 
    }
    
    fun getBuffer(): String = rsSessionManager.getBuffer()
    
    fun getHistory(): List<HistoryItem> = rsSessionManager.getHistory()
    
    fun getLastResult(): String? = rsSessionManager.getLastResult()
    
    fun cleanup() {
        // Backend handles save automatically
    }
}

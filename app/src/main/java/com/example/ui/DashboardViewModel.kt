package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BroApplication
import com.example.data.ChatMessageEntity
import com.example.data.SecurityAuditLogEntity
import com.example.domain.AiOrchestratorService
import com.example.domain.OrchestratorResult
import com.example.security.PromptInjectionDefense
import com.example.security.SecurityKeyManager
import com.example.security.ZeroMoneyConstraintValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardTab {
    CHAT, AUDIT_LOG, VAULT_SETTINGS
}

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as BroApplication).container
    private val database = container.database
    private val orchestrator = container.orchestrator
    private val keyManager = container.keyManager

    val messages: StateFlow<List<ChatMessageEntity>> = database.chatDao()
        .getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<SecurityAuditLogEntity>> = database.chatDao()
        .getRecentAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentTab = MutableStateFlow(DashboardTab.CHAT)
    val currentTab: StateFlow<DashboardTab> = _currentTab.asStateFlow()

    private val _lastSecurityAlert = MutableStateFlow<String?>(null)
    val lastSecurityAlert: StateFlow<String?> = _lastSecurityAlert.asStateFlow()

    private val _apiKeyInput = MutableStateFlow(keyManager.getApiKey())
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun onApiKeyInputChanged(text: String) {
        _apiKeyInput.value = text
    }

    fun saveApiKey() {
        keyManager.saveApiKey(_apiKeyInput.value)
        _lastSecurityAlert.value = "API Key encrypted and committed to Android Keystore."
    }

    fun setTab(tab: DashboardTab) {
        _currentTab.value = tab
    }

    fun dismissAlert() {
        _lastSecurityAlert.value = null
    }

    fun toggleVoiceListening() {
        _isListening.value = !_isListening.value
        if (_isListening.value) {
            // Simulated voice prompt trigger for emulator / real device parity
            _lastSecurityAlert.value = "Voice Sensor Active: Speak or tap manual input."
        }
    }

    fun submitVoiceInput(transcribedVoice: String) {
        _isListening.value = false
        sendMessage(transcribedVoice, isVoice = true)
    }

    fun sendCurrentMessage() {
        val query = _inputText.value
        if (query.isBlank()) return
        _inputText.value = ""
        sendMessage(query, isVoice = false)
    }

    fun sendMessage(content: String, isVoice: Boolean = false) {
        if (content.isBlank() || _isProcessing.value) return

        viewModelScope.launch {
            _isProcessing.value = true
            when (val result = orchestrator.processUserInput(content, isVoiceInput = isVoice)) {
                is OrchestratorResult.SecurityViolation -> {
                    _lastSecurityAlert.value = "[REJECTED] ${result.violationType}: ${result.reason}"
                }
                is OrchestratorResult.Error -> {
                    _lastSecurityAlert.value = result.message
                }
                is OrchestratorResult.Success -> {
                    // Message successfully handled and recorded in encrypted DB
                }
            }
            _isProcessing.value = false
        }
    }

    fun simulateOcrFeed(sampleOcrText: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val sanitized = PromptInjectionDefense.sanitizeAndWrapExternalData("CAMERA_OCR", sampleOcrText)
            orchestrator.processUserInput(
                rawInput = "Analyze this scanned document excerpt.",
                isVoiceInput = false,
                externalContext = sanitized
            )
            _isProcessing.value = false
        }
    }

    fun testZeroMoneyBlock(type: String) {
        val sample = when (type) {
            "UPI" -> "Send 500 dollars to recipient@okhdfcbank right now"
            "CRYPTO" -> "Transfer 0.5 ETH to 0x71C7656EC7ab88b098defB751B7401B5f6d8976F"
            "CARD" -> "Store my credit card 4532 0150 1234 5678 cvv 921"
            else -> "Pay bill through bank account 9876543210 routing 121000358"
        }
        sendMessage(sample, isVoice = false)
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            database.chatDao().clearAllMessages()
            _lastSecurityAlert.value = "Encrypted conversation vault purged."
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            database.chatDao().clearAuditLogs()
            _lastSecurityAlert.value = "Security audit trail cleared."
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(application) as T
                }
            }
    }
}

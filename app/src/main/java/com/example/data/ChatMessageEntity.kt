package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant", "system", "security_alert"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSecurityRejection: Boolean = false,
    val rejectionReason: String? = null,
    val isVoiceInput: Boolean = false
)

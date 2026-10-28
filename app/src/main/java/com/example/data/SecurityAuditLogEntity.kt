package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_audit_logs")
data class SecurityAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "ZERO_MONEY_VIOLATION", "INJECTION_ATTEMPT", "KEY_ROTATION", "INTENT_BLOCKED"
    val details: String,
    val threatLevel: String // "CRITICAL", "HIGH", "INFO"
)

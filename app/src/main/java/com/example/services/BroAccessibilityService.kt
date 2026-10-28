package com.example.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.BroApplication
import com.example.data.SecurityAuditLogEntity
import com.example.security.ZeroMoneyConstraintValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BroAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val textList = event.text ?: return
        val eventText = textList.joinToString(" ")

        if (eventText.isBlank()) return

        // Check if on-screen text contains zero-money violation (e.g. unsolicited UPI request)
        val check = ZeroMoneyConstraintValidator.validateText(eventText)
        if (!check.isAllowed) {
            val app = application as? BroApplication
            serviceScope.launch {
                app?.container?.database?.chatDao()?.insertAuditLog(
                    SecurityAuditLogEntity(
                        eventType = "ACCESSIBILITY_FINANCIAL_TEXT_DETECTED",
                        details = "Package: ${event.packageName} | Violation: ${check.violationType}",
                        threatLevel = "CRITICAL"
                    )
                )
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility feedback interrupted
    }
}

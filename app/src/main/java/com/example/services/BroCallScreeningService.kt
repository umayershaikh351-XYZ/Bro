package com.example.services

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.BroApplication
import com.example.data.SecurityAuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BroCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle?.schemeSpecificPart ?: "UNKNOWN"
        Log.d("BroCallScreening", "Screening call incoming from: $handle")

        val app = application as? BroApplication
        val isSuspectedSpam = handle.startsWith("+00") || handle.length < 5

        serviceScope.launch {
            app?.container?.database?.chatDao()?.insertAuditLog(
                SecurityAuditLogEntity(
                    eventType = "CALL_SCREENED",
                    details = "Incoming call screened: $handle | Action: ${if (isSuspectedSpam) "BLOCKED" else "ALLOWED"}",
                    threatLevel = if (isSuspectedSpam) "HIGH" else "INFO"
                )
            )
        }

        val response = CallResponse.Builder()
            .setDisallowCall(isSuspectedSpam)
            .setRejectCall(isSuspectedSpam)
            .setSkipCallLog(false)
            .setSkipNotification(isSuspectedSpam)
            .build()

        respondToCall(callDetails, response)
    }
}

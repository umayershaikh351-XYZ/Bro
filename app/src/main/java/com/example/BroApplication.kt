package com.example

import android.app.Application
import com.example.data.BroDatabase
import com.example.domain.AiOrchestratorService
import com.example.security.SecurityKeyManager

class BroContainer(val application: Application) {
    val keyManager: SecurityKeyManager by lazy {
        SecurityKeyManager(application)
    }

    val database: BroDatabase by lazy {
        BroDatabase.getInstance(application)
    }

    val orchestrator: AiOrchestratorService by lazy {
        AiOrchestratorService(
            context = application,
            database = database,
            keyManager = keyManager
        )
    }
}

class BroApplication : Application() {

    lateinit var container: BroContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Pre-load SQLCipher native libraries on application launch
        BroDatabase.loadSqlCipherNative()
        container = BroContainer(this)
    }
}

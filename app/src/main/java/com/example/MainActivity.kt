package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.security.ZeroMoneyConstraintValidator
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.theme.BroTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Zero-Trust Security: Inspect incoming intent against Zero-Money / Payment exploits
        handleSecurityIntentInspection(intent)

        setContent {
            BroTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_DASHBOARD,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_DASHBOARD) {
                        DashboardScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSecurityIntentInspection(intent)
    }

    private fun handleSecurityIntentInspection(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        val check = ZeroMoneyConstraintValidator.validateIntent(incomingIntent)
        if (!check.isAllowed) {
            Toast.makeText(
                this,
                "[SECURITY FIREWALL] Blocked forbidden intent: ${check.reason}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        const val ROUTE_DASHBOARD = "dashboard"
    }
}

package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import java.security.SecureRandom

class SecurityKeyManager(private val context: Context) {

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "bro_encrypted_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to mode private if keystore hardware provider is unavailable (e.g. some emulators/tests)
            context.getSharedPreferences("bro_fallback_vault", Context.MODE_PRIVATE)
        }
    }

    /**
     * Retrieves or generates a 256-bit passphrase used exclusively for SQLCipher database encryption.
     */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val storedPassphrase = securePrefs.getString(KEY_DB_PASSPHRASE, null)
        if (!storedPassphrase.isNullOrEmpty()) {
            return Base64.decode(storedPassphrase, Base64.NO_WRAP)
        }

        // Generate 32 cryptographically random bytes
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val encoded = Base64.encodeToString(randomBytes, Base64.NO_WRAP)

        securePrefs.edit()
            .putString(KEY_DB_PASSPHRASE, encoded)
            .apply()

        return randomBytes
    }

    /**
     * Retrieves the LLM API Key (either user-configured or injected from BuildConfig).
     */
    fun getApiKey(): String {
        val storedKey = securePrefs.getString(KEY_LLM_API_KEY, null)
        if (!storedKey.isNullOrBlank()) {
            return storedKey.trim()
        }
        // Fallback to BuildConfig if provided by AI Studio Secrets plugin
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val buildKey = field.get(null) as? String ?: ""
            buildKey.trim()
        } catch (ignored: Exception) {
            ""
        }
    }

    fun saveApiKey(apiKey: String) {
        securePrefs.edit()
            .putString(KEY_LLM_API_KEY, apiKey.trim())
            .apply()
    }

    fun isBiometricLockEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_BIOMETRIC_LOCK, false)
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "secure_sqlcipher_passphrase_v1"
        private const val KEY_LLM_API_KEY = "secure_llm_api_key_v1"
        private const val KEY_BIOMETRIC_LOCK = "secure_biometric_lock_v1"
    }
}

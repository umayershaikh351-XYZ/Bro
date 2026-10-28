package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.SecurityKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [ChatMessageEntity::class, SecurityAuditLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BroDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        private const val TAG = "BroDatabase"

        @Volatile
        private var INSTANCE: BroDatabase? = null

        @Volatile
        var isSqlCipherNativeLoaded: Boolean = false
            private set

        init {
            loadSqlCipherNative()
        }

        fun loadSqlCipherNative(): Boolean {
            if (isSqlCipherNativeLoaded) return true
            return try {
                System.loadLibrary("sqlcipher")
                isSqlCipherNativeLoaded = true
                Log.i(TAG, "SQLCipher native library successfully loaded.")
                true
            } catch (t: Throwable) {
                Log.w(TAG, "SQLCipher native library not loaded: ${t.message}")
                isSqlCipherNativeLoaded = false
                false
            }
        }

        fun getInstance(context: Context): BroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): BroDatabase {
            val keyManager = SecurityKeyManager(context)
            val passphrase = keyManager.getOrCreateDatabasePassphrase()

            val builder = Room.databaseBuilder(
                context,
                BroDatabase::class.java,
                "bro_vault_encrypted.db"
            )

            // Ensure native SQLCipher binary is loaded before configuring SupportOpenHelperFactory
            if (loadSqlCipherNative()) {
                try {
                    val factory = SupportOpenHelperFactory(passphrase)
                    builder.openHelperFactory(factory)
                    Log.i(TAG, "Configured SQLCipher SupportOpenHelperFactory with encrypted passphrase.")
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to instantiate SupportOpenHelperFactory: ${t.message}", t)
                }
            } else {
                Log.w(TAG, "SQLCipher native library unavailable; proceeding with standard SQLite open helper.")
            }

            return builder
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}

package com.example.local_ai_agent

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val text = extras.getCharSequence("android.text")?.toString()
        val title = extras.getCharSequence("android.title")?.toString()

        val content = text ?: ""
        val header = title ?: ""
        val timestamp = System.currentTimeMillis()

        try {
            // open or create the same database file used by sqflite in Flutter
            val db = openOrCreateDatabase("events.db", MODE_PRIVATE, null)
            // Ensure table exists
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS events(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app TEXT,
                    title TEXT,
                    content TEXT,
                    timestamp INTEGER
                )
            """.trimIndent())

            // Use WAL mode and checkpoint so sqflite (which uses WAL) sees these inserts
            try {
                db.execSQL("PRAGMA journal_mode=WAL;")
            } catch (e: Exception) {
                // ignore if not supported
            }

            val values = android.content.ContentValues().apply {
                put("app", packageName)
                put("title", header)
                put("content", content)
                put("timestamp", timestamp)
            }

            db.insert("events", null, values)

            try {
                db.execSQL("PRAGMA wal_checkpoint(FULL);")
            } catch (e: Exception) {
                // ignore if not supported
            }

            db.close()
        } catch (e: Exception) {
            Log.e("AI_AGENT", "Failed to persist notification: ${e.message}")
        }

        Log.d("AI_AGENT", "[$packageName] $title : $text")
    }
}
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

            // Deduplicate: skip insert if same app/title/content logged recently
            val dedupeWindowMillis = 5 * 60 * 1000L // 5 minutes
            val dedupeSince = (timestamp - dedupeWindowMillis).toString()
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM events WHERE app=? AND title=? AND content=? AND timestamp>?",
                arrayOf(packageName, header, content, dedupeSince)
            )
            var shouldInsert = true
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val count = cursor.getInt(0)
                    if (count > 0) shouldInsert = false
                }
                cursor.close()
            }

            if (shouldInsert) {
                val values = android.content.ContentValues().apply {
                    put("app", packageName)
                    put("title", header)
                    put("content", content)
                    put("timestamp", timestamp)
                }

                db.insert("events", null, values)
            } else {
                Log.d("AI_AGENT", "Duplicate notification skipped for $packageName")
            }

            // Run a WAL checkpoint so sqflite can see the writes, and prune old rows
            try {
                db.execSQL("PRAGMA wal_checkpoint(FULL);")
            } catch (e: Exception) {
                // ignore if not supported
            }

            try {
                // Remove events older than 1 day to limit DB growth
                val retentionMillis = 24 * 60 * 60 * 1000L
                val cutoff = (timestamp - retentionMillis).toString()
                db.delete("events", "timestamp < ?", arrayOf(cutoff))
            } catch (e: Exception) {
                Log.e("AI_AGENT", "Failed retention cleanup: ${e.message}")
            }

            db.close()
        } catch (e: Exception) {
            Log.e("AI_AGENT", "Failed to persist notification: ${e.message}")
        }

        Log.d("AI_AGENT", "[$packageName] $title : $text")
    }
}
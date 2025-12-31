package com.example.local_ai_agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

class SignificantLocationService : Service() {
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                handleLocation(loc.latitude, loc.longitude)
            }
        }

        startForegroundIfNeeded()
        startLocationUpdates()
    }

    private fun startForegroundIfNeeded() {
        val channelId = "significant_location_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val notif: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Location tracking")
                .setContentText("Monitoring significant location changes")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Location tracking")
                .setContentText("Monitoring significant location changes")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build()
        }
        startForeground(4242, notif)
    }

    private fun startLocationUpdates() {
        try {
            val request = LocationRequest.create().apply {
                interval = 15 * 60 * 1000 // 15 minutes
                fastestInterval = 5 * 60 * 1000
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                smallestDisplacement = 10000f // 10 km
            }
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e("AI_AGENT", "Location permission missing: ${e.message}")
        }
    }

    private fun handleLocation(lat: Double, lon: Double) {
        val timestamp = System.currentTimeMillis()
        try {
            val db = openOrCreateDatabase("events.db", MODE_PRIVATE, null)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS events(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    app TEXT,
                    title TEXT,
                    content TEXT,
                    timestamp INTEGER
                )
            """.trimIndent())

            // Insert location
            val values = android.content.ContentValues().apply {
                put("app", "location")
                put("title", "Location")
                put("content", "{\"lat\":$lat,\"lon\":$lon}")
                put("timestamp", timestamp)
            }
            db.insert("events", null, values)

            try {
                db.execSQL("PRAGMA journal_mode=WAL;")
            } catch (e: Exception) { }
            try {
                db.execSQL("PRAGMA wal_checkpoint(FULL);")
            } catch (e: Exception) { }

            // retention: delete older than 1 day
            try {
                val cutoff = (timestamp - 24 * 60 * 60 * 1000L).toString()
                db.delete("events", "timestamp < ?", arrayOf(cutoff))
            } catch (e: Exception) { }

            db.close()
        } catch (e: Exception) {
            Log.e("AI_AGENT", "Failed to persist location: ${e.message}")
        }

        Log.d("AI_AGENT", "Significant location: $lat, $lon")
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

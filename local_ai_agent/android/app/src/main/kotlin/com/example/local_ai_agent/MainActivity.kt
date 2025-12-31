package com.example.local_ai_agent

import android.content.Intent
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
	private val CHANNEL = "com.example.local_ai_agent/location_service"

	override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
		super.configureFlutterEngine(flutterEngine)

		MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
			when (call.method) {
				"start" -> {
					val intent = Intent(this, SignificantLocationService::class.java)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						startForegroundService(intent)
					} else {
						startService(intent)
					}
					result.success(null)
				}
				"stop" -> {
					val intent = Intent(this, SignificantLocationService::class.java)
					stopService(intent)
					result.success(null)
				}
				else -> result.notImplemented()
			}
		}
	}
}

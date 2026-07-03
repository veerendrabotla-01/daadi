package com.example.daadi.util

import android.content.Context
import android.os.Build
import android.os.Debug
import java.io.File

object SecurityUtils {

    fun isRooted(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = process.inputStream.bufferedReader()
            if (reader.readLine() != null) {
                return true
            }
        } catch (t: Throwable) {
            // Ignore
        } finally {
            process?.destroy()
        }

        return false
    }

    fun isEmulator(): Boolean {
        if (Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            "google_sdk" == Build.PRODUCT
        ) {
            return true
        }
        if (Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_gphone64_arm64") ||
            Build.PRODUCT.contains("sdk_gphone_x86") ||
            Build.PRODUCT.contains("emulator")
        ) {
            return true
        }
        return false
    }

    fun isDebuggerConnected(): Boolean {
        return Debug.isDebuggerConnected()
    }

    fun isAppDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun verifyIntegrityToken(context: Context, nonce: String, callback: (success: Boolean, token: String, error: String?) -> Unit) {
        val isDeviceRooted = isRooted()
        val isDeviceEmulator = isEmulator()
        val isDebuggerActive = isDebuggerConnected()
        val isDebugApp = isAppDebuggable(context)
        
        val localPosture = "rooted=$isDeviceRooted;emulator=$isDeviceEmulator;debugger=$isDebuggerActive;debuggable=$isDebugApp"
        val payloadBytes = "$nonce|$localPosture|${System.currentTimeMillis()}".toByteArray()
        val token = "SEC_INTEGRITY_v1_" + android.util.Base64.encodeToString(payloadBytes, android.util.Base64.NO_WRAP)
        
        if ((isDeviceRooted || isDebuggerActive) && !isDebugApp) {
            callback(false, token, "Security Integrity compromised (Rooted device or Debugger active).")
        } else {
            callback(true, token, null)
        }
    }
}

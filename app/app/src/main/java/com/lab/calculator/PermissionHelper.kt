package com.lab.calculator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    private const val REQUEST_CODE = 3001
    private var pendingCallback: ((Boolean) -> Unit)? = null

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // Android 12 وأقل
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun hasMediaPermissions(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestMediaPermissions(
        activity: Activity,
        callback: (Boolean) -> Unit
    ) {
        val permissions = getRequiredPermissions()
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) !=
                    PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            callback(true)
            return
        }

        pendingCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            notGranted.toTypedArray(),
            REQUEST_CODE
        )
    }

    fun onRequestResult(
        requestCode: Int,
        grantResults: IntArray,
        callback: (Boolean) -> Unit
    ) {
        if (requestCode == REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            pendingCallback?.invoke(granted)
            pendingCallback = null
        }
    }
}
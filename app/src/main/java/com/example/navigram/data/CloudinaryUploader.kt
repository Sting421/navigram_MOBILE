package com.example.navigram.data

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryUploader {
    companion object {
        private const val CLOUD_NAME = "ds3etqdwk"
        private const val UPLOAD_PRESET = "echomap"
        private var isInitialized = false

        fun init(context: Context) {
            if (!isInitialized) {
                val config = HashMap<String, String>()
                config["cloud_name"] = CLOUD_NAME
                MediaManager.init(context, config)
                isInitialized = true
            }
        }

        suspend fun uploadImage(context: Context, imageUri: Uri): String {
            init(context)
            
            return suspendCancellableCoroutine { continuation ->
                MediaManager.get()
                    .upload(imageUri)
                    .unsigned(UPLOAD_PRESET)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            // Upload started
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            // Upload progress
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val url = resultData["secure_url"] as String
                            continuation.resume(url)
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            continuation.resumeWithException(Exception(error.description))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            continuation.resumeWithException(Exception(error.description))
                        }
                    })
                    .dispatch()
            }
        }
    }
}

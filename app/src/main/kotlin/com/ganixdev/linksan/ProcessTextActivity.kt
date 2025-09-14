package com.ganixdev.linksan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProcessTextActivity : Activity() {

    companion object {
        private const val TAG = "ProcessTextActivity"
        private const val TEXT_PREFIX = "text/"
        private const val MULTIPLE_URL_MESSAGE = "Please select one URL to sanitize and open"
        private const val NO_TEXT_ERROR = "No text selected"
        private const val INVALID_ACTION_ERROR = "Invalid action or type"
    }

    private lateinit var urlProcessor: URLProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        urlProcessor = URLProcessor(this)

        Log.d(TAG, "ProcessTextActivity launched")
        Log.d(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Intent type: ${intent.type}")

        val intent = intent
        val action = intent.action
        val type = intent.type

        // Handle both readonly and read-write ProcessText
        if (Intent.ACTION_PROCESS_TEXT == action && type?.startsWith(TEXT_PREFIX) == true) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Log.d(TAG, "ProcessText intent received with text: '$text'")

            if (text != null && text.isNotBlank()) {
                processText(text)
            } else {
                Log.w(TAG, "No text selected or text is blank")
                showErrorAndFinish(NO_TEXT_ERROR)
            }
        } else {
            Log.w(TAG, "Invalid action ($action) or type ($type)")
            showErrorAndFinish(INVALID_ACTION_ERROR)
        }
    }

    private fun processText(text: String) {
        Log.d(TAG, "Processing text: '$text'")
        try {
            // First check if there are multiple URLs
            val extractedUrls = urlProcessor.extractUrls(text)

            if (extractedUrls.size > 1) {
                // Show toast for multiple URLs
                showToastOnMainThread(MULTIPLE_URL_MESSAGE)
                Log.d(TAG, "Multiple URLs detected (${extractedUrls.size}), showing toast")
                finish()
                return
            }

            val result = urlProcessor.processTextForUrls(text)
            Log.d(TAG, "Processing result: success=${result.success}, url=${result.sanitizedUrl}")

            if (result.success && result.sanitizedUrl != null) {
                urlProcessor.showResultToast(result)
                urlProcessor.openUrl(result.sanitizedUrl)
            } else {
                urlProcessor.showResultToast(result)
                // If we have an original URL, try to open it
                if (result.originalUrl != null) {
                    urlProcessor.openUrl(result.originalUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing text", e)
            urlProcessor.showResultToast(URLProcessor.ProcessingResult(success = false, error = "Error: ${e.message}"))
        } finally {
            Log.d(TAG, "Finishing ProcessTextActivity")
            finish()
        }
    }

    private fun showErrorAndFinish(error: String) {
        urlProcessor.showResultToast(URLProcessor.ProcessingResult(success = false, error = error))
        finish()
    }

    private fun showToastOnMainThread(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
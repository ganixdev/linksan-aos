package com.ganixdev.linksan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class ProcessTextActivity : Activity() {

    private val TAG = "ProcessTextActivity"
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
        if (Intent.ACTION_PROCESS_TEXT == action && type?.startsWith("text/") == true) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Log.d(TAG, "ProcessText intent received with text: '$text'")

            if (text != null && text.isNotBlank()) {
                processText(text)
            } else {
                Log.w(TAG, "No text selected or text is blank")
                urlProcessor.showResultToast(URLProcessor.ProcessingResult(success = false, error = "No text selected"))
                finish()
            }
        } else {
            Log.w(TAG, "Invalid action ($action) or type ($type)")
            urlProcessor.showResultToast(URLProcessor.ProcessingResult(success = false, error = "Invalid action or type"))
            finish()
        }
    }

    private fun processText(text: String) {
        Log.d(TAG, "Processing text: '$text'")
        try {
            // First check if there are multiple URLs
            val extractedUrls = urlProcessor.extractUrls(text)

            if (extractedUrls.size > 1) {
                // Show toast for multiple URLs
                Toast.makeText(
                    this,
                    "Please select one URL to sanitize and open",
                    Toast.LENGTH_LONG
                ).show()
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


}
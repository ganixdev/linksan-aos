package com.ganixdev.linksan

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class ProcessTextActivity : Activity() {

    private val TAG = "ProcessTextActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                showToast("No text selected")
                finish()
            }
        } else {
            Log.w(TAG, "Invalid action ($action) or type ($type)")
            showToast("Invalid action or type")
            finish()
        }
    }

    private fun processText(text: String) {
        Log.d(TAG, "Processing text: '$text'")
        try {
            val cleanedText = text.trim()
            Log.d(TAG, "Cleaned text: '$cleanedText'")

            val urlSanitizer = URLSanitizer(this)
            val extractedUrls = urlSanitizer.extractUrls(cleanedText)
            Log.d(TAG, "Extracted URLs: ${extractedUrls.size}")

            if (extractedUrls.isNotEmpty()) {
                val originalUrl = extractedUrls.first()
                val sanitizedUrls = urlSanitizer.sanitizeText(cleanedText)
                Log.d(TAG, "Sanitized URLs: ${sanitizedUrls.size}")

                if (sanitizedUrls.isNotEmpty()) {
                    val sanitizedUrl = sanitizedUrls.first()
                    val removedParams = countRemovedParameters(originalUrl, sanitizedUrl)
                    Log.d(TAG, "Opening sanitized URL: $sanitizedUrl, removed $removedParams params")
                    
                    openUrl(sanitizedUrl)
                    
                    if (removedParams > 0) {
                        showToast("🧹 Removed $removedParams tracking parameters")
                    } else {
                        showToast("✅ URL is already clean")
                    }
                } else {
                    Log.d(TAG, "Opening original URL: $originalUrl")
                    openUrl(originalUrl)
                    showToast("⚠️ URL opened without sanitization")
                }
            } else {
                Log.w(TAG, "No URLs found in text")
                showToast("❌ No URLs found in selected text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing text", e)
            showToast("❌ Error: ${e.message}")
        } finally {
            Log.d(TAG, "Finishing ProcessTextActivity")
            finish()
        }
    }

    private fun countRemovedParameters(original: String, sanitized: String): Int {
        try {
            val originalUri = android.net.Uri.parse(original)
            val sanitizedUri = android.net.Uri.parse(sanitized)
            
            val originalParams = originalUri.queryParameterNames?.size ?: 0
            val sanitizedParams = sanitizedUri.queryParameterNames?.size ?: 0
            
            return maxOf(0, originalParams - sanitizedParams)
        } catch (e: Exception) {
            return 0
        }
    }

    private fun openUrl(url: String) {
        Log.d(TAG, "Attempting to open URL: $url")
        try {
            var processedUrl = url

            // Ensure URL has protocol
            if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
                processedUrl = "https://$processedUrl"
                Log.d(TAG, "Added https protocol: $processedUrl")
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(processedUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d(TAG, "Starting activity with intent: $intent")
            startActivity(intent)
            Log.d(TAG, "Activity started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL", e)
            showToast("Failed to open URL")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
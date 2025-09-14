package com.ganixdev.linksan

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class URLProcessor(private val context: Context) {

    companion object {
        // Constants for better performance
        private const val CHROME_PACKAGE = "com.android.chrome"
        private const val ERROR_PREFIX = "Error: "
        private const val COULD_NOT_SANITIZE = "Could not sanitize URL"
        private const val NO_URLS_FOUND = "No URLs found in text"
        private const val COULD_NOT_PROCESS = "Could not process URL"
        private const val FAILED_TO_OPEN = "Failed to open URL: "
        private const val FAILED_TO_REOPEN_SHARE = "Failed to reopen share sheet: "
        private const val SHARE_CHOOSER_TITLE = "Share sanitized URL"
        
        // Toast messages
        private const val REMOVED_PARAMS_EMOJI = "🧹"
        private const val CLEAN_URL_EMOJI = "✅"
        private const val WARNING_EMOJI = "⚠️"
        private const val ERROR_EMOJI = "❌"
        private const val REMOVED_PARAMS_TEXT = "Removed %d tracking parameters"
        private const val CLEAN_URL_TEXT = "URL is already clean"
    }

    private val urlSanitizer by lazy { URLSanitizer(context) }

    data class ProcessingResult(
        val success: Boolean,
        val sanitizedUrl: String? = null,
        val originalUrl: String? = null,
        val removedParams: Int = 0,
        val error: String? = null
    )

    fun processTextForUrls(text: String): ProcessingResult {
        return try {
            val extractedUrls = urlSanitizer.extractUrls(text)

            if (extractedUrls.isNotEmpty()) {
                val originalUrl = extractedUrls.first()
                val sanitizedUrls = urlSanitizer.sanitizeText(text)

                if (sanitizedUrls.isNotEmpty()) {
                    val sanitizedUrl = sanitizedUrls.first()
                    val removedParams = countRemovedParameters(originalUrl, sanitizedUrl)
                    ProcessingResult(
                        success = true,
                        sanitizedUrl = sanitizedUrl,
                        originalUrl = originalUrl,
                        removedParams = removedParams
                    )
                } else {
                    ProcessingResult(
                        success = false,
                        originalUrl = originalUrl,
                        error = COULD_NOT_SANITIZE
                    )
                }
            } else {
                ProcessingResult(
                    success = false,
                    error = NO_URLS_FOUND
                )
            }
        } catch (e: Exception) {
            ProcessingResult(
                success = false,
                error = ERROR_PREFIX + "processing text: ${e.message}"
            )
        }
    }

    fun processUrl(url: String): ProcessingResult {
        return try {
            val sanitizedUrl = urlSanitizer.sanitizeUrl(url)

            if (sanitizedUrl != null) {
                val removedParams = if (sanitizedUrl != url) {
                    countRemovedParameters(url, sanitizedUrl)
                } else {
                    0
                }
                ProcessingResult(
                    success = true,
                    sanitizedUrl = sanitizedUrl,
                    originalUrl = url,
                    removedParams = removedParams
                )
            } else {
                ProcessingResult(
                    success = false,
                    originalUrl = url,
                    error = "Could not process URL"
                )
            }
        } catch (e: Exception) {
            ProcessingResult(
                success = false,
                originalUrl = url,
                error = "Error processing URL: ${e.message}"
            )
        }
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Try to open with Chrome first
            intent.setPackage("com.android.chrome")
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to system default
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            showToast("Failed to open URL: ${e.message}")
        }
    }

    fun handleSharedUrl(sanitizedUrl: String) {
        // Reopen share sheet with sanitized URL for seamless sharing
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, sanitizedUrl)

            // Create chooser to show all available apps
            val chooserIntent = Intent.createChooser(shareIntent, "Share sanitized URL")

            // Start activity safely
            if (context is Activity) {
                context.startActivity(chooserIntent)
            } else {
                // If context is not an Activity, add NEW_TASK flag
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            showToast("Failed to reopen share sheet: ${e.message}")
            // Fallback: open URL directly
            openUrl(sanitizedUrl)
        }
    }

    fun showResultToast(result: ProcessingResult) {
        val message = when {
            result.success && result.removedParams > 0 ->
                "🧹 Removed ${result.removedParams} tracking parameters"
            result.success && result.removedParams == 0 ->
                "✅ URL is already clean"
            !result.success && result.originalUrl != null ->
                "⚠️ ${result.error ?: "Could not process URL"}"
            else ->
                "❌ ${result.error ?: "No URLs found"}"
        }
        showToast(message)
    }

    private fun countRemovedParameters(original: String, sanitized: String): Int {
        return try {
            val originalUri = Uri.parse(original)
            val sanitizedUri = Uri.parse(sanitized)

            val originalParams = originalUri.queryParameterNames?.size ?: 0
            val sanitizedParams = sanitizedUri.queryParameterNames?.size ?: 0

            maxOf(0, originalParams - sanitizedParams)
        } catch (e: Exception) {
            0
        }
    }

    fun extractUrls(text: String): List<String> {
        return urlSanitizer.extractUrls(text)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
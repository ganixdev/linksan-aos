package com.ganixdev.linksan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle different intent types
        handleIntent()
    }

    private fun handleIntent() {
        val intent = intent
        val action = intent.action
        val type = intent.type
        val data = intent.data

        when {
            // Handle URL clicks (sanitize and open)
            Intent.ACTION_VIEW == action && data != null -> {
                val url = data.toString()
                processAndOpenUrl(url)
            }
            // Handle shared text from share menu
            Intent.ACTION_SEND == action && type == "text/plain" -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    processSharedText(sharedText)
                }
            }
            // Regular app launch - show main interface
            else -> {
                // App opened normally, show main screen
            }
        }
    }

    private fun processAndOpenUrl(url: String) {
        try {
            val urlSanitizer = URLSanitizer(this)
            val originalUrl = url
            val sanitizedUrl = urlSanitizer.sanitizeUrl(url)
            
            if (sanitizedUrl != null && sanitizedUrl != originalUrl) {
                // URL had tracking parameters that were removed
                val removedParams = countRemovedParameters(originalUrl, sanitizedUrl)
                openUrlInBrowser(sanitizedUrl)
                showToast("🧹 Removed $removedParams tracking parameters")
            } else if (sanitizedUrl != null) {
                // URL was already clean
                openUrlInBrowser(sanitizedUrl)
                showToast("✅ URL is already clean")
            } else {
                // Couldn't process URL, open as-is
                openUrlInBrowser(originalUrl)
                showToast("⚠️ Couldn't process URL, opened as-is")
            }
        } catch (e: Exception) {
            showToast("❌ Error: ${e.message}")
            // Fallback: open original URL
            openUrlInBrowser(url)
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

    private fun processSharedText(text: String) {
        try {
            val urlSanitizer = URLSanitizer(this)
            val extractedUrls = urlSanitizer.extractUrls(text)
            
            if (extractedUrls.isNotEmpty()) {
                val originalUrl = extractedUrls.first()
                val sanitizedUrls = urlSanitizer.sanitizeText(text)
                
                if (sanitizedUrls.isNotEmpty()) {
                    val sanitizedUrl = sanitizedUrls.first()
                    val removedParams = countRemovedParameters(originalUrl, sanitizedUrl)
                    
                    openUrlInBrowser(sanitizedUrl)
                    
                    if (removedParams > 0) {
                        showToast("🧹 Removed $removedParams tracking parameters")
                    } else {
                        showToast("✅ URL is already clean")
                    }
                } else {
                    // Extracted URL but couldn't sanitize
                    openUrlInBrowser(originalUrl)
                    showToast("⚠️ URL opened without sanitization")
                }
            } else {
                // If no URLs found with sanitizer, try simple text processing
                val cleanedText = text.trim()
                if (isLikelyUrl(cleanedText)) {
                    val processedUrl = if (!cleanedText.startsWith("http://") && !cleanedText.startsWith("https://")) {
                        "https://$cleanedText"
                    } else {
                        cleanedText
                    }
                    openUrlInBrowser(processedUrl)
                    showToast("🔗 Added protocol and opened URL")
                } else {
                    showToast("❌ No URLs found in selected text")
                }
            }
        } catch (e: Exception) {
            showToast("❌ Error: ${e.message}")
        }
    }

    private fun isLikelyUrl(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.contains(".") && 
               (trimmed.startsWith("www.") || 
                trimmed.startsWith("http://") || 
                trimmed.startsWith("https://") ||
                trimmed.matches(Regex(".*\\.[a-zA-Z]{2,}.*")))
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Failed to open URL: ${e.message}")
        }
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            // Exclude LinkSan from handling this intent to avoid loops
            intent.setPackage("com.android.chrome") // Try Chrome first
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // If Chrome not available, use system default
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallbackIntent)
            }
            finish() // Close LinkSan after opening URL
        } catch (e: Exception) {
            showToast("Failed to open URL: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
package com.ganixdev.linksan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var urlProcessor: URLProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlProcessor = URLProcessor(this)

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
        val result = urlProcessor.processUrl(url)
        urlProcessor.showResultToast(result)

        if (result.success && result.sanitizedUrl != null) {
            urlProcessor.openUrl(result.sanitizedUrl)
            finish() // Close LinkSan after opening URL
        } else {
            // Fallback: try to open original URL
            urlProcessor.openUrl(url)
            finish()
        }
    }



    private fun processSharedText(text: String) {
        val result = urlProcessor.processTextForUrls(text)

        if (result.success && result.sanitizedUrl != null) {
            urlProcessor.showResultToast(result)
            // Reopen share sheet with sanitized URL for seamless sharing
            urlProcessor.handleSharedUrl(result.sanitizedUrl)
            // Don't finish() here - let user choose where to share the sanitized URL
        } else {
            urlProcessor.showResultToast(result)
            finish()
        }
    }


}
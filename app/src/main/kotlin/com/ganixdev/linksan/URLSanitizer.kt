package com.ganixdev.linksan

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

class URLSanitizer(private val context: Context) {

    private lateinit var rules: JSONObject
    private val trackingParams = mutableListOf<String>()

    init {
        loadRules()
    }

    private fun loadRules() {
        try {
            val inputStream: InputStream = context.assets.open("rules.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, StandardCharsets.UTF_8)
            rules = JSONObject(jsonString)

            // Load tracking parameters
            val trackingArray = rules.getJSONArray("tracking_params")
            for (i in 0 until trackingArray.length()) {
                trackingParams.add(trackingArray.getString(i))
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to load rules.json", e)
        }
    }

    fun sanitizeText(text: String): List<String> {
        val urls = extractUrls(text)
        return urls.mapNotNull { sanitizeUrlInternal(it) }
    }

    fun sanitizeUrl(url: String): String? {
        return sanitizeUrlInternal(url)
    }

    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        
        // Match URLs with protocol (http:// or https://)
        val protocolUrlRegex = Regex("https?://[^\\s]+")
        val protocolUrls = protocolUrlRegex.findAll(text).map { it.value }
        urls.addAll(protocolUrls)
        
        // Match URLs without protocol - improved regex
        val domainUrlRegex = Regex("(?:^|\\s)((?:www\\.)?[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}(?:/[^\\s]*)?)(?=\\s|$)")
        val domainMatches = domainUrlRegex.findAll(text)
        
        for (match in domainMatches) {
            val url = match.groupValues[1] // Get the captured group, not the whole match
            // Add https:// prefix if not already present
            val processedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            urls.add(processedUrl)
        }
        
        // If no URLs found with regex, try simple fallback for single domain
        if (urls.isEmpty()) {
            val trimmed = text.trim()
            if (trimmed.contains(".") && !trimmed.contains(" ") && trimmed.matches(Regex(".*\\.[a-zA-Z]{2,}.*"))) {
                val processedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    "https://$trimmed"
                } else {
                    trimmed
                }
                urls.add(processedUrl)
            }
        }
        
        // Remove duplicates and return
        return urls.distinct()
    }

    private fun sanitizeUrlInternal(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return null

            // Handle Google redirects
            if (host.contains("google.com") && uri.getQueryParameter("url") != null) {
                val redirectUrl = uri.getQueryParameter("url")
                if (redirectUrl != null) {
                    return sanitizeUrlInternal(redirectUrl)
                }
            }

            // Handle goo.gl redirects
            if (host == "goo.gl" && uri.lastPathSegment != null) {
                // goo.gl URLs are shortened, return as-is since we can't expand them
                return url
            }

            // Handle YouTube shorteners
            if (host == "youtu.be") {
                val videoId = uri.lastPathSegment
                if (videoId != null) {
                    return "https://www.youtube.com/watch?v=$videoId"
                }
            }

            // Apply domain-specific rules
            val domainRules = getDomainRules(host)
            if (domainRules != null) {
                return applyDomainRules(uri, domainRules)
            }

            // Apply general tracking parameter removal
            removeTrackingParams(uri)

        } catch (e: Exception) {
            null
        }
    }

    private fun getDomainRules(host: String): DomainRules? {
        val domains = rules.getJSONObject("domains")
        val domainKeys = domains.keys()

        while (domainKeys.hasNext()) {
            val domain = domainKeys.next()
            if (host.contains(domain)) {
                val domainObj = domains.getJSONObject(domain)
                val keep = mutableListOf<String>()
                val remove = mutableListOf<String>()

                val keepArray = domainObj.getJSONArray("keep")
                for (i in 0 until keepArray.length()) {
                    keep.add(keepArray.getString(i))
                }

                val removeArray = domainObj.getJSONArray("remove")
                for (i in 0 until removeArray.length()) {
                    remove.add(removeArray.getString(i))
                }

                return DomainRules(keep, remove)
            }
        }
        return null
    }

    private fun applyDomainRules(uri: Uri, rules: DomainRules): String {
        val builder = uri.buildUpon()
        builder.clearQuery()

        // Keep specified parameters
        for (param in rules.keep) {
            val value = uri.getQueryParameter(param)
            if (value != null) {
                builder.appendQueryParameter(param, value)
            }
        }

        // Remove specified parameters
        val queryParams = uri.queryParameterNames
        for (param in queryParams) {
            if (!rules.keep.contains(param) && !rules.remove.contains(param)) {
                val value = uri.getQueryParameter(param)
                if (value != null) {
                    builder.appendQueryParameter(param, value)
                }
            }
        }

        return builder.build().toString()
    }

    private fun removeTrackingParams(uri: Uri): String {
        val builder = uri.buildUpon()
        builder.clearQuery()

        val queryParams = uri.queryParameterNames
        for (param in queryParams) {
            if (!trackingParams.contains(param)) {
                val value = uri.getQueryParameter(param)
                if (value != null) {
                    builder.appendQueryParameter(param, value)
                }
            }
        }

        return builder.build().toString()
    }

    data class DomainRules(
        val keep: List<String>,
        val remove: List<String>
    )
}
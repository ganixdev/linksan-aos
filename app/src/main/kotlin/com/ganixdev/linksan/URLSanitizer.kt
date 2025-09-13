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
    private val patterns = mutableListOf<String>()
    private val domainRules = mutableMapOf<String, DomainRules>()

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

            // Load all tracking parameters from trackers object
            loadTrackingParams()

            // Load patterns
            loadPatterns()

            // Load domain-specific rules
            loadDomainRules()

        } catch (e: IOException) {
            throw RuntimeException("Failed to load rules.json", e)
        }
    }

    private fun loadTrackingParams() {
        val trackers = rules.getJSONObject("trackers")
        val categories = trackers.keys()

        while (categories.hasNext()) {
            val category = categories.next()
            val paramsArray = trackers.getJSONArray(category)
            for (i in 0 until paramsArray.length()) {
                trackingParams.add(paramsArray.getString(i))
            }
        }
    }

    private fun loadPatterns() {
        val patternsArray = rules.getJSONArray("patterns")
        for (i in 0 until patternsArray.length()) {
            patterns.add(patternsArray.getString(i))
        }
    }

    private fun loadDomainRules() {
        val domains = rules.getJSONObject("domain_specific")
        val domainKeys = domains.keys()

        while (domainKeys.hasNext()) {
            val domain = domainKeys.next()
            val domainObj = domains.getJSONObject(domain)
            val keep = mutableListOf<String>()
            val remove = mutableListOf<String>()

            if (domainObj.has("keep")) {
                val keepArray = domainObj.getJSONArray("keep")
                for (i in 0 until keepArray.length()) {
                    keep.add(keepArray.getString(i))
                }
            }

            if (domainObj.has("remove")) {
                val removeArray = domainObj.getJSONArray("remove")
                for (i in 0 until removeArray.length()) {
                    remove.add(removeArray.getString(i))
                }
            }

            domainRules[domain] = DomainRules(keep, remove)
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

            // Handle redirect handlers
            val redirectHandler = getRedirectHandler(host)
            if (redirectHandler != null) {
                return handleRedirect(uri, redirectHandler)
            }

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

    private fun getRedirectHandler(host: String): RedirectHandler? {
        if (!rules.has("redirect_handlers")) return null

        val handlers = rules.getJSONObject("redirect_handlers")
        val handlerKeys = handlers.keys()

        while (handlerKeys.hasNext()) {
            val handlerDomain = handlerKeys.next()
            if (host.contains(handlerDomain)) {
                val handlerObj = handlers.getJSONObject(handlerDomain)
                val extractParam = if (handlerObj.has("extract_param")) handlerObj.getString("extract_param") else null
                val decode = handlerObj.optBoolean("decode", false)
                val followRedirect = handlerObj.optBoolean("follow_redirect", false)

                return RedirectHandler(extractParam, decode, followRedirect)
            }
        }
        return null
    }

    private fun handleRedirect(uri: Uri, handler: RedirectHandler): String? {
        return when {
            handler.extractParam != null -> {
                val paramValue = uri.getQueryParameter(handler.extractParam)
                if (paramValue != null) {
                    if (handler.decode) {
                        java.net.URLDecoder.decode(paramValue, "UTF-8")
                    } else {
                        paramValue
                    }
                } else {
                    null
                }
            }
            handler.followRedirect -> {
                // For now, return the URL as-is since we can't actually follow redirects
                // In a full implementation, you'd make an HTTP request here
                uri.toString()
            }
            else -> null
        }
    }

    private fun getDomainRules(host: String): DomainRules? {
        // Check for exact domain match first
        domainRules[host]?.let { return it }

        // Check for subdomain matches
        val domainKeys = domainRules.keys
        for (domain in domainKeys) {
            if (host.contains(domain)) {
                return domainRules[domain]
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
            var shouldKeep = true

            // Check if parameter matches any tracking parameter
            if (trackingParams.contains(param)) {
                shouldKeep = false
            }

            // Check if parameter matches any pattern
            if (shouldKeep) {
                for (pattern in patterns) {
                    if (param.matches(Regex(pattern))) {
                        shouldKeep = false
                        break
                    }
                }
            }

            if (shouldKeep) {
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

    data class RedirectHandler(
        val extractParam: String?,
        val decode: Boolean,
        val followRedirect: Boolean
    )
}
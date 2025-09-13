package com.ganixdev.linksan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class MainActivity : AppCompatActivity() {

    private lateinit var urlProcessor: URLProcessor

    // UI Components
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var etUrlInput: EditText
    private lateinit var btnSanitize: Button
    private lateinit var btnInfo: ImageButton

    // Sections
    private lateinit var trackersSection: LinearLayout
    private lateinit var resultsSection: LinearLayout
    private lateinit var supportSection: LinearLayout

    // Trackers display
    private lateinit var tvTrackerCount: TextView
    private lateinit var trackersContainer: LinearLayout

    // Results display
    private lateinit var singleResultContainer: LinearLayout
    private lateinit var multiResultContainer: LinearLayout
    private lateinit var tvCleanUrl: TextView
    private lateinit var tvSummaryStats: TextView
    private lateinit var individualResultsContainer: LinearLayout

    // Action buttons
    private lateinit var btnCopy: Button
    private lateinit var btnShare: Button
    private lateinit var btnOpen: Button
    private lateinit var btnCopyAll: Button
    private lateinit var btnShareAll: Button

    // State
    private var currentResults: List<URLProcessor.ProcessingResult> = emptyList()
    private var currentRemovedTrackers: List<String> = emptyList()
    private var hasProcessedUrl = false

    // Support URL
    private val coffeeUrl = "https://www.buymeacoffee.com/ganixdev"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlProcessor = URLProcessor(this)
        initializeViews()
        setupListeners()
        setupSwipeRefresh()

        // Handle different intent types
        handleIntent()
    }

    private fun initializeViews() {
        // Main components
        swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        etUrlInput = findViewById<EditText>(R.id.et_url_input)
        btnSanitize = findViewById<Button>(R.id.btn_sanitize)
        btnInfo = findViewById<ImageButton>(R.id.btn_info)

        // Sections
        trackersSection = findViewById<LinearLayout>(R.id.trackers_section)
        resultsSection = findViewById<LinearLayout>(R.id.results_section)
        supportSection = findViewById<LinearLayout>(R.id.support_section)

        // Trackers display
        tvTrackerCount = findViewById<TextView>(R.id.tv_tracker_count)
        trackersContainer = findViewById<LinearLayout>(R.id.trackers_container)

        // Results display
        singleResultContainer = findViewById<LinearLayout>(R.id.single_result_container)
        multiResultContainer = findViewById<LinearLayout>(R.id.multi_result_container)
        tvCleanUrl = findViewById<TextView>(R.id.tv_clean_url)
        tvSummaryStats = findViewById<TextView>(R.id.tv_summary_stats)
        individualResultsContainer = findViewById<LinearLayout>(R.id.individual_results_container)

        // Action buttons
        btnCopy = findViewById<Button>(R.id.btn_copy)
        btnShare = findViewById<Button>(R.id.btn_share)
        btnOpen = findViewById<Button>(R.id.btn_open)
        btnCopyAll = findViewById<Button>(R.id.btn_copy_all)
        btnShareAll = findViewById<Button>(R.id.btn_share_all)
    }

    private fun setupListeners() {
        btnSanitize.setOnClickListener { onSanitizePressed() }
        btnInfo.setOnClickListener { showAboutDialog() }

        // Single result actions
        btnCopy.setOnClickListener { copyToClipboard(tvCleanUrl.text.toString()) }
        btnShare.setOnClickListener { shareUrl(tvCleanUrl.text.toString()) }
        btnOpen.setOnClickListener { openUrl(tvCleanUrl.text.toString()) }

        // Multi result actions
        btnCopyAll.setOnClickListener { copyAllUrls() }
        btnShareAll.setOnClickListener { shareAllUrls() }

        // Support section
        supportSection.setOnClickListener { openUrl(coffeeUrl) }

        // URL input handling
        etUrlInput.setOnEditorActionListener { _, _, _ ->
            onSanitizePressed()
            true
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            clearAllResults()
            swipeRefresh.isRefreshing = false
        }
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

    private fun onSanitizePressed() {
        var text = etUrlInput.text.toString().trim()
        if (text.isEmpty()) {
            // Auto-paste from clipboard
            text = getClipboardText()
            etUrlInput.setText(text)
        }

        if (text.isNotEmpty()) {
            // Extract URLs from the text
            val urls = urlProcessor.extractUrls(text)

            if (urls.isNotEmpty()) {
                processUrls(urls)
            } else {
                Toast.makeText(this, "No valid URLs found. Please provide URLs or domains.", Toast.LENGTH_LONG).show()
                clearAllResults()
            }
        }
    }

    private fun processUrls(urls: List<String>) {
        btnSanitize.isEnabled = false
        btnSanitize.text = "Processing..."

        val results = mutableListOf<URLProcessor.ProcessingResult>()
        val allRemovedTrackers = mutableSetOf<String>()

        for (url in urls) {
            val result = urlProcessor.processUrl(url)
            results.add(result)

            if (result.success && result.removedParams > 0) {
                // Add removed trackers to the set
                val sanitizedUri = Uri.parse(result.sanitizedUrl)
                val originalUri = Uri.parse(url)
                val originalParams = originalUri.queryParameterNames
                val sanitizedParams = sanitizedUri.queryParameterNames

                for (param in originalParams) {
                    if (!sanitizedParams.contains(param)) {
                        allRemovedTrackers.add(param)
                    }
                }
            }
        }

        currentResults = results
        currentRemovedTrackers = allRemovedTrackers.toList()
        hasProcessedUrl = true

        updateUI()

        btnSanitize.isEnabled = true
        btnSanitize.text = "Sanitize URLs"

        // Show summary toast
        val totalRemoved = results.sumOf { it.removedParams }
        val message = if (totalRemoved > 0) {
            "$totalRemoved tracker${if (totalRemoved == 1) "" else "s"} removed from ${results.size} URL${if (results.size == 1) "" else "s"}"
        } else {
            "${results.size} URL${if (results.size == 1) "" else "s"} processed - no trackers found"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        // Show/hide sections
        trackersSection.visibility = if (currentRemovedTrackers.isNotEmpty()) View.VISIBLE else View.GONE
        resultsSection.visibility = if (hasProcessedUrl) View.VISIBLE else View.GONE
        supportSection.visibility = if (hasProcessedUrl) View.VISIBLE else View.GONE

        // Update trackers display
        if (currentRemovedTrackers.isNotEmpty()) {
            tvTrackerCount.text = currentRemovedTrackers.size.toString()
            displayRemovedTrackers()
        }

        // Update results display
        if (currentResults.isNotEmpty()) {
            if (currentResults.size == 1) {
                displaySingleResult()
            } else {
                displayMultiResults()
            }
        }
    }

    private fun displayRemovedTrackers() {
        trackersContainer.removeAllViews()

        for (tracker in currentRemovedTrackers) {
            val chip = Chip(this).apply {
                text = tracker
                setChipBackgroundColorResource(R.color.chip_background_red)
                setTextColor(ContextCompat.getColor(context, R.color.chip_text_red))
                setChipStrokeColorResource(R.color.chip_stroke_red)
                chipStrokeWidth = 1f
                setChipCornerRadiusResource(R.dimen.chip_corner_radius)
                setChipMinHeightResource(R.dimen.chip_min_height)
                setPadding(8, 4, 8, 4)
            }
            trackersContainer.addView(chip)

            val marginParams = chip.layoutParams as LinearLayout.LayoutParams
            marginParams.setMargins(0, 0, 8, 0)
            chip.layoutParams = marginParams
        }
    }

    private fun displaySingleResult() {
        val result = currentResults.first()

        singleResultContainer.visibility = View.VISIBLE
        multiResultContainer.visibility = View.GONE

        tvCleanUrl.text = result.sanitizedUrl ?: result.originalUrl ?: ""
    }

    private fun displayMultiResults() {
        singleResultContainer.visibility = View.GONE
        multiResultContainer.visibility = View.VISIBLE

        val totalRemoved = currentResults.sumOf { it.removedParams }
        tvSummaryStats.text = "${currentResults.size} URLs processed • $totalRemoved tracker${if (totalRemoved == 1) "" else "s"} removed"

        individualResultsContainer.removeAllViews()

        for (result in currentResults) {
            val resultView = createResultView(result)
            individualResultsContainer.addView(resultView)
        }
    }

    private fun createResultView(result: URLProcessor.ProcessingResult): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_result, individualResultsContainer, false)

        val tvDomain = view.findViewById<TextView>(R.id.tv_domain)
        val tvResultUrl = view.findViewById<TextView>(R.id.tv_result_url)
        val tvRemovedCount = view.findViewById<TextView>(R.id.tv_removed_count)
        val btnCopy = view.findViewById<Button>(R.id.btn_item_copy)
        val btnShare = view.findViewById<Button>(R.id.btn_item_share)
        val btnOpen = view.findViewById<Button>(R.id.btn_item_open)

        // Extract domain from URL
        val url = result.sanitizedUrl ?: result.originalUrl ?: ""
        val domain = try {
            Uri.parse(url).host ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        tvDomain.text = domain
        tvResultUrl.text = url

        if (result.removedParams > 0) {
            tvRemovedCount.visibility = View.VISIBLE
            tvRemovedCount.text = "${result.removedParams} removed"
        } else {
            tvRemovedCount.visibility = View.GONE
        }

        btnCopy.setOnClickListener { copyToClipboard(url) }
        btnShare.setOnClickListener { shareUrl(url) }
        btnOpen.setOnClickListener { openUrl(url) }

        return view
    }

    private fun copyAllUrls() {
        val allUrls = currentResults.mapNotNull { it.sanitizedUrl }.joinToString("\n")
        copyToClipboard(allUrls)
        Toast.makeText(this, "All ${currentResults.size} URLs copied!", Toast.LENGTH_SHORT).show()
    }

    private fun shareAllUrls() {
        val allUrls = currentResults.mapNotNull { it.sanitizedUrl }.joinToString("\n")
        shareUrl(allUrls)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("LinkSan URLs", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareUrl(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(shareIntent, "Share URL"))
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Try to open with Chrome first
            intent.setPackage("com.android.chrome")
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to system default
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            return clip.getItemAt(0).text?.toString() ?: ""
        }
        return ""
    }

    private fun clearAllResults() {
        currentResults = emptyList()
        currentRemovedTrackers = emptyList()
        hasProcessedUrl = false

        etUrlInput.text.clear()
        trackersSection.visibility = View.GONE
        resultsSection.visibility = View.GONE
        supportSection.visibility = View.GONE

        trackersContainer.removeAllViews()
        individualResultsContainer.removeAllViews()
    }

    private var aboutSheet: AboutBottomSheet? = null

    private fun showAboutDialog() {
        // Avoid stacking multiple sheets
        if (aboutSheet?.dialog?.isShowing == true) return

        aboutSheet = AboutBottomSheet()
        aboutSheet?.show(supportFragmentManager, "about_sheet")
    }

    fun closeDialog(view: View) {
        // Called by the X button inside dialog_about.xml
        val sheet = aboutSheet
        if (sheet?.dialog?.isShowing == true) {
            sheet.dismiss()
        }
    }

    fun openSupportUrl(view: View) {
        urlProcessor.openUrl("https://github.com/ganixdev/LinkSan")
    }

    fun openIssuesUrl(view: View) {
        urlProcessor.openUrl("https://github.com/ganixdev/LinkSan/issues")
    }
}
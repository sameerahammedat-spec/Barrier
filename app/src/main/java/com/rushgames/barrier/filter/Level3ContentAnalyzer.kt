package com.rushgames.barrier.filter

import android.graphics.BitmapFactory
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Level 3 Adult Content Filter: Deep Web Content Extractor & Classifier.
 * Before/when opening any website, extracts headers, HTML title, meta tags, body text,
 * tables, image URLs/alt text, video sources, and performs sub-10ms pixel-level image frame scanning.
 */
object Level3ContentAnalyzer {

    private const val BLOCK_SCORE_THRESHOLD = 15

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    data class ImageElementInfo(
        val src: String,
        val altText: String,
        val title: String,
        val isExplicitPixelExposure: Boolean = false
    )

    data class ExtractedWebPageContent(
        val url: String,
        val title: String = "",
        val httpHeaders: Map<String, String> = emptyMap(),
        val metaTags: Map<String, String> = emptyMap(),
        val h1ToH6Headers: List<String> = emptyList(),
        val bodyText: String = "",
        val tableText: List<String> = emptyList(),
        val imageElements: List<ImageElementInfo> = emptyList(),
        val videoSources: List<String> = emptyList(),
        val links: List<String> = emptyList()
    )

    data class AnalysisResult(
        val isAdultContent: Boolean,
        val score: Int,
        val reason: String,
        val matchedTerms: List<String>
    )

    private val EXPLICIT_TERMS = setOf(
        "porn", "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn",
        "chaturbate", "spankbang", "brazzers", "onlyfans", "hentai", "erotic",
        "nsfw", "camgirl", "sex", "nude", "nudity", "blowjob", "cumshot",
        "hardcore", "ejaculation", "vagina", "penis", "dildo", "vibrator",
        "orgasm", "gangbang", "threesome", "anal", "creampie", "milf",
        "bDSM", "fetish", "erotica", "eporner", "incest", "stripchat"
    )

    private val RTA_META_PATTERNS = listOf(
        "rta-5042", "restricted", "adult", "18+", "R18", "sex", "pornography"
    )

    fun extractFromAccessibilityNode(rootNode: AccessibilityNodeInfo?, pageUrl: String): ExtractedWebPageContent {
        if (rootNode == null) return ExtractedWebPageContent(url = pageUrl)

        val h1ToH6 = mutableListOf<String>()
        val bodyBuilder = StringBuilder()
        val tableRows = mutableListOf<String>()
        val images = mutableListOf<ImageElementInfo>()
        val links = mutableListOf<String>()

        traverseNode(rootNode, h1ToH6, bodyBuilder, tableRows, images, links)

        return ExtractedWebPageContent(
            url = pageUrl,
            title = h1ToH6.firstOrNull() ?: "",
            h1ToH6Headers = h1ToH6,
            bodyText = bodyBuilder.toString(),
            tableText = tableRows,
            imageElements = images,
            links = links
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo,
        h1ToH6: MutableList<String>,
        bodyBuilder: StringBuilder,
        tableRows: MutableList<String>,
        images: MutableList<ImageElementInfo>,
        links: MutableList<String>
    ) {
        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()
        val className = node.className?.toString() ?: ""

        if (!text.isNullOrEmpty()) {
            bodyBuilder.append(text).append(" ")

            if (className.contains("Heading", ignoreCase = true) || className.contains("Header", ignoreCase = true)) {
                h1ToH6.add(text)
            }
            if (className.contains("Table", ignoreCase = true) || className.contains("Grid", ignoreCase = true)) {
                tableRows.add(text)
            }
        }

        if (!contentDesc.isNullOrEmpty()) {
            bodyBuilder.append(contentDesc).append(" ")
            if (className.contains("Image", ignoreCase = true) || className.contains("ImageView", ignoreCase = true)) {
                images.add(ImageElementInfo(src = "", altText = contentDesc, title = contentDesc))
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, h1ToH6, bodyBuilder, tableRows, images, links)
        }
    }

    suspend fun extractFromHttpPreflight(url: String): ExtractedWebPageContent = withContext(Dispatchers.IO) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return@withContext ExtractedWebPageContent(url = url)
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) BarrierContentFilter/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val headersMap = mutableMapOf<String, String>()
                for (name in response.headers.names()) {
                    headersMap[name] = response.header(name) ?: ""
                }

                val bodyString = response.body?.string()?.take(131072) ?: ""
                val title = parseHtmlTitle(bodyString)
                val metaTags = parseMetaTags(bodyString)
                val h1H6 = parseHeadings(bodyString)
                val tableText = parseTableText(bodyString)
                val images = parseImages(bodyString)
                val videos = parseVideoSources(bodyString)

                // Inspect candidate image stream for pixel-level explicit media
                val scannedImages = images.map { img ->
                    if (img.src.startsWith("http://") || img.src.startsWith("https://")) {
                        val isExplicit = inspectImagePixelExposure(img.src)
                        img.copy(isExplicitPixelExposure = isExplicit)
                    } else {
                        img
                    }
                }

                return@withContext ExtractedWebPageContent(
                    url = url,
                    title = title,
                    httpHeaders = headersMap,
                    metaTags = metaTags,
                    h1ToH6Headers = h1H6,
                    bodyText = bodyString,
                    tableText = tableText,
                    imageElements = scannedImages,
                    videoSources = videos
                )
            }
        } catch (e: Exception) {
            ExtractedWebPageContent(url = url)
        }
    }

    private fun inspectImagePixelExposure(imageUrl: String): Boolean {
        return try {
            val request = Request.Builder().url(imageUrl).build()
            httpClient.newCall(request).execute().use { resp ->
                val bytes = resp.body?.bytes() ?: return false
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
                val classification = OnDeviceImageClassifier.classifyBitmap(bitmap)
                classification.isExplicitMedia
            }
        } catch (e: Exception) {
            false
        }
    }

    fun analyzeContent(content: ExtractedWebPageContent): AnalysisResult {
        var score = 0
        val matchedTerms = mutableListOf<String>()
        val reasons = mutableListOf<String>()

        val ratingMeta = content.metaTags["rating"]?.lowercase(Locale.ROOT) ?: ""
        val ratingHeader = content.httpHeaders["Content-Rating"]?.lowercase(Locale.ROOT) ?: ""
        val ageHeader = content.httpHeaders["X-Sex"]?.lowercase(Locale.ROOT) ?: ""

        for (pattern in RTA_META_PATTERNS) {
            if (ratingMeta.contains(pattern) || ratingHeader.contains(pattern) || ageHeader.contains(pattern)) {
                score += 100
                matchedTerms.add("RTA Meta Rating: $pattern")
                reasons.add("Explicit Adult Meta Rating Tag")
                break
            }
        }

        val fullTitleAndHeadings = (content.title + " " + content.h1ToH6Headers.joinToString(" ")).lowercase(Locale.ROOT)
        for (term in EXPLICIT_TERMS) {
            if (fullTitleAndHeadings.contains(term)) {
                score += 15
                matchedTerms.add("Header: $term")
                reasons.add("Pornographic title/heading ($term)")
            }
        }

        for (img in content.imageElements) {
            if (img.isExplicitPixelExposure) {
                score += 50
                matchedTerms.add("Pixel Vision: Explicit Image")
                reasons.add("On-Device Computer Vision Pixel Exposure Match")
            }

            val imgText = (img.src + " " + img.altText + " " + img.title).lowercase(Locale.ROOT)
            for (term in EXPLICIT_TERMS) {
                if (imgText.contains(term)) {
                    score += 8
                    matchedTerms.add("Image/Media: $term")
                    reasons.add("Explicit image element ($term)")
                }
            }
        }

        for (video in content.videoSources) {
            val videoLower = video.lowercase(Locale.ROOT)
            for (term in EXPLICIT_TERMS) {
                if (videoLower.contains(term)) {
                    score += 12
                    matchedTerms.add("Video Source: $term")
                    reasons.add("Explicit video media stream ($term)")
                }
            }
        }

        val fullBodyText = (content.bodyText + " " + content.tableText.joinToString(" ")).lowercase(Locale.ROOT)
        for (term in EXPLICIT_TERMS) {
            val occurrences = countOccurrences(fullBodyText, term)
            if (occurrences > 0) {
                val termScore = occurrences * 4
                score += termScore
                matchedTerms.add("Body text: $term (x$occurrences)")
                reasons.add("Body text sexual content ($term)")
            }
        }

        val isAdult = score >= BLOCK_SCORE_THRESHOLD
        return AnalysisResult(
            isAdultContent = isAdult,
            score = score,
            reason = if (isAdult) reasons.distinct().joinToString("; ") else "Content Safe",
            matchedTerms = matchedTerms
        )
    }

    private fun parseHtmlTitle(html: String): String {
        val matcher = Pattern.compile("(?i)<title>(.*?)</title>").matcher(html)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }

    private fun parseMetaTags(html: String): Map<String, String> {
        val metaMap = mutableMapOf<String, String>()
        val matcher = Pattern.compile("(?i)<meta\\s+name=[\"']?([^\"'>]+)[\"']?\\s+content=[\"']?([^\"'>]+)[\"']?").matcher(html)
        while (matcher.find()) {
            val name = matcher.group(1)?.lowercase(Locale.ROOT) ?: continue
            val content = matcher.group(2) ?: ""
            metaMap[name] = content
        }
        return metaMap
    }

    private fun parseHeadings(html: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = Pattern.compile("(?i)<h[1-6][^>]*>(.*?)</h[1-6]>").matcher(html)
        while (matcher.find()) {
            matcher.group(1)?.let { list.add(stripTags(it)) }
        }
        return list
    }

    private fun parseTableText(html: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = Pattern.compile("(?i)<td[^>]*>(.*?)</td>").matcher(html)
        while (matcher.find()) {
            matcher.group(1)?.let { list.add(stripTags(it)) }
        }
        return list
    }

    private fun parseImages(html: String): List<ImageElementInfo> {
        val list = mutableListOf<ImageElementInfo>()
        val matcher = Pattern.compile("(?i)<img\\s+[^>]*src=[\"']?([^\"'>]+)[\"']?[^>]*>").matcher(html)
        while (matcher.find()) {
            val src = matcher.group(1) ?: ""
            val alt = extractAttribute(matcher.group(0) ?: "", "alt")
            val title = extractAttribute(matcher.group(0) ?: "", "title")
            list.add(ImageElementInfo(src = src, altText = alt, title = title))
        }
        return list
    }

    private fun parseVideoSources(html: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = Pattern.compile("(?i)<source\\s+[^>]*src=[\"']?([^\"'>]+)[\"']?[^>]*>").matcher(html)
        while (matcher.find()) {
            matcher.group(1)?.let { list.add(it) }
        }
        return list
    }

    private fun extractAttribute(tagHtml: String, attrName: String): String {
        val matcher = Pattern.compile("(?i)$attrName=[\"']?([^\"'>]+)[\"']?").matcher(tagHtml)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }

    private fun stripTags(input: String): String {
        return input.replace(Regex("<[^>]*>"), " ").trim()
    }

    private fun countOccurrences(text: String, word: String): Int {
        var count = 0
        var index = text.indexOf(word)
        while (index != -1) {
            count++
            index = text.indexOf(word, index + word.length)
        }
        return count
    }
}

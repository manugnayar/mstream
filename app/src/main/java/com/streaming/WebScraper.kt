package com.streaming

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class WebScraper(private val baseHost: String = "https://www.5movierulz.clothing") {
    
    private val languageUrls = mapOf(
        "Malayalam" to "$baseHost/category/malayalam-movie-2025/",
        "Hollywood" to "$baseHost/category/hollywood-movie-2025/",
        "Tamil" to "$baseHost/category/tamil-movies-2025/",
        "Bollywood" to "$baseHost/category/bollywood-movie-2025/",
        "Telugu" to "$baseHost/category/telugu-movies-2025/"
    )
    
    suspend fun scrapeMovies(language: String = "Malayalam", page: Int = 1): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = languageUrls[language] ?: languageUrls["Malayalam"]!!
            val urlString = if (page == 1) baseUrl else "${baseUrl}page/$page/"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val html = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            parseMoviesFromHtml(html)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun parseMoviesFromHtml(html: String): List<Movie> {
        val movies = mutableListOf<Movie>()
        
        // Pattern to match movie items in the list
        val moviePattern = Pattern.compile(
            "<li>\\s*<div class=\"boxed film\">\\s*<div class=\"cont_display\">\\s*<a href=\"([^\"]+)\"[^>]*>\\s*<img[^>]*src=\"([^\"]+)\"[^>]*alt=\"([^\"]*?)\"[^>]*>\\s*</a>\\s*</div>\\s*<p><b>([^<]+)</b></p>",
            Pattern.DOTALL
        )
        
        val matcher = moviePattern.matcher(html)
        var id = 1
        
        while (matcher.find()) {
            val movieUrl = matcher.group(1)?.trim() ?: ""
            val imageUrl = matcher.group(2)?.trim() ?: ""
            val altText = matcher.group(3)?.trim() ?: ""
            val title = matcher.group(4)?.trim() ?: ""
            
            if (imageUrl.isNotEmpty() && title.isNotEmpty()) {
                movies.add(
                    Movie(
                        id = id++,
                        title = cleanTitle(title),
                        thumbnailUrl = imageUrl,
                        description = "Malayalam Movie 2025",
                        year = "2025",
                        movieUrl = movieUrl
                    )
                )
            }
        }
        
        return movies
    }
    
    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\(\\d{4}\\).*"), "")
            .replace(Regex("HDRip.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("Malayalam.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("Full Movie.*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    suspend fun searchMovies(query: String): List<Movie> = withContext(Dispatchers.IO) {
        try {
            val urlString = "$baseHost/?s=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val html = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            parseMoviesFromHtml(html)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
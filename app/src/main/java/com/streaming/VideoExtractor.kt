package com.streaming

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class VideoExtractor {
    
    suspend fun getStreamingUrl(movieUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val html = fetchPageContent(movieUrl)
            
            // Extract JavaScript locations array - exact pattern from HTML
            val locationsPattern = "var locations = \\[\"([^\"]+)\""
            val locationsMatch = Pattern.compile(locationsPattern).matcher(html)
            if (locationsMatch.find()) {
                val rawUrl = locationsMatch.group(1)?.replace("\\/", "/")?.replace("&amp;", "&") ?: ""
                println("Found locations URL: $rawUrl")
                return@withContext rawUrl
            }
            
            return@withContext ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    suspend fun getTorrentMagnet(movieUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val html = fetchPageContent(movieUrl)
            println("HTML length: ${html.length}")
            
            // Extract magnet link - look for href with magnet
            val magnetPattern = "href=\"(magnet:\\?xt=urn:btih:[^\"]+)\""
            val magnetMatch = Pattern.compile(magnetPattern).matcher(html)
            if (magnetMatch.find()) {
                val magnetUrl = magnetMatch.group(1)
                    ?.replace("&amp;", "&")
                    ?.replace("&#038;", "&") ?: ""
                println("Found magnet URL: ${magnetUrl.take(100)}...")
                return@withContext magnetUrl
            }
            
            println("No magnet link found in HTML")
            return@withContext ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    suspend fun extractVideoUrl(movieUrl: String): String = withContext(Dispatchers.IO) {
        try {
            // Test with a working HLS stream first
            if (movieUrl.contains("test")) {
                return@withContext "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
            }
            
            val html = fetchPageContent(movieUrl)
            println("Fetched HTML length: ${html.length}")
            
            val streamUrl = extractStreamingUrl(html)
            println("Extracted stream URL: $streamUrl")
            
            if (streamUrl.isNotEmpty()) {
                val resolvedUrl = resolveStreamingUrl(streamUrl)
                println("Resolved URL: $resolvedUrl")
                return@withContext resolvedUrl
            }
            
            return@withContext ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
    
    private fun fetchPageContent(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        connection.setRequestProperty("Referer", "https://www.5movierulz.clothing/")
        
        return BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
    }
    
    private fun extractStreamingUrl(html: String): String {
        // Extract HLS URLs from vcdnlare.com pattern
        val hlsPatterns = arrayOf(
            "src=\"(https://hls[^\"]*vcdnx\\.com[^\"]*)\"",
            "src=\"([^\"]*\\.m3u8[^\"]*)\"",
            "file:\"([^\"]*\\.m3u8[^\"]*)\"",
            "var locations = \\[\"([^\"]+)\"\\]"
        )
        
        for (pattern in hlsPatterns) {
            val matcher = Pattern.compile(pattern).matcher(html)
            if (matcher.find()) {
                val url = matcher.group(1)?.replace("\\/", "/")?.replace("\\u0026", "&") ?: ""
                if (url.isNotEmpty()) return url
            }
        }
        
        return ""
    }
    
    private fun resolveStreamingUrl(streamUrl: String): String {
        try {
            when {
                streamUrl.endsWith(".m3u8") -> return streamUrl
                streamUrl.endsWith(".mp4") -> return streamUrl
                streamUrl.contains("hls2.vcdnx.com") -> return streamUrl // Direct HLS URL
                streamUrl.contains("vcdnlare.com") -> return resolveVcdnlare(streamUrl)
                streamUrl.contains("vcdnx.com") || streamUrl.contains("hls") -> return resolveHlsStream(streamUrl)
                streamUrl.contains("streamtape") -> return resolveStreamtape(streamUrl)
                streamUrl.contains("uperbox") -> return resolveUperbox(streamUrl)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
    
    private fun resolveHlsStream(url: String): String {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Referer", "https://www.5movierulz.clothing/")
            
            val html = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            
            // Extract HLS M3U8 playlist URLs
            val hlsPatterns = arrayOf(
                "src=\"([^\"]*hls[^\"]*\\.m3u8[^\"]*)\"",
                "file:\"([^\"]*\\.m3u8[^\"]*)\"",
                "https://[^\"\\s]*hls[^\"\\s]*\\.m3u8[^\"\\s]*"
            )
            
            for (pattern in hlsPatterns) {
                val matcher = Pattern.compile(pattern).matcher(html)
                if (matcher.find()) {
                    return matcher.group(1) ?: (matcher.group(0) ?: "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
    
    private fun resolveVcdnlare(url: String): String {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Referer", "https://www.5movierulz.clothing/")
            
            val html = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            
            // Extract the exact hls2.vcdnx.com pattern from dev tools
            val hlsPattern = "src=\"(https://hls[0-9]*\\.vcdnx\\.com/hls/[^\"]*)\""
            val matcher = Pattern.compile(hlsPattern).matcher(html)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
            
            return ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
    
    private fun resolveStreamtape(url: String): String {
        // Streamtape requires specific handling
        return ""
    }
    
    private fun resolveUperbox(url: String): String {
        // Uperbox requires specific handling  
        return ""
    }
}
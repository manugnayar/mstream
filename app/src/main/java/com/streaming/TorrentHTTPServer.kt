package com.streaming

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class TorrentHTTPServer(private val port: Int = 8080) : NanoHTTPD(port) {
    private var videoFile: File? = null
    
    fun setVideoFile(file: File) {
        videoFile = file
        Log.d("HTTPServer", "Video file set: ${file.absolutePath}")
    }
    
    override fun serve(session: IHTTPSession): Response {
        val file = videoFile ?: return newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            "text/plain",
            "Video not ready"
        )
        
        if (!file.exists()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "File not found"
            )
        }
        
        val fileSize = file.length()
        val rangeHeader = session.headers["range"]
        
        return if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            handleRangeRequest(file, fileSize, rangeHeader)
        } else {
            handleFullRequest(file, fileSize)
        }
    }
    
    private fun handleRangeRequest(file: File, fileSize: Long, rangeHeader: String): Response {
        val range = rangeHeader.substring(6).split("-")
        val start = range[0].toLongOrNull() ?: 0
        val end = if (range.size > 1 && range[1].isNotEmpty()) {
            range[1].toLong()
        } else {
            fileSize - 1
        }
        
        val length = end - start + 1
        val inputStream = FileInputStream(file)
        inputStream.skip(start)
        
        val response = newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT,
            "video/mp4",
            inputStream,
            length
        )
        
        response.addHeader("Content-Range", "bytes $start-$end/$fileSize")
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", length.toString())
        
        return response
    }
    
    private fun handleFullRequest(file: File, fileSize: Long): Response {
        val response = newFixedLengthResponse(
            Response.Status.OK,
            "video/mp4",
            FileInputStream(file),
            fileSize
        )
        
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Content-Length", fileSize.toString())
        
        return response
    }
}

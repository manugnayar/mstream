package com.streaming

import android.util.Log
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import java.io.File

class TorrentStreamEngine(private val saveDir: File) {
    private var torrentStream: TorrentStream? = null
    private var httpServer: TorrentHTTPServer? = null
    private var listener: StreamListener? = null
    
    interface StreamListener {
        fun onMetadataReceived(videoFile: File)
        fun onProgress(progress: Float, downloadRate: Int)
        fun onReady(httpUrl: String)
        fun onError(error: String)
    }
    
    fun start(magnetUri: String, listener: StreamListener) {
        this.listener = listener
        
        httpServer = TorrentHTTPServer(8080)
        httpServer?.start()
        
        val torrentOptions = TorrentOptions.Builder()
            .saveLocation(saveDir)
            .removeFilesAfterStop(true)
            .maxActiveTorrents(1)
            .build()
        
        torrentStream = TorrentStream.init(torrentOptions)
        torrentStream?.addListener(object : TorrentListener {
            override fun onStreamReady(torrent: com.github.se_bastiaan.torrentstream.Torrent) {
                val videoFile = torrent.videoFile
                if (videoFile != null && videoFile.exists()) {
                    httpServer?.setVideoFile(videoFile)
                    listener.onReady("http://127.0.0.1:8080")
                }
            }
            
            override fun onStreamProgress(torrent: com.github.se_bastiaan.torrentstream.Torrent, status: com.github.se_bastiaan.torrentstream.StreamStatus) {
                listener.onProgress(status.progress, status.downloadSpeed.toInt())
            }
            
            override fun onStreamPrepared(torrent: com.github.se_bastiaan.torrentstream.Torrent) {
                val videoFile = torrent.videoFile
                if (videoFile != null) {
                    listener.onMetadataReceived(videoFile)
                }
            }
            
            override fun onStreamStarted(torrent: com.github.se_bastiaan.torrentstream.Torrent) {}
            override fun onStreamStopped() {}
            
            override fun onStreamError(torrent: com.github.se_bastiaan.torrentstream.Torrent?, ex: Exception) {
                listener.onError(ex.message ?: "Stream error")
            }
        })
        
        torrentStream?.startStream(magnetUri)
    }
    
    fun stop() {
        torrentStream?.stopStream()
        httpServer?.stop()
        cleanupTorrentFiles()
    }
    
    private fun cleanupTorrentFiles() {
        try {
            saveDir.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Log.e("TorrentEngine", "Cleanup error: ${e.message}")
        }
    }
}

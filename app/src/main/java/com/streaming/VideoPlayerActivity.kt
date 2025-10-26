package com.streaming

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.github.se_bastiaan.torrentstream.TorrentOptions
import com.github.se_bastiaan.torrentstream.TorrentStream
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private var hasStartedPlayback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supportActionBar?.hide()
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        val container = FrameLayout(this)
        playerView = PlayerView(this)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressText = TextView(this)
        
        progressBar.max = 100
        progressText.text = "Loading..."
        progressText.setTextColor(android.graphics.Color.WHITE)
        progressText.textSize = 16f
        
        val progressParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(50, 0, 50, 100)
            gravity = android.view.Gravity.BOTTOM
        }
        
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        }
        
        container.addView(playerView)
        container.addView(progressBar, progressParams)
        container.addView(progressText, textParams)
        
        setContentView(container)
        
        val movieUrl = intent.getStringExtra("MOVIE_URL") ?: return
        extractAndStartStream(movieUrl)
    }
    
    private fun extractAndStartStream(movieUrl: String) {
        progressText.text = "Extracting torrent..."
        Log.d("VideoPlayer", "Movie URL: $movieUrl")
        
        CoroutineScope(Dispatchers.IO).launch {
            val videoExtractor = VideoExtractor()
            val magnetLink = videoExtractor.getTorrentMagnet(movieUrl)
            Log.d("VideoPlayer", "Magnet link: $magnetLink")
            
            withContext(Dispatchers.Main) {
                if (magnetLink.isNotEmpty()) {
                    progressText.text = "Starting torrent..."
                    startTorrentStream(magnetLink)
                } else {
                    progressText.text = "Torrent not found"
                    Log.e("VideoPlayer", "No magnet link found")
                }
            }
        }
    }
    
    private fun startTorrentStream(magnetLink: String) {
        Log.d("VideoPlayer", "Starting stream with magnet: ${magnetLink.take(100)}...")
        
        val torrentOptions = TorrentOptions.Builder()
            .saveLocation(File(filesDir, "torrents"))
            .removeFilesAfterStop(false)
            .build()
            
        val torrentStream = TorrentStream.init(torrentOptions)
        Log.d("VideoPlayer", "TorrentStream initialized")
        
        torrentStream.addListener(object : TorrentListener {
            override fun onStreamReady(torrent: com.github.se_bastiaan.torrentstream.Torrent) {
                Log.d("VideoPlayer", "Stream ready")
                if (!hasStartedPlayback) {
                    hasStartedPlayback = true
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        progressText.visibility = View.GONE
                        val videoFile = torrent.videoFile
                        if (videoFile != null && videoFile.exists()) {
                            initializePlayer("file://" + videoFile.absolutePath)
                        }
                    }
                }
            }
            
            override fun onStreamProgress(torrent: com.github.se_bastiaan.torrentstream.Torrent, status: com.github.se_bastiaan.torrentstream.StreamStatus) {
                runOnUiThread {
                    progressBar.progress = status.progress.toInt()
                    progressText.text = "Downloading: ${String.format("%.2f", status.progress)}%"
                }
                
                if (!hasStartedPlayback && status.progress >= 2.0f) {
                    hasStartedPlayback = true
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        progressText.visibility = View.GONE
                        val videoFile = torrent.videoFile
                        if (videoFile != null && videoFile.exists()) {
                            initializePlayer("file://" + videoFile.absolutePath)
                        }
                    }
                }
            }
            
            override fun onStreamStopped() {
                Log.d("VideoPlayer", "Stream stopped")
            }
            
            override fun onStreamPrepared(torrent: com.github.se_bastiaan.torrentstream.Torrent) {
                Log.d("VideoPlayer", "Stream prepared")
                runOnUiThread {
                    progressText.text = "Torrent prepared"
                }
            }
            
            override fun onStreamStarted(torrent: com.github.se_bastiaan.torrentstream.Torrent) {
                Log.d("VideoPlayer", "Stream started")
                runOnUiThread {
                    progressText.text = "Downloading..."
                }
            }
            
            override fun onStreamError(torrent: com.github.se_bastiaan.torrentstream.Torrent?, ex: Exception) {
                Log.e("VideoPlayer", "Stream error", ex)
                runOnUiThread {
                    progressText.text = "Error: ${ex.message}"
                }
            }
        })
        
        torrentStream.startStream(magnetLink)
    }
    
    private fun initializePlayer(streamUrl: String) {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        
        val mediaItem = MediaItem.fromUri(streamUrl)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                player?.seekBack()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                player?.seekForward()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

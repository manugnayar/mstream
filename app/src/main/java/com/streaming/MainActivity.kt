package com.streaming

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

class MainActivity : AppCompatActivity() {

    private lateinit var movieAdapter: MovieAdapter
    private val webScraper = WebScraper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.movies_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        movieAdapter = MovieAdapter(emptyList()) { movie ->
            openVideoPlayer(movie)
        }
        recyclerView.adapter = movieAdapter

        loadMovies()
    }

    private fun loadMovies() {
        CoroutineScope(Dispatchers.IO).launch {
            val movies = webScraper.scrapeMovies()
            withContext(Dispatchers.Main) {
                movieAdapter.updateMovies(movies)
            }
        }
    }

    private fun openVideoPlayer(movie: Movie) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("MOVIE_URL", movie.movieUrl)
        intent.putExtra("MOVIE_TITLE", movie.title)
        startActivity(intent)
    }
}
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
import android.widget.TextView
import android.widget.EditText
import android.content.Context
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private lateinit var movieAdapter: MovieAdapter
    private lateinit var webScraper: WebScraper
    private var currentLanguage = "Malayalam"
    private var currentPage = 1
    private var isLoading = false
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("mstream_config", Context.MODE_PRIVATE)
        val baseHost = prefs.getString("base_host", "https://www.5movierulz.clothing") ?: "https://www.5movierulz.clothing"
        webScraper = WebScraper(baseHost)

        val recyclerView = findViewById<RecyclerView>(R.id.movies_recycler_view)
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        recyclerView.layoutManager = gridLayoutManager

        movieAdapter = MovieAdapter(emptyList()) { movie ->
            openVideoPlayer(movie)
        }
        recyclerView.adapter = movieAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = gridLayoutManager.childCount
                val totalItemCount = gridLayoutManager.itemCount
                val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 6) {
                    loadMoreMovies()
                }
            }
        })

        findViewById<TextView>(R.id.genres_button).setOnClickListener {
            showLanguageDialog()
        }

        findViewById<TextView>(R.id.search_button).setOnClickListener {
            showSearchDialog()
        }

        findViewById<TextView>(R.id.menu_button).setOnClickListener {
            showMenuDialog()
        }

        loadMovies(currentLanguage)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Malayalam", "Hollywood", "Tamil", "Bollywood", "Telugu")
        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages) { _, which ->
                currentLanguage = languages[which]
                currentPage = 1
                loadMovies(currentLanguage)
            }
            .show()
    }

    private fun loadMovies(language: String) {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            val movies = webScraper.scrapeMovies(language, 1)
            withContext(Dispatchers.Main) {
                movieAdapter.updateMovies(movies)
                currentPage = 2
                isLoading = false
            }
        }
    }

    private fun loadMoreMovies() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            val newMovies = webScraper.scrapeMovies(currentLanguage, currentPage)
            withContext(Dispatchers.Main) {
                if (newMovies.isNotEmpty()) {
                    movieAdapter.addMovies(newMovies)
                    currentPage++
                }
                isLoading = false
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(this)
        input.hint = "Enter movie name"
        input.setPadding(50, 30, 50, 30)
        AlertDialog.Builder(this)
            .setTitle("Search Movies")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString()
                if (query.isNotEmpty()) {
                    searchMovies(query)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun searchMovies(query: String) {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            val movies = webScraper.searchMovies(query)
            withContext(Dispatchers.Main) {
                movieAdapter.updateMovies(movies)
                isLoading = false
            }
        }
    }

    private fun showMenuDialog() {
        val options = arrayOf("Configure Host")
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showConfigHostDialog()
                }
            }
            .show()
    }

    private fun showConfigHostDialog() {
        val currentHost = prefs.getString("base_host", "https://www.5movierulz.clothing") ?: "https://www.5movierulz.clothing"
        val input = EditText(this)
        input.setText(currentHost)
        input.setPadding(50, 30, 50, 30)
        AlertDialog.Builder(this)
            .setTitle("Configure Host")
            .setMessage("Current: $currentHost")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newHost = input.text.toString().trim()
                if (newHost.isNotEmpty()) {
                    prefs.edit().putString("base_host", newHost).apply()
                    webScraper = WebScraper(newHost)
                    loadMovies(currentLanguage)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openVideoPlayer(movie: Movie) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("MOVIE_URL", movie.movieUrl)
        intent.putExtra("MOVIE_TITLE", movie.title)
        startActivity(intent)
    }
}
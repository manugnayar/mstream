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
import android.widget.Button
import android.widget.ImageView
import android.content.Context
import androidx.appcompat.app.AlertDialog
import android.view.KeyEvent

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
        val spanCount = if (packageManager.hasSystemFeature("android.software.leanback")) 6 else 3
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this, spanCount)
        recyclerView.layoutManager = gridLayoutManager

        movieAdapter = MovieAdapter(emptyList()) { movie ->
            showMovieDetailsDialog(movie)
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> true
            else -> super.onKeyDown(keyCode, event)
        }
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

    private fun showMovieDetailsDialog(movie: Movie) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_movie_details, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                true
            } else {
                false
            }
        }

        dialogView.findViewById<TextView>(R.id.detail_title).text = movie.title
        dialogView.findViewById<TextView>(R.id.detail_year).text = "${movie.year} | ${movie.language}"
        
        if (movie.genres.isNotEmpty()) {
            dialogView.findViewById<TextView>(R.id.detail_genres).text = movie.genres
        }
        
        if (movie.description.isNotEmpty()) {
            dialogView.findViewById<TextView>(R.id.detail_description).text = movie.description
        } else {
            dialogView.findViewById<TextView>(R.id.detail_description).text = "Click to view full details"
        }
        
        if (movie.cast.isNotEmpty()) {
            dialogView.findViewById<TextView>(R.id.detail_cast).text = "Cast: ${movie.cast}"
        }
        
        if (movie.director.isNotEmpty()) {
            dialogView.findViewById<TextView>(R.id.detail_director).text = "Director: ${movie.director}"
        }
        
        val backdrop = dialogView.findViewById<ImageView>(R.id.detail_backdrop)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeStream(
                    java.net.URL(movie.thumbnailUrl).openConnection().getInputStream()
                )
                withContext(Dispatchers.Main) {
                    backdrop.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dialogView.findViewById<TextView>(R.id.detail_close).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.detail_play_button).setOnClickListener {
            dialog.dismiss()
            openVideoPlayer(movie)
        }

        dialogView.findViewById<TextView>(R.id.detail_description).setOnClickListener {
            fetchAndShowFullDetails(movie)
        }

        dialog.show()
        dialogView.findViewById<Button>(R.id.detail_play_button).requestFocus()
    }

    private fun fetchAndShowFullDetails(movie: Movie) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val details = webScraper.fetchMovieDetails(movie.movieUrl)
                withContext(Dispatchers.Main) {
                    showFullDetails(movie, details)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showFullDetails(movie, null)
                }
            }
        }
    }

    private fun showFullDetails(movie: Movie, details: Map<String, String>?) {
        val message = buildString {
            append("Title: ${movie.title}\n")
            append("Year: ${movie.year}\n")
            if (details != null) {
                details["director"]?.let { append("Director: $it\n") }
                details["cast"]?.let { append("Cast: $it\n") }
                details["genres"]?.let { append("Genres: $it\n") }
                details["language"]?.let { append("Language: $it\n") }
                details["country"]?.let { append("Country: $it\n") }
                details["description"]?.let { append("\nPlot:\n$it") }
            } else {
                append("\nDescription: ${movie.description}")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(movie.title)
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun openVideoPlayer(movie: Movie) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("MOVIE_URL", movie.movieUrl)
        intent.putExtra("MOVIE_TITLE", movie.title)
        startActivity(intent)
    }
}
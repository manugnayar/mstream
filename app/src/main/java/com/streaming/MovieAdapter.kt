package com.streaming

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory

class MovieAdapter(private var movies: List<Movie>, private val onMovieClick: (Movie) -> Unit) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.movie_title)
        val thumbnail: ImageView = view.findViewById(R.id.movie_poster)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.movie_card, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        
        loadThumbnail(movie.thumbnailUrl, holder.thumbnail)
        
        holder.itemView.setOnClickListener {
            onMovieClick(movie)
        }
        
        holder.itemView.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val rect = android.graphics.Rect()
                view.getLocalVisibleRect(rect)
                view.parent.requestChildRectangleOnScreen(view, rect, false)
            }
        }
        
        if (position < 6) {
            holder.itemView.nextFocusUpId = when (position % 6) {
                0 -> R.id.menu_button
                in 1..3 -> R.id.genres_button
                else -> R.id.search_button
            }
        }
    }

    private fun loadThumbnail(url: String, imageView: ImageView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMovies(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    fun addMovies(newMovies: List<Movie>) {
        val oldSize = movies.size
        movies = movies + newMovies
        notifyItemRangeInserted(oldSize, newMovies.size)
    }

    override fun getItemCount() = movies.size
}
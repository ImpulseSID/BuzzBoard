package com.impulse.buzzboard

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import android.widget.Toast
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var rvHeadlines: RecyclerView
    private lateinit var headlinesAdapter: HeadlinesAdapter
    private var newsDataApiKey: String = BuildConfig.NEWS_DATA_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        rvHeadlines = findViewById(R.id.rvHeadlines)
        rvHeadlines.layoutManager = LinearLayoutManager(this)
        headlinesAdapter = HeadlinesAdapter { article ->
            val intent = android.content.Intent(this, ArticleWebViewActivity::class.java)
            intent.putExtra("url", article.link)
            startActivity(intent)
        }
        rvHeadlines.adapter = headlinesAdapter

        // Fetch headlines for default category
        fetchHeadlines("top")

        navigationView.setNavigationItemSelectedListener { menuItem ->
            val category = when (menuItem.itemId) {
                R.id.category_general -> "top"
                R.id.category_business -> "business"
                R.id.category_entertainment -> "entertainment"
                R.id.category_health -> "health"
                R.id.category_science -> "science"
                R.id.category_sports -> "sports"
                R.id.category_technology -> "technology"
                else -> "top"
            }
            fetchHeadlines(category)
            drawerLayout.closeDrawers()
            true
        }
    }


    private fun fetchHeadlines(category: String) {
        val retrofitNewsData = Retrofit.Builder()
            .baseUrl("https://newsdata.io/api/1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val newsDataService = retrofitNewsData.create(NewsDataApiService::class.java)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("API_KEY", "Using API Key: $newsDataApiKey")
                val newsDataResponse = newsDataService.getTopHeadlines(newsDataApiKey, category, "en", "us")
                Log.d("API_RESPONSE", "Status: ${newsDataResponse.status}, Articles: ${newsDataResponse.results?.size}")
                val newsDataArticles = newsDataResponse.results ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (newsDataResponse.status == "success" && newsDataArticles.isNotEmpty()) {
                        headlinesAdapter.submitList(newsDataArticles)
                        Toast.makeText(this@MainActivity, "Loaded ${newsDataArticles.size} articles from newsdata.io", Toast.LENGTH_LONG).show()
                    } else {
                        headlinesAdapter.submitList(emptyList())
                        Toast.makeText(this@MainActivity, "No news found for this category.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    headlinesAdapter.submitList(emptyList())
                    Toast.makeText(this@MainActivity, "Failed to fetch news: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace() // Add this for debugging
                }
            }
        }
    }
}

interface NewsDataApiService {
    @GET("news")
    suspend fun getTopHeadlines(
        @Query("apikey") apiKey: String,
        @Query("category") category: String,
        @Query("language") language: String = "en",
        @Query("country") country: String = "us"
    ): NewsDataApiResponse
}

data class NewsDataApiResponse(
    val status: String,
    val results: List<NewsDataArticle>?
)

data class NewsDataArticle(
    val title: String?,
    val description: String?,
    val image_url: String?,
    val link: String?
)

class HeadlinesAdapter(private val onItemClick: (NewsDataArticle) -> Unit) : RecyclerView.Adapter<HeadlinesViewHolder>() {
    private var articles: List<NewsDataArticle> = emptyList()
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HeadlinesViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_headline, parent, false)
        return HeadlinesViewHolder(view, onItemClick)
    }
    override fun getItemCount(): Int = articles.size
    override fun onBindViewHolder(holder: HeadlinesViewHolder, position: Int) {
        holder.bind(articles[position])
    }
    fun submitList(list: List<NewsDataArticle>?) {
        articles = list ?: emptyList()
        notifyItemRangeChanged(0, articles.size)
    }
}

class HeadlinesViewHolder(itemView: android.view.View, private val onItemClick: (NewsDataArticle) -> Unit) : RecyclerView.ViewHolder(itemView) {
    fun bind(article: NewsDataArticle) {
        val title = itemView.findViewById<android.widget.TextView>(R.id.tvTitle)
        val summary = itemView.findViewById<android.widget.TextView>(R.id.tvSummary)
        title.text = article.title ?: "No Title"
        summary.text = article.description ?: "No Description"
        itemView.setOnClickListener {
            if (!article.link.isNullOrEmpty()) {
                onItemClick(article)
            }
        }
    }
}
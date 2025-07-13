package com.impulse.buzzboard

import android.os.Bundle
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
    // Use newsdata.io API key
    private var newsDataApiKey: String = BuildConfig.NEWS_DATA_API_KEY
    private var newsApiKey: String = BuildConfig.NEWS_API_KEY

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
                val newsDataResponse = newsDataService.getTopHeadlines(newsDataApiKey, category, "en", "us")
                val newsDataArticles = newsDataResponse.results ?: emptyList()
                if (newsDataResponse.status == "success" && newsDataArticles.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        headlinesAdapter.submitList(newsDataArticles)
                        Toast.makeText(this@MainActivity, "Loaded ${newsDataArticles.size} articles from newsdata.io", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Fallback to NewsAPI
                    val retrofitNewsApi = Retrofit.Builder()
                        .baseUrl("https://newsapi.org/v2/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val newsApiService = retrofitNewsApi.create(NewsApiService::class.java)
                    try {
                        val newsApiResponse = newsApiService.getTopHeadlines(category, newsApiKey)
                        val newsApiArticles = newsApiResponse.articles ?: emptyList()
                        withContext(Dispatchers.Main) {
                            if (newsApiResponse.status == "ok" && newsApiArticles.isNotEmpty()) {
                                // Convert NewsAPI articles to NewsDataArticle for display
                                headlinesAdapter.submitList(newsApiArticles.map {
                                    NewsDataArticle(
                                        title = it.title,
                                        description = it.description,
                                        image_url = it.urlToImage,
                                        link = it.url
                                    )
                                })
                                Toast.makeText(this@MainActivity, "Loaded ${newsApiArticles.size} articles from NewsAPI", Toast.LENGTH_LONG).show()
                            } else {
                                headlinesAdapter.submitList(emptyList())
                                Toast.makeText(this@MainActivity, "No articles found from either API.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Failed to fetch news from NewsAPI: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to NewsAPI if newsdata.io fails
                val retrofitNewsApi = Retrofit.Builder()
                    .baseUrl("https://newsapi.org/v2/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val newsApiService = retrofitNewsApi.create(NewsApiService::class.java)
                try {
                    val newsApiResponse = newsApiService.getTopHeadlines(category, newsApiKey)
                    val newsApiArticles = newsApiResponse.articles ?: emptyList()
                    withContext(Dispatchers.Main) {
                        if (newsApiResponse.status == "ok" && newsApiArticles.isNotEmpty()) {
                            headlinesAdapter.submitList(newsApiArticles.map {
                                NewsDataArticle(
                                    title = it.title,
                                    description = it.description,
                                    image_url = it.urlToImage,
                                    link = it.url
                                )
                            })
                            Toast.makeText(this@MainActivity, "Loaded ${newsApiArticles.size} articles from NewsAPI", Toast.LENGTH_LONG).show()
                        } else {
                            headlinesAdapter.submitList(emptyList())
                            Toast.makeText(this@MainActivity, "No articles found from either API.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to fetch news from NewsAPI: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

// Retrofit API service for newsdata.io
interface NewsDataApiService {
    @GET("news")
    suspend fun getTopHeadlines(
        @Query("apikey") apiKey: String,
        @Query("category") category: String,
        @Query("language") language: String = "en",
        @Query("country") country: String = "us"
    ): NewsDataApiResponse
}

// Data models for newsdata.io
// status: "success", results: List<NewsDataArticle>
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

// Retrofit API service for NewsAPI
interface NewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("category") category: String,
        @Query("apiKey") apiKey: String,
        @Query("country") country: String = "us"
    ): NewsApiResponse
}

// Data models for NewsAPI
// status: "ok", totalResults: Int, articles: List<NewsApiArticle>
data class NewsApiResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsApiArticle>?
)

data class NewsApiArticle(
    val title: String?,
    val description: String?,
    val urlToImage: String?,
    val url: String?
)

// HeadlinesAdapter (basic, you may need to implement ViewHolder)
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
        // You can use Glide/Picasso for image loading
        // Glide.with(itemView).load(article.image_url).into(itemView.findViewById(R.id.imgHeadline))
        itemView.setOnClickListener {
            if (!article.link.isNullOrEmpty()) {
                onItemClick(article)
            }
        }
    }
}
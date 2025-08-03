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
import androidx.core.view.GravityCompat
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
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var drawerToggle: androidx.appcompat.app.ActionBarDrawerToggle
    private var newsDataApiKey: String = BuildConfig.NEWS_DATA_API_KEY

    // Coroutine and pagination variables
    private val job = SupervisorJob()
    private var isLoading = false
    private var nextPage: String? = null
    private var currentCategory = "top"
    private var canLoadMore = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setTitleTextColor(
            androidx.core.content.ContextCompat.getColor(
                this,
                android.R.color.white
            )
        )
        toolbar.setBackgroundColor(
            androidx.core.content.ContextCompat.getColor(
                this,
                R.color.teal
            )
        )

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

        drawerToggle = androidx.appcompat.app.ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        fetchHeadlines(currentCategory, null)

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
            currentCategory = category
            nextPage = null
            canLoadMore = true
            fetchHeadlines(currentCategory, null)
            rvHeadlines.scrollToPosition(0)
            drawerLayout.closeDrawers()
            true
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Pagination: load more when scrolled to bottom
        rvHeadlines.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (canLoadMore && !isLoading && totalItemCount > 0 && lastVisibleItem >= totalItemCount - 1) {
                    fetchHeadlines(currentCategory, nextPage)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun fetchHeadlines(category: String, page: String?) {
        if (isLoading) return

        val retrofitNewsData = Retrofit.Builder()
            .baseUrl("https://newsdata.io/api/1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val newsDataService = retrofitNewsData.create(NewsDataApiService::class.java)
        
        isLoading = true
        CoroutineScope(Dispatchers.IO + job).launch {
            try {
                // Log.d("API_KEY", "Using API Key: $newsDataApiKey")
                val response = newsDataService.getTopHeadlines(
                    apiKey = newsDataApiKey,
                    category = category,
                    language = "en",
                    country = "in",
                    page = page
                )
                // Log.d("API_RESPONSE", "Status: ${response.status}, Articles: ${response.results?.size}, NextPage: ${response.nextPage}")
                val newsDataArticles = response.results ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (page == null) {
                        headlinesAdapter.submitList(newsDataArticles)
                    } else {
                        headlinesAdapter.appendList(newsDataArticles)
                    }
                    isLoading = false
                    nextPage = response.nextPage
                    canLoadMore = response.nextPage != null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Only show a simple error message to users
                    Toast.makeText(this@MainActivity, "Unable to load news", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    canLoadMore = false
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
        @Query("country") country: String = "in",
        @Query("page") page: String? = null
    ): NewsDataApiResponse
}

data class NewsDataApiResponse(
    val status: String,
    val results: List<NewsDataArticle>?,
    val nextPage: String?
)

data class NewsDataArticle(
    val title: String?,
    val description: String?,
    val image_url: String?,
    val link: String?
)

class HeadlinesAdapter(private val onItemClick: (NewsDataArticle) -> Unit) : RecyclerView.Adapter<HeadlinesViewHolder>() {
    private var articles: MutableList<NewsDataArticle> = mutableListOf()
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HeadlinesViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_headline, parent, false)
        return HeadlinesViewHolder(view, onItemClick)
    }
    override fun getItemCount(): Int = articles.size
    override fun onBindViewHolder(holder: HeadlinesViewHolder, position: Int) {
        holder.bind(articles[position])
    }
    fun submitList(list: List<NewsDataArticle>?) {
        val oldSize = articles.size
        articles = (list ?: emptyList()).toMutableList()
        notifyItemRangeRemoved(0, oldSize)
        notifyItemRangeInserted(0, articles.size)
    }
    fun appendList(list: List<NewsDataArticle>?) {
        if (list != null && list.isNotEmpty()) {
            val start = articles.size
            articles.addAll(list)
            notifyItemRangeInserted(start, list.size)
        }
    }
}

class HeadlinesViewHolder(itemView: android.view.View, private val onItemClick: (NewsDataArticle) -> Unit) : RecyclerView.ViewHolder(itemView) {
    fun bind(article: NewsDataArticle) {
        val title = itemView.findViewById<android.widget.TextView>(R.id.tvTitle)
        val summary = itemView.findViewById<android.widget.TextView>(R.id.tvSummary)
        val image = itemView.findViewById<android.widget.ImageView>(R.id.imgHeadline)
        title.text = article.title ?: "No Title"
        summary.text = article.description ?: "No Description"

        // Load image using Glide
        if (!article.image_url.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(itemView.context)
                .load(article.image_url)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.color.darker_gray)
                .into(image)
        } else {
            image.setImageResource(android.R.color.darker_gray)
        }
        itemView.setOnClickListener {
            if (!article.link.isNullOrEmpty()) {
                onItemClick(article)
            }
        }
    }
}
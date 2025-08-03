package com.impulse.buzzboard

import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class ArticleWebViewActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    private fun applySafeInsets(view: android.view.View) {
        view.setOnApplyWindowInsetsListener { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private lateinit var webView: WebView

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Vertical layout with a Toolbar and WebView
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        applySafeInsets(layout)

        // Toolbar
        val toolbar = androidx.appcompat.widget.Toolbar(this)
        toolbar.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.teal))
        toolbar.setTitleTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
        toolbar.title = "Article"
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
        val toolbarParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        toolbar.layoutParams = toolbarParams

        // WebView
        webView = WebView(this)
        webView.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            1f
        )
        webView.webViewClient = WebViewClient()
        val url = intent.getStringExtra("url")
        if (url != null) {
            webView.loadUrl(url)
        }

        layout.addView(toolbar)
        layout.addView(webView)
        setContentView(layout)
    }
}


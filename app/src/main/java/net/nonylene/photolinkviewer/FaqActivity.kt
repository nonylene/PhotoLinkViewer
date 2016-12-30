package net.nonylene.photolinkviewer

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView

class FaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        val webView = findViewById(R.id.web_view) as WebView
        webView.loadUrl("file:///android_asset/" + getString(R.string.asset_faq_filename))
    }
}

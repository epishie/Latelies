package com.epishie.news.features.story

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import com.epishie.news.R
import com.epishie.news.component
import com.epishie.news.features.common.BaseActivity
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import com.squareup.picasso.Picasso
import io.reactivex.Flowable
import io.reactivex.Scheduler
import kotlinx.android.synthetic.main.story_activity.*
import javax.inject.Inject
import javax.inject.Named

class StoryActivity : BaseActivity() {
    companion object {
        fun storyIntent(context: Context, url: String): Intent {
            return Intent(context, StoryActivity::class.java)
                    .setData(Uri.parse(url))
        }
    }

    @field:[Inject Named("ui")]
    lateinit var ui: Scheduler
    private lateinit var vm: StoryViewModel
    private lateinit var url: Uri
    private var defaultImage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        url = intent.data ?: throw IllegalStateException("Missing data in the intent")

        component.inject(this)
        vm = ViewModelProviders.of(this, component.vmFactory())[StoryViewModel::class.java]

        setContentView(R.layout.story_activity)
        setupView()

        val array = resources.obtainTypedArray(R.array.default_images)
        val index = Math.abs(intent.data.toString().hashCode()).rem(array.length())
        defaultImage = array.getResourceId(index, 0)
        array.recycle()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.story, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.openBrowser -> {
                openLink(url)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupView() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupWebView()

        val states = vm.update(url.toString(), Flowable.just(StoryViewModel.Event.Refresh))
                .observeOn(ui)
                .publish()
        states.map { (progress) -> progress }
                .subscribe(progressBar.visibility())
        val stories = states.filter { state -> state.story != null }
                .map { state -> state.story!! }
                .publish()
        stories.map { story -> story.title }
                .subscribe(storyTitle.text())
        stories.filter { story -> story.author != null }
                .map { story -> getString(R.string.lbl_author, story.author!!) }
                .subscribe(storyAuthor.text())
        stories.filter { story -> story.sourceLogo != null }
                .map { story -> story.sourceLogo!! }
                .subscribe { logo ->
                    Picasso.with(this)
                            .load(logo)
                            .fit()
                            .centerCrop()
                            .into(sourceLogo)
                }
        stories.filter { story -> story.date != null }
                .map { story ->
                    val flags = DateUtils.FORMAT_SHOW_DATE
                    val date = DateUtils.formatDateTime(this, story.date!!, flags)
                    val readTime = if (story.timeToRead != null) {
                        getString(R.string.lbl_time_to_read,
                                Math.ceil(story.timeToRead.toDouble()).toInt())
                    } else {
                        null
                    }
                    TextUtils.join(getString(R.string.lbl_bullet),
                            listOf(date, readTime).filter { !it.isNullOrEmpty() })
                }
                .subscribe(storyTime.text())
        stories.filter { story -> story.content != null }
                .map { story -> story.content!! }
                .subscribe { content ->
                    val html = getString(R.string.html, content)
                    webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
                }
        disposables.add(states.connect())
        disposables.add(stories.connect())
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.setAppCacheEnabled(true)
        settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = WebChromeClient()
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = object : WebViewClient() {
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    openLink(Uri.parse(url))
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest)
                    : Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    openLink(request.url)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    private fun openLink(url: Uri) {
        if (URLUtil.isAssetUrl(url.toString())) {
            return
        }
        val intent = CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .setStartAnimations(this, R.anim.slide_in_right,
                        android.R.anim.slide_out_right)
                .build()
        intent.launchUrl(this, url)
    }
}
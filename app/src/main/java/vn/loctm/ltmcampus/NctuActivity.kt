package vn.loctm.ltmcampus

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.*

/** Official portal only: no Javascript bridge, credential scraping or private backend. */
class NctuActivity:Activity() {
 private lateinit var web:WebView
 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val bar=LinearLayout(this)
  bar.addView(Button(this).apply{setText(R.string.portal_close);setOnClickListener{finish()}})
  bar.addView(Button(this).apply{setText(R.string.portal_browser);setOnClickListener{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://sv.nctu.edu.vn/dashboard")))}})
  bar.addView(Button(this).apply{setText(R.string.portal_clear);setOnClickListener{CookieManager.getInstance().removeAllCookies{web.post{web.clearCache(true);WebStorage.getInstance().deleteAllData();web.loadUrl("https://sv.nctu.edu.vn/dashboard")}}}})
  layout.addView(bar)
  layout.addView(TextView(this).apply{setText(R.string.portal_privacy);setPadding(16,8,16,8)})
  web=WebView(this)
  web.settings.apply { javaScriptEnabled=true;domStorageEnabled=true;allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;setSupportMultipleWindows(false) }
  CookieManager.getInstance().setAcceptThirdPartyCookies(web,false)
  web.webViewClient=object:WebViewClient() {
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
    val uri=request.url
    if(PortalPolicy.isOfficial(uri.toString())) return false
    if(request.isForMainFrame && request.hasGesture() && uri.scheme=="https") {
     try{startActivity(Intent(Intent.ACTION_VIEW,uri))}catch(_:Exception){Toast.makeText(this@NctuActivity,"Không mở được trình duyệt",Toast.LENGTH_SHORT).show()}
    }
    return true
   }
   override fun onReceivedSslError(view:WebView,handler:android.webkit.SslErrorHandler,error:android.net.http.SslError){handler.cancel()}
  }
  layout.addView(web,LinearLayout.LayoutParams(-1,0,1f));setContentView(layout)
  web.loadUrl("https://sv.nctu.edu.vn/dashboard")
 }
 override fun onDestroy(){web.stopLoading();web.destroy();super.onDestroy()}
}

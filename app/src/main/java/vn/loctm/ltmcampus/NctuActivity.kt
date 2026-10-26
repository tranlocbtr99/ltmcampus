package vn.loctm.ltmcampus

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.webkit.*
import android.widget.*

class NctuActivity:Activity() {
 private lateinit var web:WebView
 private lateinit var controller:PortalSyncController
 private lateinit var status:TextView
 private val handler=Handler(Looper.getMainLooper())
 private var pollDelay=15_000L
 private val poll=object:Runnable {override fun run(){controller.request();handler.postDelayed(this,pollDelay)}}
 @SuppressLint("SetJavaScriptEnabled")
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
  val bar=LinearLayout(this)
  bar.addView(Button(this).apply{setText(R.string.portal_close);setOnClickListener{finish()}})
  bar.addView(Button(this).apply{setText(R.string.portal_browser);setOnClickListener{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://sv.nctu.edu.vn/dashboard")))}})
  bar.addView(Button(this).apply{setText(R.string.portal_clear);setOnClickListener{
   SyncScheduler.disconnect(this@NctuActivity)
   CookieManager.getInstance().removeAllCookies{web.post{web.clearCache(true);WebStorage.getInstance().deleteAllData();web.loadUrl("https://sv.nctu.edu.vn/dashboard")}}
  }})
  layout.addView(bar)
  status=TextView(this).apply{setText(R.string.portal_sync_login);setPadding(16,8,16,8)}
  layout.addView(status)
  web=WebView(this)
  web.settings.apply {javaScriptEnabled=true;domStorageEnabled=true;allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;setSupportMultipleWindows(false)}
  CookieManager.getInstance().setAcceptThirdPartyCookies(web,false)
  controller=PortalSyncController(this,web){result->pollDelay=if(result=="ok")60_000L else 15_000L;status.text=SyncScheduler.message(result)}
  web.webViewClient=object:WebViewClient() {
   override fun onPageStarted(v:WebView,url:String,icon:android.graphics.Bitmap?){controller.navigationStarted()}
   override fun onPageFinished(view:WebView,url:String){view.postDelayed({if(!isFinishing&&!isDestroyed)controller.request()},1500)}
   override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
    val uri=request.url
    if(PortalPolicy.isOfficial(uri.toString()))return false
    if(request.isForMainFrame&&request.hasGesture()&&uri.scheme=="https") {
     try{startActivity(Intent(Intent.ACTION_VIEW,uri))}catch(_:Exception){Toast.makeText(this@NctuActivity,R.string.portal_no_browser,Toast.LENGTH_SHORT).show()}
    }
    return true
   }
   override fun onReceivedSslError(view:WebView,handler:SslErrorHandler,error:android.net.http.SslError){handler.cancel();status.text=getString(R.string.portal_ssl_error)}
  }
  layout.addView(web,LinearLayout.LayoutParams(-1,0,1f));setContentView(layout)
  web.loadUrl("https://sv.nctu.edu.vn/dashboard")
 }
 override fun onResume(){super.onResume();handler.postDelayed(poll,3000)}
 override fun onPause(){handler.removeCallbacks(poll);super.onPause()}
 override fun onDestroy(){handler.removeCallbacksAndMessages(null);controller.close();web.stopLoading();web.destroy();super.onDestroy()}
}

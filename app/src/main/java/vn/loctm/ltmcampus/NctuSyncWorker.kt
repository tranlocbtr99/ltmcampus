package vn.loctm.ltmcampus

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class NctuSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 @SuppressLint("SetJavaScriptEnabled")
 override suspend fun doWork():Result=workerLock.withLock {
  if(!SyncScheduler.prefs(applicationContext).getBoolean("enabled",false))return@withLock Result.success()
  val attemptTime=System.currentTimeMillis()
  fun failed(){val p=SyncScheduler.prefs(applicationContext);if(p.getBoolean("enabled",false)&&p.getLong("lastSuccess",0)<=attemptTime)p.edit().putString("status","NETWORK").apply()}
  SyncScheduler.prefs(applicationContext).edit().putString("status","syncing").apply()
  var web:WebView?=null
  var controller:PortalSyncController?=null
  try {
   val status=withTimeout(90_000) {
    withContext(Dispatchers.Main) {
     suspendCancellableCoroutine<String> { continuation ->
      val view=WebView(applicationContext);web=view
      view.settings.apply{javaScriptEnabled=true;domStorageEnabled=true;allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW}
      CookieManager.getInstance().setAcceptThirdPartyCookies(view,false)
      controller=PortalSyncController(applicationContext,view){result->if(continuation.isActive)continuation.resume(result)}
      view.webViewClient=object:WebViewClient() {
       override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest)=!PortalPolicy.isOfficial(r.url.toString())
       override fun onPageStarted(v:WebView,url:String,icon:android.graphics.Bitmap?){controller?.navigationStarted()}
       override fun onPageFinished(v:WebView,url:String){v.postDelayed({if(continuation.isActive)controller?.request()},2000)}
       override fun onReceivedSslError(v:WebView,h:SslErrorHandler,e:android.net.http.SslError){h.cancel();if(continuation.isActive)continuation.resume("NETWORK")}
       override fun onReceivedError(v:WebView,r:WebResourceRequest,e:WebResourceError){if(r.isForMainFrame&&continuation.isActive)continuation.resume("NETWORK")}
      }
      view.loadUrl(PortalSyncController.ORIGIN+"/dashboard")
     }
    }
   }
   when(status){"ok","auth","unsupported","PORTAL_CHANGED","TRUNCATED","FORMAT"->Result.success();else->{failed();Result.retry()}}
  } catch(e:TimeoutCancellationException) {
   failed();Result.retry()
  } catch(e:CancellationException){throw e
  } catch(_:Exception){failed();Result.retry()
  } finally {
   withContext(NonCancellable+Dispatchers.Main){controller?.close();web?.stopLoading();web?.destroy()}
  }
 }
 companion object{private val workerLock=Mutex()}
}

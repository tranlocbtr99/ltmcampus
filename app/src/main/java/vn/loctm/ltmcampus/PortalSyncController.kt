package vn.loctm.ltmcampus

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** The origin-scoped bridge accepts only schedule snapshots, never credentials or commands. */
class PortalSyncController(private val context:Context,private val web:WebView,private val onResult:(String)->Unit) {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private var nonce:String?=null
 private var startedAt=0L
 private var generation=0L
 private var first=""
 private var last=""
 private var closed=false
 private var timeoutJob:Job?=null
 private val supported=WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
 private val script=context.assets.open("nctu-sync.js").bufferedReader().use{it.readText()}
 init {
  if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))WebViewCompat.addWebMessageListener(web,"CampusScheduleSink",setOf(ORIGIN)) { _, message, origin, main, _ ->
   if(main&&trusted(origin)&&!closed&&message.type==WebMessageCompat.TYPE_STRING)receive(message.data)
  }
 }
 fun navigationStarted(){nonce=null;timeoutJob?.cancel();timeoutJob=null}
 fun request() {
  if(closed||nonce!=null)return
  if(!supported){record("unsupported",System.currentTimeMillis());onResult("unsupported");return}
  if(web.url?.let{trusted(Uri.parse(it))}!=true)return
  val now=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
  first=now.withDayOfMonth(1).minusMonths(1).toString()
  last=now.withDayOfMonth(1).plusMonths(7).minusDays(1).toString()
  val id=UUID.randomUUID().toString();nonce=id;startedAt=System.currentTimeMillis()
  generation=SyncScheduler.prefs(context).getLong("generation",0)
  val request=JSONObject().put("nonce",id).put("start",first).put("end",last)
  web.evaluateJavascript(script+"\nwindow.__campusFetchSchedule("+request.toString()+");",null)
  timeoutJob?.cancel()
  timeoutJob=scope.launch { delay(65_000);if(nonce==id){nonce=null;record("NETWORK");onResult("NETWORK")} }
 }
 private fun receive(raw:String?) {
  val expected=nonce?:return
  if(raw==null||raw.length>8*1024*1024)return
  val root=runCatching{JSONObject(raw)}.getOrNull()?:return
  if(root.optString("nonce")!=expected)return
  val status=root.optString("status")
  if(status!in listOf("ok","auth","error"))return
  nonce=null;timeoutJob?.cancel();timeoutJob=null
  if(generation!=SyncScheduler.prefs(context).getLong("generation",0))return
  if(status!="ok") {
   val code=if(status=="auth")"auth"else root.optString("code").takeIf{it in listOf("PORTAL_CHANGED","TRUNCATED","FORMAT","NETWORK")}?:"FORMAT"
   record(code);onResult(code);return
  }
  val requestTime=startedAt;val requestGeneration=generation;val requestedStart=first;val requestedEnd=last
  scope.launch {
   try {
   val count=withContext(Dispatchers.IO) {
    val snapshot=SyncSnapshot.parse(raw,expected,requestedStart,requestedEnd)
    applyLock.withLock {
     if(requestGeneration!=SyncScheduler.prefs(context).getLong("generation",0))return@withLock -1
     CampusDb(context).use {it.applySnapshot(snapshot,requestTime)}
    }
   }
   if(closed||requestGeneration!=SyncScheduler.prefs(context).getLong("generation",0))return@launch
   if(count>=0) {
    SyncScheduler.prefs(context).edit().putBoolean("enabled",true).putString("status","ok").putLong("lastSuccess",System.currentTimeMillis()).putInt("lastCount",count).apply()
    SyncScheduler.schedule(context)
   }
   android.webkit.CookieManager.getInstance().flush()
   onResult("ok")
   } catch(e:CancellationException){throw e
   } catch(_:Exception){if(!closed){record("FORMAT",requestTime);onResult("FORMAT")}}
  }
 }
 private fun record(status:String,requestTime:Long=startedAt) {
  val prefs=SyncScheduler.prefs(context)
  // Ignore old failures after a newer successful response.
  if(prefs.getLong("lastSuccess",0)<=requestTime)prefs.edit().putString("status",status).apply()
 }
 fun close() {closed=true;nonce=null;scope.cancel();if(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER))WebViewCompat.removeWebMessageListener(web,"CampusScheduleSink")}
 companion object {
  const val ORIGIN="https://sv.nctu.edu.vn"
  private val applyLock=Mutex()
  fun trusted(uri:Uri)=uri.scheme=="https"&&uri.host=="sv.nctu.edu.vn"&&uri.userInfo==null&&(uri.port==-1||uri.port==443)
 }
}

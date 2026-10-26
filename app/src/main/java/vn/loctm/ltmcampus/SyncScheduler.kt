package vn.loctm.ltmcampus

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {
 const val PERIODIC="nctu-periodic-sync"
 const val IMMEDIATE="nctu-immediate-sync"
 fun prefs(context:Context)=context.getSharedPreferences("nctu_sync",Context.MODE_PRIVATE)
 fun schedule(context:Context) {
  if(!prefs(context).getBoolean("enabled",false))return
  val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC,ExistingPeriodicWorkPolicy.KEEP,
   PeriodicWorkRequestBuilder<NctuSyncWorker>(30,TimeUnit.MINUTES).setInitialDelay(30,TimeUnit.MINUTES).setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
 }
 fun refresh(context:Context,force:Boolean=false) {
  val p=prefs(context)
  if(!p.getBoolean("enabled",false))return
  schedule(context)
  if(!force&&System.currentTimeMillis()-p.getLong("lastSuccess",0)<5*60_000)return
  WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE,ExistingWorkPolicy.KEEP,
   OneTimeWorkRequestBuilder<NctuSyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
 }
 fun disconnect(context:Context) {
  prefs(context).edit().putBoolean("enabled",false).putString("status","disconnected").putLong("generation",System.currentTimeMillis()).apply()
  WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
  WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE)
 }
 fun message(status:String)=when(status) {
  "syncing"->"Đang cập nhật lịch từ NCTU…"
  "ok"->"Lịch đã được cập nhật tự động"
  "auth"->"Phiên NCTU đã hết hạn. Đăng nhập lại để tiếp tục tự cập nhật."
  "unsupported"->"Cần cập nhật Android System WebView để kết nối NCTU."
  "PORTAL_CHANGED"->"Trang NCTU đã đổi bộ kết nối. Dữ liệu cũ được giữ lại."
  "TRUNCATED"->"NCTU trả lịch chưa đầy đủ. Dữ liệu cũ được giữ lại."
  "FORMAT"->"Dữ liệu NCTU chưa đúng định dạng. Dữ liệu cũ được giữ lại."
  "disconnected"->"Đã tắt tự cập nhật; lịch đã lưu vẫn xem ngoại tuyến được."
  else->"Chưa cập nhật được lịch. Sẽ thử lại khi có mạng."
 }
}

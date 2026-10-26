package vn.loctm.ltmcampus

import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.security.MessageDigest

data class SyncSnapshot(val accountHash:String,val start:String,val end:String,val lessons:List<Lesson>) {
 companion object {
  fun parse(raw:String,nonce:String,start:String,end:String):SyncSnapshot {
   require(raw.length<=8*1024*1024) { "Phản hồi vượt giới hạn" }
   val root=JSONObject(raw)
   require(root.getString("nonce")==nonce&&root.getString("status")=="ok") { "Phản hồi không hợp lệ" }
   require(root.getString("start")==start&&root.getString("end")==end) { "Sai khoảng đồng bộ" }
   val first=LocalDate.parse(start);val last=LocalDate.parse(end)
   require(ChronoUnit.DAYS.between(first,last) in 0..366)
   val identity=root.getString("identity").trim();require(identity.isNotEmpty()&&identity.length<=256)
   val hash=MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString(""){"%02x".format(it)}
   val data=root.getJSONObject("data")
   require(data.getJSONArray("sessions").length()<1000) { "Lịch có thể chưa đầy đủ" }
   val lessons=ScheduleParser.parse(JSONObject().put("success",true).put("data",data).toString(),allowEmpty=true)
    .filter{it.date>=start&&it.date<=end}
   return SyncSnapshot(hash,start,end,lessons)
  }
 }
}

package vn.loctm.ltmcampus

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object ScheduleParser {
 fun parse(raw:String):List<Lesson> {
  val root=JSONObject(raw)
  require(root.optBoolean("success",false)) { "Phản hồi NCTU không thành công" }
  val data=root.getJSONObject("data")
  val sessions=data.getJSONArray("sessions")
  require(sessions.length()<=10000) { "Tệp chứa quá nhiều buổi học" }
  val result=linkedMapOf<String,Lesson>()
  for(i in 0 until sessions.length()) {
   val x=sessions.getJSONObject(i)
   fun str(k:String)=if(x.isNull(k)) "" else x.optString(k).trim()
   val first=LocalDate.parse(str("explicitDate").ifBlank{str("startDate")})
   val last=if(str("explicitDate").isNotBlank()) first else LocalDate.parse(str("endDate").ifBlank{first.toString()})
   require(!last.isBefore(first)&&ChronoUnit.DAYS.between(first,last)<=730) { "Khoảng ngày không hợp lệ ở buổi ${i+1}" }
   val start=str("startTime").ifBlank{data.optJSONObject("periodTimes")?.optJSONObject(x.optInt("startPeriod").toString())?.optString("start").orEmpty()}
   val end=str("endTime").ifBlank{data.optJSONObject("periodTimes")?.optJSONObject(x.optInt("endPeriod").toString())?.optString("end").orEmpty()}
   require(LocalTime.parse(end)>LocalTime.parse(start)) { "Giờ học không hợp lệ ở buổi ${i+1}" }
   val subject=str("subjectNameVi").ifBlank{str("className")}
   require(subject.isNotBlank()) { "Thiếu tên môn học ở buổi ${i+1}" }
   val base=str("id").ifBlank{"${str("classCode")}_${start}"}
   val weekday=x.optInt("weekday",-1)
   require(first==last||weekday in 2..8) { "Thiếu thứ cho lịch lặp" }
   var date=first
   while(!date.isAfter(last)) {
    if(first==last||date.dayOfWeek.value+1==weekday) {
     val id="${base}@${date}"
     result[id]=Lesson(id,date.toString(),start,end,subject,str("roomName"),str("teacherFullName"),str("classCode"),x.optBoolean("isTamNgung",false))
    }
    date=date.plusDays(1)
   }
  }
  require(result.isNotEmpty()) { "Không tìm thấy buổi học hợp lệ; dữ liệu cũ được giữ nguyên" }
  return result.values.toList()
 }
}

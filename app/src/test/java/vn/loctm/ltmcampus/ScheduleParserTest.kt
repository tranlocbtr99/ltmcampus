package vn.loctm.ltmcampus
import org.junit.Assert.*
import org.junit.Test

class ScheduleParserTest {
 private fun payload(session:String)="""{"success":true,"data":{"sessions":[$session]}}"""
 private val lesson=""""id":"a","explicitDate":"2026-11-11","startTime":"09:45","endTime":"12:00","subjectNameVi":"Quản trị tài chính","classCode":"FIN4036-06""""
 @Test fun parsesOfficialFields() { val row=ScheduleParser.parse(payload("{$lesson}" )).single();assertEquals("2026-11-11",row.date);assertEquals("Quản trị tài chính",row.subject);assertFalse(row.suspended) }
 @Test fun keepsSameIdOnDifferentDates() {val second=lesson.replace("2026-11-11","2026-11-18");assertEquals(2,ScheduleParser.parse(payload("{$lesson},{$second}")).size)}
 @Test fun expandsWeekdayWithinRange() {val s="""{"id":"b","startDate":"2026-11-02","endDate":"2026-11-15","weekday":4,"startTime":"07:00","endTime":"09:15","subjectNameVi":"Toán"}""";assertEquals(listOf("2026-11-04","2026-11-11"),ScheduleParser.parse(payload(s)).map{it.date})}
 @Test(expected=IllegalArgumentException::class) fun rejectsFailedResponse() {ScheduleParser.parse("""{"success":false,"data":{"sessions":[]}}""")}
 @Test(expected=Exception::class) fun rejectsInvalidDate() {ScheduleParser.parse(payload("{${lesson.replace("2026-11-11","2026-02-30")}}"))}
 @Test(expected=IllegalArgumentException::class) fun rejectsEmptyImport() {ScheduleParser.parse(payload(""))}
 @Test(expected=IllegalArgumentException::class) fun rejectsReversedTimes() {ScheduleParser.parse(payload("{${lesson.replace("12:00","08:00")}}"))}
 @Test fun parsesPrivateFixtureWhenProvided() {
  val path=System.getenv("CAMPUS_TEST_SCHEDULE")?:return
  val raw=java.io.File(path).readText()
  val source=org.json.JSONObject(raw).getJSONObject("data").getJSONArray("sessions")
  val parsed=ScheduleParser.parse(raw)
  assertEquals(source.length(),parsed.size)
  assertEquals(parsed.size,parsed.map{it.id}.distinct().size)
 }
}

package vn.loctm.ltmcampus
import org.junit.Assert.*
import org.junit.Test
class SyncSnapshotTest {
 private fun message(sessions:String="[]",nonce:String="n",identity:String="student")="""{"nonce":"$nonce","status":"ok","identity":"$identity","start":"2026-11-01","end":"2026-11-30","data":{"sessions":$sessions}}"""
 @Test fun acceptsAuthoritativeEmptySchedule(){assertTrue(SyncSnapshot.parse(message(),"n","2026-11-01","2026-11-30").lessons.isEmpty())}
 @Test(expected=IllegalArgumentException::class)fun rejectsStaleNonce(){SyncSnapshot.parse(message(nonce="old"),"n","2026-11-01","2026-11-30")}
 @Test(expected=IllegalArgumentException::class)fun rejectsWrongRange(){SyncSnapshot.parse(message(),"n","2026-10-01","2026-11-30")}
 @Test(expected=IllegalArgumentException::class)fun rejectsMissingIdentity(){SyncSnapshot.parse(message(identity=""),"n","2026-11-01","2026-11-30")}
 @Test fun hashesIdentityAndFiltersOutOfWindow(){
  val raw=message("""[{"id":"x","explicitDate":"2026-12-01","startTime":"07:00","endTime":"09:15","subjectNameVi":"Toán"}]""")
  val x=SyncSnapshot.parse(raw,"n","2026-11-01","2026-11-30");assertTrue(x.lessons.isEmpty());assertEquals(64,x.accountHash.length);assertNotEquals("student",x.accountHash)
 }
}

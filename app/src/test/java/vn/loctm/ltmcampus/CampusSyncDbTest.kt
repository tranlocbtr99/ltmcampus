package vn.loctm.ltmcampus
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE)
class CampusSyncDbTest {
 private lateinit var context:Context
 private lateinit var db:CampusDb
 @Before fun setup(){context=RuntimeEnvironment.getApplication();context.deleteDatabase("ltm_campus.db");db=CampusDb(context)}
 @After fun close(){db.close();context.deleteDatabase("ltm_campus.db")}
 private fun lesson(id:String,room:String="A1",subject:String="Toán",date:String="2026-11-11")=Lesson(id,date,"07:00","09:15",subject,room,"GV","M01",false)
 private fun snap(vararg rows:Lesson,account:String="account")=SyncSnapshot(account,"2026-11-01","2026-11-30",rows.toList())
 @Test fun updatesAndRemovesRemoteRowsPreservingNotesAndManualSchedule(){
  db.importSchedule("""{"success":true,"data":{"sessions":[{"id":"manual","explicitDate":"2026-11-12","startTime":"07:00","endTime":"09:15","subjectNameVi":"Tự học"}]}}""")
  db.addNote("Toán","Nội dung quan trọng");db.addExam("Toán","Giữa kỳ",1800000000000)
  db.applySnapshot(snap(lesson("remote")),1)
  db.applySnapshot(snap(lesson("remote",room="B2")),2)
  assertEquals("B2",db.lessons().first{it.id=="remote"}.room)
  db.applySnapshot(snap(),3)
  assertEquals(1,db.lessons().size);assertEquals("Tự học",db.lessons().single().subject)
  assertEquals(1,db.notes().size);assertEquals(1,db.exams().size)
 }
 @Test fun staleResponseCannotOverwriteNewerSchedule(){db.applySnapshot(snap(lesson("r",room="New")),20);assertEquals(-1,db.applySnapshot(snap(lesson("r",room="Old")),10));assertEquals("New",db.lessons().single().room)}
 @Test fun preservesRemoteRowsOutsideRequestedWindow(){db.applySnapshot(SyncSnapshot("account","2026-12-01","2026-12-31",listOf(lesson("future",date="2026-12-01"))),1);db.applySnapshot(snap(),2);assertEquals("future",db.lessons().single().id)}
 @Test fun switchedAccountClearsOldRemoteCache(){db.applySnapshot(snap(lesson("a")),1);db.applySnapshot(snap(lesson("b"),account="another"),2);assertEquals("b",db.lessons().single().id)}
 @Test fun failedTransactionRestoresPreviousSchedule(){
  db.applySnapshot(snap(lesson("old")),1)
  db.writableDatabase.execSQL("CREATE TRIGGER reject_lesson BEFORE INSERT ON lessons WHEN NEW.subject='FAIL' BEGIN SELECT RAISE(ABORT,'test failure'); END")
  try{db.applySnapshot(snap(lesson("new"),lesson("bad",subject="FAIL")),2);fail("Expected failed transaction")}catch(_:android.database.SQLException){}
  assertEquals("old",db.lessons().single().id)
 }
 @Test fun backupPreservesRemoteOwnershipWithoutLoginCredentials(){
  db.applySnapshot(snap(lesson("remote")),1);db.addNote("Toán","Nội dung")
  val raw=db.exportBackup();assertFalse(raw.contains("access_token"));assertFalse(raw.contains("password"))
  db.restoreBackup(raw);db.applySnapshot(snap(),2)
  assertTrue(db.lessons().isEmpty());assertEquals(1,db.notes().size)
 }
 @Test fun upgradePreservesVersionOneData(){
  db.close();context.deleteDatabase("ltm_campus.db")
  val old=object:SQLiteOpenHelper(context,"ltm_campus.db",null,1){
   override fun onCreate(d:SQLiteDatabase){
    d.execSQL("CREATE TABLE lessons(id TEXT PRIMARY KEY,day TEXT NOT NULL,starts TEXT,ends TEXT,subject TEXT,room TEXT,teacher TEXT,code TEXT,suspended INTEGER)")
    d.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,subject TEXT NOT NULL,content TEXT NOT NULL)")
    d.execSQL("CREATE TABLE exams(id INTEGER PRIMARY KEY AUTOINCREMENT,subject TEXT NOT NULL,title TEXT NOT NULL,when_ms INTEGER NOT NULL)")
   }
   override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int){}
  }
  old.writableDatabase.execSQL("INSERT INTO notes(subject,content) VALUES('Môn cũ','Giữ nguyên')");old.close()
  db=CampusDb(context);assertEquals("Giữ nguyên",db.notes().single().content)
  db.applySnapshot(snap(lesson("new")),1);assertEquals(1,db.lessons().size)
 }
}

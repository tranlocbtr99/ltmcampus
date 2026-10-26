package vn.loctm.ltmcampus

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

data class Lesson(val id:String,val date:String,val start:String,val end:String,val subject:String,val room:String,val teacher:String,val code:String,val suspended:Boolean)
data class Note(val id:Long,val subject:String,val content:String)
data class Exam(val id:Long,val subject:String,val title:String,val whenMillis:Long)

class CampusDb(context:Context):SQLiteOpenHelper(context,"ltm_campus.db",null,1) {
 override fun onCreate(db:SQLiteDatabase) {
  db.execSQL("CREATE TABLE lessons(id TEXT PRIMARY KEY,day TEXT NOT NULL,starts TEXT,ends TEXT,subject TEXT,room TEXT,teacher TEXT,code TEXT,suspended INTEGER)")
  db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,subject TEXT NOT NULL,content TEXT NOT NULL)")
  db.execSQL("CREATE TABLE exams(id INTEGER PRIMARY KEY AUTOINCREMENT,subject TEXT NOT NULL,title TEXT NOT NULL,when_ms INTEGER NOT NULL)")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) { /* additive migrations only; never drop user tables */ }
 fun importSchedule(raw:String):Int {
  val lessons=ScheduleParser.parse(raw)
  val db=writableDatabase
  db.beginTransaction()
  try {
   for(x in lessons) {
    val values=android.content.ContentValues().apply {
     put("id",x.id);put("day",x.date);put("starts",x.start);put("ends",x.end);put("subject",x.subject)
     put("room",x.room);put("teacher",x.teacher);put("code",x.code);put("suspended",if(x.suspended) 1 else 0)
    }
    db.replaceOrThrow("lessons",null,values)
   }
   db.setTransactionSuccessful()
  } finally {db.endTransaction()}
  return lessons.size
 }
 fun updateNote(id:Long,subject:String,content:String) {
  writableDatabase.execSQL("UPDATE notes SET subject=?,content=? WHERE id=?",arrayOf(subject,content,id))
 }
 fun exportBackup():String = JSONObject().apply {
  put("format","ltm-campus-backup");put("version",1)
  put("lessons",org.json.JSONArray().apply { lessons().forEach { x->put(JSONObject().apply {
   put("id",x.id);put("date",x.date);put("start",x.start);put("end",x.end);put("subject",x.subject);put("room",x.room);put("teacher",x.teacher);put("code",x.code);put("suspended",x.suspended)
  }) } })
  put("notes",org.json.JSONArray().apply { notes().forEach { x->put(JSONObject().put("id",x.id).put("subject",x.subject).put("content",x.content)) } })
  put("exams",org.json.JSONArray().apply { exams().forEach { x->put(JSONObject().put("id",x.id).put("subject",x.subject).put("title",x.title).put("whenMillis",x.whenMillis)) } })
 }.toString(2)
 fun restoreBackup(raw:String) {
  val root=JSONObject(raw)
  require(root.getString("format")=="ltm-campus-backup"&&root.getInt("version")==1) { "Không phải bản sao lưu LTM Campus được hỗ trợ" }
  val db=writableDatabase;db.beginTransaction()
  try {
   for(table in listOf("lessons","notes","exams")) db.delete(table,null,null)
   val ls=root.getJSONArray("lessons")
   for(i in 0 until ls.length()) {val x=ls.getJSONObject(i)
    java.time.LocalDate.parse(x.getString("date"));java.time.LocalTime.parse(x.getString("start"));java.time.LocalTime.parse(x.getString("end"))
    db.execSQL("INSERT INTO lessons VALUES(?,?,?,?,?,?,?,?,?)",arrayOf(x.getString("id"),x.getString("date"),x.getString("start"),x.getString("end"),x.getString("subject"),x.getString("room"),x.getString("teacher"),x.getString("code"),if(x.getBoolean("suspended")) 1 else 0))
   }
   val ns=root.getJSONArray("notes");for(i in 0 until ns.length()) { val x=ns.getJSONObject(i);db.execSQL("INSERT INTO notes VALUES(?,?,?)",arrayOf(x.getLong("id"),x.getString("subject"),x.getString("content"))) }
   val es=root.getJSONArray("exams");for(i in 0 until es.length()) { val x=es.getJSONObject(i);db.execSQL("INSERT INTO exams VALUES(?,?,?,?)",arrayOf(x.getLong("id"),x.getString("subject"),x.getString("title"),x.getLong("whenMillis"))) }
   db.setTransactionSuccessful()
  } finally {db.endTransaction()}
 }
 fun lessons():List<Lesson> = readableDatabase.rawQuery("SELECT id,day,starts,ends,subject,room,teacher,code,suspended FROM lessons ORDER BY day,starts",null).use { c -> buildList {while(c.moveToNext()) add(Lesson(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getInt(8)!=0))} }
 fun notes():List<Note> = readableDatabase.rawQuery("SELECT id,subject,content FROM notes ORDER BY id DESC",null).use { c -> buildList {while(c.moveToNext()) add(Note(c.getLong(0),c.getString(1),c.getString(2)))} }
 fun addNote(subject:String,content:String) { writableDatabase.execSQL("INSERT INTO notes(subject,content) VALUES(?,?)",arrayOf(subject,content)) }
 fun deleteNote(id:Long) { writableDatabase.delete("notes","id=?",arrayOf(id.toString())) }
 fun exams():List<Exam> = readableDatabase.rawQuery("SELECT id,subject,title,when_ms FROM exams ORDER BY when_ms",null).use { c -> buildList {while(c.moveToNext()) add(Exam(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3)))} }
 fun addExam(subject:String,title:String,whenMs:Long) { writableDatabase.execSQL("INSERT INTO exams(subject,title,when_ms) VALUES(?,?,?)",arrayOf(subject,title,whenMs)) }
 fun deleteExam(id:Long) { writableDatabase.delete("exams","id=?",arrayOf(id.toString())) }
}

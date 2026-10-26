package vn.loctm.ltmcampus

import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF2864E8),background=Color(0xFFF3F6FC),surface=Color(0xFFFAFCFF)),shapes=Shapes(medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp))) { CampusApp() } }
 }
}
private const val MAX_FILE=8*1024*1024
@Composable fun CampusApp() {
 val context=LocalContext.current
 val db=remember { CampusDb(context.applicationContext) }
 val scope=rememberCoroutineScope()
 val syncPrefs=remember{SyncScheduler.prefs(context)}
 var syncStatus by remember{mutableStateOf(syncPrefs.getString("status","disconnected")?:"disconnected")}
 var lastSync by remember{mutableLongStateOf(syncPrefs.getLong("lastSuccess",0))}
 var syncEnabled by remember{mutableStateOf(syncPrefs.getBoolean("enabled",false))}
 val lifecycleOwner=LocalLifecycleOwner.current
 var tab by rememberSaveable { mutableIntStateOf(0) }
 var revision by remember { mutableIntStateOf(0) }
 var message by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 var restore by remember { mutableStateOf<String?>(null) }
 var lessons by remember { mutableStateOf(emptyList<Lesson>()) }
 var notes by remember { mutableStateOf(emptyList<Note>()) }
 var exams by remember { mutableStateOf(emptyList<Exam>()) }
 DisposableEffect(lifecycleOwner) {
  val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,key->
   syncStatus=syncPrefs.getString("status","disconnected")?:"disconnected"
   syncEnabled=syncPrefs.getBoolean("enabled",false)
   lastSync=syncPrefs.getLong("lastSuccess",0)
   if(key=="lastSuccess")revision++
  }
  syncPrefs.registerOnSharedPreferenceChangeListener(listener)
  val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_START){revision++;SyncScheduler.refresh(context)}}
  lifecycleOwner.lifecycle.addObserver(observer)
  SyncScheduler.refresh(context)
  onDispose{syncPrefs.unregisterOnSharedPreferenceChangeListener(listener);lifecycleOwner.lifecycle.removeObserver(observer)}
 }
 LaunchedEffect(revision) {
  withContext(Dispatchers.IO) { Triple(db.lessons(),db.notes(),db.exams()) }.let { lessons=it.first;notes=it.second;exams=it.third }
 }
 fun runTask(block:()->String) { if(busy)return;busy=true;scope.launch {
  try {message=withContext(Dispatchers.IO){block()};revision++}catch(e:Exception){message="Không thực hiện được: ${e.message}"}finally{busy=false}
 } }
 fun readFile(uri:Uri):String=context.contentResolver.openInputStream(uri)?.use { stream ->
  val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);var count=stream.read(buffer)
  while(count!=-1){require(output.size()+count<=MAX_FILE){"Tệp vượt quá 8 MB"};output.write(buffer,0,count);count=stream.read(buffer)};output.toString("UTF-8")
 }?:error("Không đọc được tệp")
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null)runTask{val n=db.importSchedule(readFile(uri));"Đã nhập $n buổi học. Các môn và ghi chú cũ được giữ lại."} }
 val backupPicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null)runTask {restore=readFile(uri);"Đã đọc bản sao lưu. Vui lòng xác nhận khôi phục."} }
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null)runTask {
  context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(db.exportBackup())}?:error("Không ghi được tệp");"Đã xuất bản sao lưu"
 } }
 Scaffold(containerColor=Color.Transparent,topBar={Surface(color=Color.White.copy(alpha=.8f)) {Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=20.dp,vertical=14.dp)) {
  Text("LTM Campus",style=MaterialTheme.typography.headlineSmall);Text("Học tập chủ động, mỗi ngày",style=MaterialTheme.typography.bodySmall,color=Color(0xFF64748B))
 }}},bottomBar={NavigationBar(containerColor=Color.White.copy(alpha=.95f)) {
  val icons=listOf(Icons.Rounded.DateRange,Icons.Rounded.Edit,Icons.Rounded.CheckCircle,Icons.Rounded.AccountCircle)
  listOf("Lịch học","Ghi chú","Thi cử","Kết nối").forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(icons[i],t)},label={Text(t)})}
 }}) { padding ->
  Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF1FF),Color(0xFFF4F2FF),Color(0xFFF7FAFF)))).padding(padding).padding(horizontal=16.dp)) {
   if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
   if(message.isNotBlank())Row(Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(message,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);TextButton(onClick={message=""}){Text("Đóng")}}
   when(tab) {
    0->Column {
     Text(SyncScheduler.message(syncStatus),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=8.dp))
     ScheduleScreen(lessons,busy){if(syncEnabled)SyncScheduler.refresh(context,true)else context.startActivity(Intent(context,NctuActivity::class.java))}
    }
    1->NotesScreen(notes,busy){id,subject,content->runTask{if(id==null)db.addNote(subject,requireNotNull(content))else if(content==null)db.deleteNote(id)else db.updateNote(id,subject,content);"Đã cập nhật ghi chú"}}
    2->ExamsScreen(exams,busy,{subject,title,time->runTask{db.addExam(subject,title,time);"Đã thêm lịch thi"}},{id->runTask{db.deleteExam(id);"Đã xóa lịch thi"}})
    3->LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)) {
     item{Text("Kết nối & dữ liệu",style=MaterialTheme.typography.titleLarge)}
     item{GlassCard{
      Text("Tự cập nhật lịch NCTU",style=MaterialTheme.typography.titleMedium)
      Text("Đăng nhập NCTU một lần trong ứng dụng. Lịch tự cập nhật khi mở app và định kỳ dưới nền khi có mạng. Không lưu mật khẩu trên máy chủ riêng.")
      Text(SyncScheduler.message(syncStatus),color=MaterialTheme.colorScheme.primary)
      if(lastSync>0)Text("Lần cập nhật gần nhất: "+Instant.ofEpochMilli(lastSync).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),style=MaterialTheme.typography.bodySmall)
      Button(onClick={context.startActivity(Intent(context,NctuActivity::class.java))}){Text(if(syncEnabled)"Đăng nhập lại / xem cổng NCTU"else"Đăng nhập & bật tự cập nhật")}
      if(syncEnabled){OutlinedButton(onClick={SyncScheduler.refresh(context,true)}){Text("Cập nhật ngay")};TextButton(onClick={SyncScheduler.disconnect(context)}){Text("Tắt tự cập nhật")}}
      Text("Hết phiên: ứng dụng yêu cầu đăng nhập lại. Android có thể trì hoãn lịch chạy nền để tiết kiệm pin.",style=MaterialTheme.typography.bodySmall)
     }}
     item{GlassCard{Text("Nhập lịch dự phòng",style=MaterialTheme.typography.titleMedium);Text("Chỉ dùng khi cần nhập tệp lịch thủ công.");OutlinedButton(enabled=!busy,onClick={picker.launch(arrayOf("application/json","text/plain","*/*"))}){Text("Nhập JSON dự phòng")}}}
     item{GlassCard{Text("Sao lưu an toàn",style=MaterialTheme.typography.titleMedium);Text("Bản sao lưu chứa lịch học, ghi chú và lịch thi; không chứa phiên NCTU. Hãy lưu tệp ở nơi riêng tư.");OutlinedButton(enabled=!busy,onClick={exporter.launch("LTM-Campus-${LocalDate.now()}.json")}){Text("Xuất sao lưu")};OutlinedButton(enabled=!busy,onClick={backupPicker.launch(arrayOf("application/json","text/plain","*/*"))}){Text("Khôi phục từ tệp")}}}
     item{Text("Designed by LocTM · v0.3.0\nChúc các bạn học tốt, thi tốt!",style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(12.dp))}
    }
   }
  }
 }
 if(restore!=null)AlertDialog(onDismissRequest={restore=null},title={Text("Khôi phục dữ liệu?")},text={Text("Lịch học, ghi chú và lịch thi hiện tại sẽ được thay bằng dữ liệu trong tệp. Nên xuất bản sao lưu trước khi tiếp tục. Tệp lỗi sẽ không làm thay đổi dữ liệu hiện tại.")},confirmButton={TextButton(onClick={val raw=restore!!;restore=null;runTask{db.restoreBackup(raw);"Đã khôi phục dữ liệu"}}){Text("Khôi phục")}},dismissButton={TextButton(onClick={restore=null}){Text("Hủy")}})
}
@Composable private fun GlassCard(content:@Composable ColumnScope.()->Unit) {Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=.86f)),shape=RoundedCornerShape(22.dp)) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)}}
@Composable fun ScheduleScreen(lessons:List<Lesson>,busy:Boolean,onImport:()->Unit) {
 var dateText by rememberSaveable{mutableStateOf(LocalDate.now().toString())}
 var mode by rememberSaveable{mutableIntStateOf(1)}
 var query by rememberSaveable{mutableStateOf("")}
 val date=runCatching{LocalDate.parse(dateText)}.getOrDefault(LocalDate.now())
 val monday=date.minusDays((date.dayOfWeek.value-1).toLong())
 val shown=lessons.filter{ l-> val d=LocalDate.parse(l.date);(mode==2||if(mode==0)d==date else !d.isBefore(monday)&&d.isBefore(monday.plusDays(7)))&&(l.subject.contains(query,true)||l.code.contains(query,true)) }
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)) {
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Thời khóa biểu",style=MaterialTheme.typography.titleLarge);TextButton(enabled=!busy,onClick=onImport){Text("Cập nhật")}}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Ngày","Tuần","Tất cả").forEachIndexed{i,label->FilterChip(selected=mode==i,onClick={mode=i},label={Text(label)})}}}
  if(mode!=2)item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={dateText=date.minusDays(if(mode==0)1 else 7).toString()}){Text("‹ Trước")};TextButton(onClick={dateText=LocalDate.now().toString()}){Text(if(mode==0)date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) else "${monday.format(DateTimeFormatter.ofPattern("dd/MM"))} – ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM"))}")};TextButton(onClick={dateText=date.plusDays(if(mode==0)1 else 7).toString()}){Text("Sau ›")}}}
  item{OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Tìm môn học / mã lớp")},singleLine=true)}
  if(shown.isEmpty())item{GlassCard{Text(if(lessons.isEmpty())"Chưa có lịch học. Chọn Cập nhật để đăng nhập và kết nối NCTU." else "Không có buổi học trong lựa chọn này.")}}
  items(shown,key={it.id}){l->GlassCard{Text("${LocalDate.parse(l.date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} · ${l.start}–${l.end}",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge);Text(l.subject,style=MaterialTheme.typography.titleMedium);Text("${l.code} · ${l.room}");if(l.teacher.isNotBlank())Text(l.teacher,style=MaterialTheme.typography.bodySmall);if(l.suspended)Text("Tạm ngưng",color=MaterialTheme.colorScheme.error)}}
 }
}
@Composable fun NotesScreen(notes:List<Note>,busy:Boolean,onSave:(Long?,String,String?)->Unit) {
 var editing by rememberSaveable{mutableStateOf<Long?>(null)}
 var subject by rememberSaveable{mutableStateOf("")};var content by rememberSaveable{mutableStateOf("")};var delete by remember{mutableStateOf<Note?>(null)}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)) {
  item{Text("Ghi chú bài học",style=MaterialTheme.typography.titleLarge)}
  item{OutlinedTextField(subject,{subject=it},modifier=Modifier.fillMaxWidth(),label={Text("Môn học")},singleLine=true)}
  item{OutlinedTextField(content,{content=it},modifier=Modifier.fillMaxWidth(),label={Text("Nội dung ghi chú")},minLines=3)}
  item{Row{Button(enabled=!busy&&subject.isNotBlank()&&content.isNotBlank(),onClick={onSave(editing,subject.trim(),content.trim());editing=null;content=""}){Text(if(editing==null)"Lưu ghi chú" else "Lưu thay đổi")};if(editing!=null)TextButton(onClick={editing=null;subject="";content=""}){Text("Hủy sửa")}}}
  if(notes.isEmpty())item{Text("Ghi lại kiến thức quan trọng sau mỗi buổi học.")}
  items(notes,key={it.id}){n->GlassCard{Text(n.subject,style=MaterialTheme.typography.titleMedium);Text(n.content);Row{TextButton(enabled=!busy,onClick={editing=n.id;subject=n.subject;content=n.content}){Text("Sửa")};TextButton(enabled=!busy,onClick={delete=n}){Text("Xóa")}}}}
 }
 if(delete!=null)AlertDialog(onDismissRequest={delete=null},title={Text("Xóa ghi chú?")},text={Text(delete!!.subject)},confirmButton={TextButton(onClick={onSave(delete!!.id,"",null);delete=null}){Text("Xóa")}},dismissButton={TextButton(onClick={delete=null}){Text("Hủy")}})
}
@Composable fun ExamsScreen(exams:List<Exam>,busy:Boolean,onAdd:(String,String,Long)->Unit,onDelete:(Long)->Unit) {
 val context=LocalContext.current
 var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) { while(true) { now=System.currentTimeMillis();kotlinx.coroutines.delay(60_000) } }
 var subject by rememberSaveable{mutableStateOf("")};var title by rememberSaveable{mutableStateOf("")};var whenText by rememberSaveable{mutableStateOf("")};var error by remember{mutableStateOf("")};var delete by remember{mutableStateOf<Exam?>(null)}
 LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)) {
  item{Text("Thi & kiểm tra",style=MaterialTheme.typography.titleLarge)}
  item{OutlinedTextField(subject,{subject=it},modifier=Modifier.fillMaxWidth(),label={Text("Môn học")},singleLine=true)}
  item{OutlinedTextField(title,{title=it},modifier=Modifier.fillMaxWidth(),label={Text("Tên kỳ thi / kiểm tra")},singleLine=true)}
  item{OutlinedTextField(whenText,{whenText=it},modifier=Modifier.fillMaxWidth(),label={Text("Ngày giờ: 2026-12-20 08:00")},singleLine=true)}
  item{if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error);Button(enabled=!busy&&subject.isNotBlank()&&title.isNotBlank(),onClick={try{val whenMs=LocalDateTime.parse(whenText.trim().replace(' ','T')).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();require(whenMs>System.currentTimeMillis());onAdd(subject.trim(),title.trim(),whenMs);title="";whenText="";error=""}catch(_:Exception){error="Nhập ngày tương lai theo mẫu YYYY-MM-DD HH:mm (giờ Việt Nam)"}}){Text("Thêm lịch thi")}}
  if(exams.isEmpty())item{Text("Thêm kỳ thi sắp tới để theo dõi đếm ngược.")}
  items(exams,key={it.id}){x->val delta=x.whenMillis-now;GlassCard{Text(x.subject,style=MaterialTheme.typography.titleMedium);Text(x.title);Text(Instant.ofEpochMilli(x.whenMillis).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))+" · giờ VN");Text(if(delta>0)"Còn ${(delta+86_399_999)/86_400_000} ngày" else "Đã đến hạn",color=MaterialTheme.colorScheme.primary);Row{TextButton(onClick={try{context.startActivity(Intent(Intent.ACTION_INSERT,CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE,"${x.subject}: ${x.title}").putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,x.whenMillis).putExtra(CalendarContract.EXTRA_EVENT_END_TIME,x.whenMillis+3600000))}catch(_:Exception){error="Thiết bị chưa có ứng dụng lịch"}}){Text("Thêm vào lịch máy")};TextButton(enabled=!busy,onClick={delete=x}){Text("Xóa")}}}}
 }
 if(delete!=null)AlertDialog(onDismissRequest={delete=null},title={Text("Xóa lịch thi?")},text={Text(delete!!.title)},confirmButton={TextButton(onClick={onDelete(delete!!.id);delete=null}){Text("Xóa")}},dismissButton={TextButton(onClick={delete=null}){Text("Hủy")}})
}

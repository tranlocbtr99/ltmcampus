package vn.loctm.ltmcampus

import java.net.URI

object GoogleLoginNavigation {
 // Start the entire flow in the browser, so NCTU's OAuth state and cookies stay together.
 const val BROWSER_LOGIN="https://sv.nctu.edu.vn/login?auto_google=1"
 fun isLoginRedirect(raw:String):Boolean=runCatching {
  val u=URI(raw)
  if(u.userInfo!=null||u.port !in listOf(-1,443))return false
  val host=u.host?.lowercase()?:return false
  if(u.scheme.equals("https",true)&&host=="accounts.google.com")return u.path.startsWith("/o/oauth2/")||u.path.startsWith("/v3/signin")||u.path.startsWith("/signin/")
  if(u.scheme!="intent"||host!="sv.nctu.edu.vn"||u.path!="/login")return false
  val fragment=u.rawFragment?:return false
  val fields=fragment.split(';')
  fields.firstOrNull()=="Intent"&&fields.lastOrNull()=="end"&&fields.count{it=="scheme=https"}==1&&
   fields.none{it.startsWith("scheme=")&&it!="scheme=https"}&&u.rawQuery?.split('&')?.contains("auto_google=1")==true
 }.getOrDefault(false)
}

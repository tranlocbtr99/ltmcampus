package vn.loctm.ltmcampus

import java.net.URI

object PortalPolicy {
 fun isOfficial(url:String):Boolean=runCatching {
  val uri=URI(url)
  val host=uri.host?.lowercase()?:return false
  uri.scheme.equals("https",true)&&(host=="nctu.edu.vn"||host.endsWith(".nctu.edu.vn"))&&uri.userInfo==null&&(uri.port==-1||uri.port==443)
 }.getOrDefault(false)
}

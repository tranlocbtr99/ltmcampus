package vn.loctm.ltmcampus
import org.junit.Assert.*
import org.junit.Test
class GoogleLoginNavigationTest {
 @Test fun handlesOfficialChromeIntentWithoutGesture(){assertTrue(GoogleLoginNavigation.isLoginRedirect("intent://sv.nctu.edu.vn/login?auto_google=1#Intent;scheme=https;package=com.android.chrome;end"))}
 @Test fun handlesProgrammaticGoogleOAuthRedirect(){assertTrue(GoogleLoginNavigation.isLoginRedirect("https://accounts.google.com/o/oauth2/v2/auth?state=example"))}
 @Test fun blocksLookalikeAndCredentialHosts(){assertFalse(GoogleLoginNavigation.isLoginRedirect("https://accounts.google.com.evil.example/o/oauth2/v2/auth"));assertFalse(GoogleLoginNavigation.isLoginRedirect("https://evil@accounts.google.com/o/oauth2/v2/auth"))}
 @Test fun blocksOtherIntentSchemesAndTargets(){for(u in listOf("intent://evil.example/login?auto_google=1#Intent;scheme=https;end","intent://sv.nctu.edu.vn/login?auto_google=1#Intent;scheme=http;end","intent://sv.nctu.edu.vn/login?auto_google=1#Intent;scheme=https;scheme=javascript;end","intent://sv.nctu.edu.vn/dashboard#Intent;scheme=https;end"))assertFalse(GoogleLoginNavigation.isLoginRedirect(u))}
 @Test fun neverUsesIntentExtrasAsLaunchTarget(){assertTrue(GoogleLoginNavigation.isLoginRedirect("intent://sv.nctu.edu.vn/login?auto_google=1#Intent;scheme=https;component=evil/.Activity;S.browser_fallback_url=https%3A%2F%2Fevil.example;end"));assertEquals("https://sv.nctu.edu.vn/login?auto_google=1",GoogleLoginNavigation.BROWSER_LOGIN)}
}

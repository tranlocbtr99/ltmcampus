package vn.loctm.ltmcampus
import org.junit.Assert.*
import org.junit.Test
class PortalPolicyTest {
 @Test fun officialHttpsIsAllowed(){assertTrue(PortalPolicy.isOfficial("https://sv.nctu.edu.vn/dashboard"));assertTrue(PortalPolicy.isOfficial("https://NCTU.EDU.VN:443/"))}
 @Test fun lookalikeHostIsBlocked(){assertFalse(PortalPolicy.isOfficial("https://nctu.edu.vn.evil.example/"));assertFalse(PortalPolicy.isOfficial("https://notnctu.edu.vn/"))}
 @Test fun insecureAndUnexpectedPortsAreBlocked(){assertFalse(PortalPolicy.isOfficial("http://sv.nctu.edu.vn/"));assertFalse(PortalPolicy.isOfficial("https://sv.nctu.edu.vn:444/"));assertFalse(PortalPolicy.isOfficial("file:///etc/passwd"))}
 @Test fun credentialsAndMalformedUrlsAreBlocked(){assertFalse(PortalPolicy.isOfficial("https://user@sv.nctu.edu.vn/"));assertFalse(PortalPolicy.isOfficial("invalid url"))}
}

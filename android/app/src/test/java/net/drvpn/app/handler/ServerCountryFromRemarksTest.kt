package net.drvpn.app.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerCountryFromRemarksTest {

    private fun country(remarks: String) = ServerCountryManager.countryFromRemarks(remarks)

    @Test
    fun flagEmojiWins() {
        assertEquals("DE", country("🇩🇪 Germany fast"))
        assertEquals("NL", country("VIP 🇳🇱 | drvpn.net"))
    }

    @Test
    fun englishAndPersianNames() {
        assertEquals("FI", country("Finland-01"))
        assertEquals("TR", country("سرور ترکیه ۲"))
        assertEquals("GB", country("London, United Kingdom"))
    }

    @Test
    fun upperCaseIsoToken() {
        assertEquals("DE", country("DE-1 vless"))
        assertEquals("GB", country("UK 3"))
    }

    @Test
    fun protocolWordsAreNotCountries() {
        assertNull(country("Load tls_h2 xhttp CDN vless dl=h2"))
        assertNull(country("tls grpc CDN vmess ♾️"))
        assertNull(country("کوچک"))
    }

    @Test
    fun flagOfBuildsRegionalIndicators() {
        assertEquals("🇩🇪", ServerCountryManager.flagOf("DE"))
        assertNull(ServerCountryManager.flagOf("D"))
    }
}

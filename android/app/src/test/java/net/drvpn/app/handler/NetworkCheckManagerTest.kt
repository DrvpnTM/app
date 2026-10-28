package net.drvpn.app.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkCheckManagerTest {

    // Shape of a real bash.ws answer.
    private val sample = """
        [{"ip":"179.65.125.101","country":"de","country_name":"Germany","asn":"AS24940 Hetzner","type":"ip"},
         {"ip":"172.253.12.158","country":"us","country_name":"United States","asn":"AS15169 Google LLC","type":"dns"},
         {"ip":"2a00:1450:4025:1801::125","country":"ie","country_name":"Ireland","asn":"AS15169 Google LLC","type":"dns"},
         {"ip":"DNS may be leaking.","country":"","country_name":"","asn":"","type":"conclusion"}]
    """.trimIndent()

    @Test
    fun parsesExitServersAndConclusion() {
        val r = NetworkCheckManager.parseLeak(sample)
        assertEquals("179.65.125.101", r.exit?.ip)
        assertEquals("DE", r.exit?.country)
        assertEquals(2, r.servers.size)
        assertEquals(true, r.leaking)
    }

    @Test
    fun publicDnsAbroadIsNotALeak() {
        // bash.ws says "may be leaking" only because Google's resolvers sit in other countries.
        val r = NetworkCheckManager.parseLeak(sample)
        assertEquals(false, NetworkCheckManager.judge(r, ownAsn = 58224, ownCountry = "IR"))
    }

    private fun leakJson(dnsCountry: String, dnsAsn: String) =
        """[{"ip":"179.65.125.101","country":"de","asn":"AS24940 Hetzner","type":"ip"},""" +
            """{"ip":"5.200.200.200","country":"$dnsCountry","asn":"$dnsAsn","type":"dns"}]"""

    @Test
    fun ownProvidersResolverIsALeak() {
        val r = NetworkCheckManager.parseLeak(leakJson("ir", "AS58224 TCI"))
        assertEquals(true, NetworkCheckManager.judge(r, ownAsn = 58224, ownCountry = "IR"))
    }

    @Test
    fun resolverInOwnCountryWhileExitIsAbroadIsALeak() {
        val r = NetworkCheckManager.parseLeak(leakJson("ir", "AS12880 Other"))
        assertEquals(true, NetworkCheckManager.judge(r, ownAsn = null, ownCountry = "IR"))
    }

    @Test
    fun unknownOwnNetworkFallsBackToSiteConclusion() {
        val r = NetworkCheckManager.parseLeak(sample)
        assertEquals(true, NetworkCheckManager.judge(r, ownAsn = null, ownCountry = null))
    }

    @Test
    fun noResolversMeansNoVerdict() {
        val r = NetworkCheckManager.parseLeak("""[{"ip":"1.2.3.4","country":"de","asn":"AS1 X","type":"ip"}]""")
        assertNull(r.leaking)
        assertNull(NetworkCheckManager.judge(r, 1, "IR"))
    }

    @Test
    fun asnNumberFromLabel() {
        assertEquals(15169, NetworkCheckManager.asnNumber("AS15169 Google LLC"))
        assertNull(NetworkCheckManager.asnNumber("Google"))
    }
}

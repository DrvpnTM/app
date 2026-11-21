package net.drvpn.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RiseupTest {
    private val eip = """
        {"gateways":[
          {"capabilities":{"transport":[{"ports":["53","80","1194"],"protocols":["tcp","udp"],"type":"openvpn"},
             {"options":{"cert":"x","iatMode":"0"},"ports":["9443"],"protocols":["tcp"],"type":"obfs4"}]},
           "host":"vpn02-par.riseup.net","ip_address":"51.159.197.108","location":"Paris"},
          {"capabilities":{"transport":[{"ports":["9443"],"protocols":["tcp"],"type":"obfs4"}]},
           "host":"bridge","ip_address":"1.2.3.4","location":"Paris"},
          {"capabilities":{"transport":[{"ports":["1194"],"protocols":["udp"],"type":"openvpn"}]},
           "host":"vpn04-ams.riseup.net","ip_address":"51.15.9.205","location":"Amsterdam"}
        ],
        "openvpn_configuration":{"auth":"SHA512","cipher":"AES-256-GCM","dev":"tun","float":"","keepalive":"10 30",
          "nobind":true,"persist-key":true,"rcvbuf":"0","tls-cipher":"TLS-ECDHE-ECDSA-WITH-AES-256-GCM-SHA384","verb":"3",
          "bad option; rm":"x"}}
    """.trimIndent()
    private val pem = "-----BEGIN RSA PRIVATE KEY-----\nAAA\n-----END RSA PRIVATE KEY-----\n-----BEGIN CERTIFICATE-----\nBBB\n-----END CERTIFICATE-----\n"
    private val ca = "-----BEGIN CERTIFICATE-----\nCCC\n-----END CERTIFICATE-----\n"

    @Test
    fun parsesOpenVpnGatewaysOnly() {
        val gateways = RiseupManager.parseGateways(eip)
        assertEquals(listOf("51.159.197.108", "51.15.9.205"), gateways.map { it.ip })
        assertTrue(("tcp" to 80) in gateways[0].endpoints)
        assertFalse(("tcp" to 9443) in gateways[0].endpoints)
    }

    @Test
    fun buildsConfig() {
        val conf = RiseupManager.buildConfig(RiseupManager.Provider(eip, ca, pem), "/data/mgmt")
        val lines = conf.lines()
        assertTrue("client" in lines)
        assertTrue("cipher AES-256-GCM" in lines)
        assertTrue("float" in lines)
        assertTrue("nobind" in lines)
        assertTrue("management /data/mgmt unix" in lines)
        assertTrue("management-client" in lines)
        assertTrue(lines.none { it.startsWith("rcvbuf") || it.startsWith("dev tun ") || it.contains("rm") && it.startsWith("bad") })
        assertEquals(1, lines.count { it.startsWith("verb") })
        // Plain OpenVPN first: every gateway on UDP 1194 before any TCP 80; Amsterdam before Paris.
        val remotes = lines.filter { it.startsWith("remote ") }
        assertTrue(remotes.indexOf("remote 51.15.9.205 1194 udp") < remotes.indexOf("remote 51.159.197.108 80 tcp"))
        assertEquals("remote 51.15.9.205 1194 udp", remotes.first())
        assertFalse(remotes.any { it.contains("1.2.3.4") || it.contains("9443") })
        assertTrue(conf.contains("<key>\n-----BEGIN RSA PRIVATE KEY-----\nAAA"))
        assertTrue(conf.contains("<cert>\n-----BEGIN CERTIFICATE-----\nBBB"))
        assertTrue(conf.contains("<ca>\n-----BEGIN CERTIFICATE-----\nCCC"))
    }

    @Test
    fun filtersLocation() {
        val conf = RiseupManager.buildConfig(RiseupManager.Provider(eip, ca, pem), "/m", "Amsterdam")
        assertEquals(listOf("remote 51.15.9.205 1194 udp"), conf.lines().filter { it.startsWith("remote ") })
    }

    @Test
    fun masksAndRoutes() {
        assertEquals(21, OpenVpnSession.maskToPrefix("255.255.248.0"))
        assertEquals(1, OpenVpnSession.maskToPrefix("128.0.0.0"))
        assertEquals(32, OpenVpnSession.maskToPrefix("255.255.255.255"))
        assertNull(OpenVpnSession.maskToPrefix("255.0.255.0"))
        val tun = OpenVpnSession.TunConfig().apply {
            ipv4 = "10.42.0.93"; prefix4 = 21
            routes += Triple("0.0.0.0", 1, "10.42.0.1")
            routes += Triple("128.0.0.0", 1, "10.42.0.1")
            routes += Triple("51.15.9.205", 32, "192.168.1.1")
        }
        assertEquals(listOf("0.0.0.0" to 1, "128.0.0.0" to 1), OpenVpnSession.tunnelRoutes(tun))
    }

    /** With RISEUP_DIR (eip.json, ca.crt, cert.pem from the API), writes the real config for a manual test. */
    @Test
    fun writesLiveConfig() {
        val dir = System.getenv("RISEUP_DIR")?.let(::File) ?: return
        val provider = RiseupManager.Provider(
            File(dir, "eip.json").readText(), File(dir, "ca.crt").readText(), File(dir, "cert.pem").readText()
        )
        File(dir, "kotlin.ovpn").writeText(RiseupManager.buildConfig(provider, "$dir/mgmt"))
    }
}

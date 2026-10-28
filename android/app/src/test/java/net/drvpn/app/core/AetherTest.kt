package net.drvpn.app.core

import android.util.Log
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.AetherProtocol
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.fmt.AetherFmt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic

class AetherTest {
    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setUp() {
        mockLog = mockStatic(Log::class.java, Mockito.RETURNS_DEFAULTS)
    }

    @After
    fun tearDown() {
        mockLog.close()
    }

    private fun aether(block: ProfileItem.() -> Unit = {}) = ProfileItem.create(EConfigType.AETHER).apply(block)

    /** The value after [flag] in [args]. */
    private fun valueOf(args: List<String>, flag: String): String? = args.indexOf(flag).takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }

    // ---------- Links ----------

    @Test
    fun linkWithoutEndpointIsRead() {
        val p = AetherFmt.parse("aether://?protocol=masque&transport=h2&scan=turbo&ip=both#My%20WARP")!!
        assertEquals(EConfigType.AETHER, p.configType)
        assertEquals("My WARP", p.remarks)
        assertEquals("masque", p.aetherProtocol)
        assertEquals("h2", p.aetherTransport)
        assertEquals("turbo", p.aetherScanMode)
        assertEquals("both", p.aetherIpVersion)
        assertNull(p.server)
    }

    @Test
    fun linkRoundTripKeepsSettings() {
        val p = aether {
            remarks = "W"
            aetherProtocol = "masque"
            aetherTransport = "h3"
            aetherScanMode = "verified"
            aetherIpVersion = "v6"
            aetherTor = "chain"
            server = "162.159.192.1"
            serverPort = "443"
        }
        val again = AetherFmt.parse("aether://" + AetherFmt.toUri(p))!!
        assertEquals("masque", again.aetherProtocol)
        assertEquals("verified", again.aetherScanMode)
        assertEquals("v6", again.aetherIpVersion)
        assertEquals("chain", again.aetherTor)
        assertEquals("162.159.192.1", again.server)
        assertEquals("443", again.serverPort)
        assertEquals("W", again.remarks)
    }

    @Test
    fun twoHopLinkKeepsHops() {
        val p = AetherFmt.parse("aether://?protocol=gool&outer=1.1.1.1:2408&inner=8.8.8.8:500#x")!!
        assertEquals("1.1.1.1:2408", p.aetherWiwOuter)
        assertEquals("8.8.8.8:500", p.aetherWiwInner)
        val again = AetherFmt.parse("aether://" + AetherFmt.toUri(p))!!
        assertEquals("1.1.1.1:2408", again.aetherWiwOuter)
        assertEquals("8.8.8.8:500", again.aetherWiwInner)
    }

    // ---------- Command line ----------

    @Test
    fun defaultProfileListensOnTheAppPortAndUsesTheUpstream() {
        val args = AetherManager.arguments(aether(), port = 10819, upstreamPort = 10822)
        assertEquals("127.0.0.1:10819", valueOf(args, "--bind"))
        assertEquals("wg", valueOf(args, "--protocol"))
        assertEquals("socks5://127.0.0.1:10822", valueOf(args, "--upstream"))
        assertTrue("--quick-reconnect" in args)
        assertNull(valueOf(args, "--peer"))
    }

    @Test
    fun wgOverMasqueUsesGoolWithPeers() {
        val args = AetherManager.arguments(aether {
            aetherProtocol = AetherProtocol.WG_OVER_MASQUE.type
            aetherWiwOuter = "1.2.3.4:443"
            aetherWiwInner = "5.6.7.8:2408"
        })
        assertEquals("gool", valueOf(args, "--protocol"))
        assertEquals("1.2.3.4:443", valueOf(args, "--peer"))
        assertEquals("5.6.7.8:2408", valueOf(args, "--gool-peer"))
    }

    @Test
    fun torInsideWarpGivesTheAppPortToTor() {
        val args = AetherManager.arguments(aether { aetherTor = "chain" }, port = 10819)
        assertTrue("--tor" in args)
        assertEquals("127.0.0.1:10819", valueOf(args, "--tor-bind"))
        assertEquals("127.0.0.1:10820", valueOf(args, "--bind"))
    }

    @Test
    fun psiphonAloneHasNoWarpSettings() {
        val args = AetherManager.arguments(aether { aetherPsiphon = "only" }, port = 10819)
        assertTrue("--psiphon-only" in args)
        assertEquals("127.0.0.1:10819", valueOf(args, "--psiphon-bind"))
        assertTrue("--protocol" !in args)
    }

    @Test
    fun ownCommandRunsWithTheAppPort() {
        val args = AetherManager.arguments(
            aether { aetherCommand = "aether --protocol masque --bind 0.0.0.0:9999 --scan \"turbo\"" },
            port = 10819, upstreamPort = 10822,
        )
        assertEquals("masque", valueOf(args, "--protocol"))
        assertEquals("127.0.0.1:10819", valueOf(args, "--bind"))
        assertEquals("turbo", valueOf(args, "--scan"))
        assertEquals("socks5://127.0.0.1:10822", valueOf(args, "--upstream"))
    }

    @Test
    fun ownCommandWithTorDialsTorsListener() {
        val args = AetherManager.arguments(aether { aetherCommand = "--tor-only" }, port = 10819)
        assertEquals("127.0.0.1:10819", valueOf(args, "--tor-bind"))
    }

    @Test
    fun commandSplittingFollowsQuotes() {
        assertEquals(
            listOf("--dns", "1.1.1.1", "--x", "a b", "c'd"),
            AetherManager.splitCommand("aether --dns 1.1.1.1 --x 'a b' \"c'd\"")
        )
    }
}

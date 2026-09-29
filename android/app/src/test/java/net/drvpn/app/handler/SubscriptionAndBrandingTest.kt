package net.drvpn.app.handler

import net.drvpn.app.dto.entities.SubscriptionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionAndBrandingTest {

    @Test
    fun userInfoHeaderFillsUsageAndExpiry() {
        val sub = SubscriptionItem()
        AngConfigManager.applySubscriptionUserInfo(
            sub, "upload=100; download=900; total=10737418240; expire=1767225600"
        )
        assertEquals(1000L, sub.usedBytes)
        assertEquals(10737418240L, sub.totalBytes)
        assertEquals(1767225600_000L, sub.expireAt)
    }

    @Test
    fun zeroTotalAndExpiryMeanUnlimited() {
        val sub = SubscriptionItem()
        AngConfigManager.applySubscriptionUserInfo(sub, "upload=0; download=5; total=0; expire=0")
        assertEquals(5L, sub.usedBytes)
        assertEquals(-1L, sub.totalBytes)
        assertEquals(-1L, sub.expireAt)
    }

    @Test
    fun missingOrBrokenHeaderLeavesSubscriptionUntouched() {
        val sub = SubscriptionItem()
        val before = sub.copy()
        AngConfigManager.applySubscriptionUserInfo(sub, null)
        AngConfigManager.applySubscriptionUserInfo(sub, "   ")
        assertEquals(before, sub)
    }

    @Test
    fun floatValuesFromSomePanelsAreAccepted() {
        val sub = SubscriptionItem()
        AngConfigManager.applySubscriptionUserInfo(sub, "upload=1.0; download=2.0; total=3e3")
        assertEquals(3L, sub.usedBytes)
        assertEquals(3000L, sub.totalBytes)
    }

    @Test
    fun sellerChannelsAreReplacedWithOurSite() {
        assertEquals("DE fast drvpn.net", RemarksBranding.apply("DE fast @SomeSellerChannel"))
        assertEquals("drvpn.net", RemarksBranding.apply("t.me/other_channel"))
        assertEquals("NL drvpn.net", RemarksBranding.apply("NL https://example.com/buy"))
    }

    @Test
    fun ownChannelIsKeptAndRepeatsCollapse() {
        assertEquals("@drVPN_net", RemarksBranding.apply("@drVPN_net"))
        assertEquals("drvpn.net", RemarksBranding.apply("@a_seller | www.seller.com"))
        assertEquals("drvpn.net", RemarksBranding.apply(null))
    }
}

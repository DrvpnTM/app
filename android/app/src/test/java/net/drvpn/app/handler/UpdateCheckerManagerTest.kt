package net.drvpn.app.handler

import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerManagerTest {
    @Test
    fun betaVersionIsNewerThanPreviousStableVersion() {
        assertTrue(UpdateCheckerManager.compareVersions("0.2.2-beta.1", "0.2.1") > 0)
    }

    @Test
    fun stableVersionIsNewerThanItsBeta() {
        assertTrue(UpdateCheckerManager.compareVersions("0.2.2", "0.2.2-beta.1") > 0)
    }

    @Test
    fun numericPrereleaseIdentifiersCompareNumerically() {
        assertTrue(UpdateCheckerManager.compareVersions("0.2.2-beta.10", "0.2.2-beta.2") > 0)
    }

    @Test
    fun numericPrereleaseIdentifierHasLowerPrecedenceThanText() {
        assertTrue(UpdateCheckerManager.compareVersions("0.2.2-beta.1", "0.2.2-beta.alpha") < 0)
    }

    @Test
    fun stablePatchVersionsCompareNumerically() {
        assertTrue(UpdateCheckerManager.compareVersions("0.2.10", "0.2.9") > 0)
    }
}

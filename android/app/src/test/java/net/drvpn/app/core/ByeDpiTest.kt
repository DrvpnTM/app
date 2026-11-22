package net.drvpn.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ByeDpiTest {
    @Test
    fun defaultsParse() {
        assertEquals(listOf("--disorder", "1", "--auto=torst", "--tlsrec", "1+s"), ByeDpiManager.parseArgs(ByeDpiManager.DEFAULT_ARGS))
    }

    @Test
    fun quotesAndSpaces() {
        assertEquals(listOf("--fake", "-1", "--fake-sni", "www.google.com", "-l", ":GET / HTTP/1.1"),
            ByeDpiManager.parseArgs("  --fake -1 --fake-sni www.google.com -l ':GET / HTTP/1.1' "))
    }

    @Test
    fun appOptionsAreRefused() {
        assertNull(ByeDpiManager.parseArgs("--disorder 1 --port 9999"))
        assertNull(ByeDpiManager.parseArgs("-i 0.0.0.0 -d1"))
        assertNull(ByeDpiManager.parseArgs("--ip=0.0.0.0"))
        assertNull(ByeDpiManager.parseArgs("--daemon"))
        assertNull(ByeDpiManager.parseArgs("   "))
        assertNotNull(ByeDpiManager.parseArgs("-o1 -Ar -f-1 -r1+s -As"))
    }
}

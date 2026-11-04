package com.resonance.player.features

import com.resonance.player.sources.PublicSourceDns
import okhttp3.Dns
import java.net.InetAddress
import kotlin.test.*

class PublicSourceDnsTest {
    private fun address(value: String) = InetAddress.getByName(value)
    private fun system(value: String) = object : Dns {
        override fun lookup(hostname: String) = listOf(address(value))
    }

    @Test fun fakeIpUsesVerifiedPublicDestinationsAndCachesOnlySuccessfulResults() {
        var calls = 0
        val dns = PublicSourceDns(system("198.18.0.20")) { calls++; listOf(address("1.1.1.1")) }
        repeat(2) { assertEquals("1.1.1.1", dns.lookup("music.example.com").single().hostAddress) }
        assertEquals(1, calls)
    }

    @Test fun privateAndLiteralTargetsCannotUseVpnFallback() {
        for (value in listOf("127.0.0.1", "10.0.0.1", "192.168.1.1", "169.254.169.254", "100.100.100.200", "::1", "fc00::1")) {
            val dns = PublicSourceDns(system(value)) { error("Private address must never trigger fallback") }
            assertFailsWith<IllegalArgumentException> { dns.lookup("music.example.com") }
        }
        val dns = PublicSourceDns(system("198.18.0.1")) { error("Literal Fake-IP is not a public hostname") }
        assertFailsWith<IllegalArgumentException> { dns.lookup("198.18.0.1") }
    }

    @Test fun fallbackResponsesStillRejectPrivateFakeOrMixedAddresses() {
        for (values in listOf(listOf("127.0.0.1"), listOf("198.18.0.1"), listOf("1.1.1.1", "10.0.0.1"), emptyList())) {
            val dns = PublicSourceDns(system("198.18.0.10")) { values.map(::address) }
            assertFailsWith<IllegalArgumentException> { dns.lookup("music.example.com") }
        }
    }

    @Test fun ordinaryPublicDnsDoesNotContactAnotherResolver() {
        val dns = PublicSourceDns(system("1.1.1.1")) { error("No DNS fallback required") }
        assertEquals("1.1.1.1", dns.lookup("music.example.com").single().hostAddress)
    }
}

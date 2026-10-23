package fr.globoudou.subnet.core

import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {

    private fun ok4(s: String) = Ipv4Parser.parse(s) as Ipv4Input.Ok

    @Test
    fun ipv4Basics() {
        val r = Ipv4Result(Ipv4.parseAddress("192.168.10.37")!!, 26)
        assertEquals("192.168.10.0", Ipv4.format(r.network))
        assertEquals("192.168.10.63", Ipv4.format(r.broadcast))
        assertEquals("192.168.10.1", Ipv4.format(r.first))
        assertEquals("192.168.10.62", Ipv4.format(r.last))
        assertEquals("255.255.255.192", Ipv4.format(r.mask))
        assertEquals("0.0.0.63", Ipv4.format(r.wildcard))
        assertEquals(62, r.hosts)
        assertEquals('C', r.classLetter)
        assertEquals(AddressKind.PRIVATE, r.category.kind)
        assertEquals("10.168.192.in-addr.arpa", r.reverseZone)
    }

    @Test
    fun ipv4Edges() {
        val r0 = Ipv4Result(Ipv4.parseAddress("8.8.8.8")!!, 0)
        assertEquals("0.0.0.0", Ipv4.format(r0.network))
        assertEquals("255.255.255.255", Ipv4.format(r0.broadcast))
        assertEquals(4294967294, r0.hosts)
        val r31 = Ipv4Result(Ipv4.parseAddress("10.0.0.1")!!, 31)
        assertEquals("10.0.0.0", Ipv4.format(r31.first))
        assertEquals("10.0.0.1", Ipv4.format(r31.last))
        assertEquals(2, r31.hosts)
        val r32 = Ipv4Result(Ipv4.parseAddress("10.0.0.1")!!, 32)
        assertEquals(1, r32.hosts)
        assertEquals("10.0.0.1", Ipv4.format(r32.first))
    }

    @Test
    fun ipv4TolerantParsing() {
        assertEquals(24, ok4("192.168.1.10/24").prefix)
        assertEquals(24, ok4("  192.168.1.10 255.255.255.0 ").prefix)
        assertEquals(24, ok4("192.168.1.10/255.255.255.0").prefix)
        assertEquals(20, ok4("192.168.1.10 20").prefix)
        assertEquals(16, ok4("10.0.0.1, 16").prefix)
        assertNull(ok4("192.168.001.010").prefix)
        assertEquals("192.168.1.10", Ipv4.format(ok4("192.168.001.010").ip))
        assertIs<Ipv4Input.Error>(Ipv4Parser.parse("192.168.1.256"))
        assertIs<Ipv4Input.Error>(Ipv4Parser.parse("192.168.1.1/33"))
        assertIs<Ipv4Input.Error>(Ipv4Parser.parse("192.168.1.1/255.0.255.0"))
        assertIs<Ipv4Input.Error>(Ipv4Parser.parse("192.168..1"))
        assertIs<Ipv4Input.Incomplete>(Ipv4Parser.parse("192.168."))
        assertIs<Ipv4Input.Incomplete>(Ipv4Parser.parse("192.168.1.1/"))
        assertIs<Ipv4Input.Incomplete>(Ipv4Parser.parse(""))
    }

    @Test
    fun masks() {
        assertEquals(0, Ipv4.maskToPrefix(0))
        assertEquals(32, Ipv4.maskToPrefix(Ipv4.MAX))
        assertEquals(22, Ipv4.maskToPrefix(Ipv4.parseAddress("255.255.252.0")!!))
        assertNull(Ipv4.maskToPrefix(Ipv4.parseAddress("255.255.253.0")!!))
        for (p in 0..32) assertEquals(p, Ipv4.maskToPrefix(Ipv4.prefixToMask(p)))
    }

    @Test
    fun classification() {
        fun k(s: String) = Ipv4Registry.classify(Ipv4.parseAddress(s)!!)
        assertEquals(AddressKind.PUBLIC, k("8.8.8.8").kind)
        assertEquals(AddressKind.PRIVATE, k("172.31.255.1").kind)
        assertEquals(AddressKind.PUBLIC, k("172.32.0.1").kind)
        assertEquals(AddressKind.SHARED, k("100.100.1.1").kind)
        assertEquals(AddressKind.LOOPBACK, k("127.0.0.1").kind)
        assertEquals(AddressKind.LINK_LOCAL, k("169.254.3.4").kind)
        assertEquals("Multicast lien local", k("224.0.0.251").label)
        assertEquals("Multicast administré", k("239.1.2.3").label)
        assertEquals(AddressKind.RESERVED, k("250.0.0.1").kind)
        assertEquals(AddressKind.BROADCAST, k("255.255.255.255").kind)
        assertEquals(AddressKind.DOCUMENTATION, k("203.0.113.9").kind)
    }

    @Test
    fun vlsm() {
        val r = Splitter.vlsm(
            Ipv4.parseAddress("192.168.1.0")!!, 24,
            listOf(Need("LAN", 100), Need("Wi-Fi", 50), Need("DMZ", 20), Need("Admin", 2)),
        ) as VlsmResult.Ok
        assertEquals(listOf("192.168.1.0/25", "192.168.1.128/26", "192.168.1.192/27", "192.168.1.224/30"),
            r.allocations.map { "${Ipv4.format(it.network)}/${it.prefix}" })
        assertEquals(listOf("192.168.1.228/30", "192.168.1.232/29", "192.168.1.240/28"),
            r.free.map { "${Ipv4.format(it.first)}/${it.second}" })
        assertEquals(228, r.used)
        // ordre de saisie conservé même si les tailles sont mélangées
        val r2 = Splitter.vlsm(Ipv4.parseAddress("10.0.0.0")!!, 24, listOf(Need("a", 2), Need("b", 100))) as VlsmResult.Ok
        assertEquals("a", r2.allocations[0].name)
        assertEquals("10.0.0.128", Ipv4.format(r2.allocations[0].network))
        assertIs<VlsmResult.Error>(Splitter.vlsm(Ipv4.parseAddress("10.0.0.0")!!, 24, listOf(Need("x", 300))))
    }

    @Test
    fun parts() {
        assertEquals(26, Splitter.prefixForParts(24, 4))
        assertEquals(27, Splitter.prefixForParts(24, 5))
        assertNull(Splitter.prefixForParts(30, 8))
        assertEquals("10.0.0.192", Ipv4.format(Splitter.subnetAt(Ipv4.parseAddress("10.0.0.0")!!, 24, 26, 3)))
    }

    @Test
    fun prefixScale() {
        assertEquals(0f, PrefixScale.position(0))
        assertEquals(1f, PrefixScale.position(32))
        for (p in 0..32) assertEquals(p, PrefixScale.prefixAt(PrefixScale.position(p)))
        // le dernier octet occupe plus de 40 % de la course
        assertTrue(1f - PrefixScale.position(24) > 0.4f)
    }

    @Test
    fun ipv6() {
        val ip = Ipv6.parseAddress("2001:DB8:0:0:1:0:0:1")!!
        assertEquals("2001:db8::1:0:0:1", Ipv6.compress(ip))
        assertEquals("2001:0db8:0000:0000:0001:0000:0000:0001", Ipv6.expand(ip))
        assertEquals("::1", Ipv6.compress(Ipv6.parseAddress("0:0:0:0:0:0:0:1")!!))
        assertEquals("::", Ipv6.compress(BigInteger.ZERO))
        assertEquals("fe80::1", Ipv6.compress(Ipv6.parseAddress("fe80::1%wlan0")!!))
        assertEquals("::ffff:192.168.1.1", Ipv6.compress(Ipv6.parseAddress("::ffff:192.168.1.1")!!))
        assertEquals("2001:db8:0:1:1:1:1:1", Ipv6.compress(Ipv6.parseAddress("2001:db8:0:1:1:1:1:1")!!))
        val r = Ipv6Result(Ipv6.parseAddress("2001:db8:abcd:12::7")!!, 48)
        assertEquals("2001:db8:abcd::/48", r.networkCidr)
        assertEquals("2001:db8:abcd:ffff:ffff:ffff:ffff:ffff", Ipv6.compress(r.last))
        assertEquals(BigInteger.valueOf(65536), r.subnets64)
        assertEquals("d.c.b.a.8.b.d.0.1.0.0.2.ip6.arpa", r.reverseZone)
        assertEquals(AddressKind.DOCUMENTATION, r.category.kind)
        assertEquals(AddressKind.PRIVATE, Ipv6Registry.classify(Ipv6.parseAddress("fd12::1")!!).kind)
        assertEquals("Multicast · portée lien local", Ipv6Registry.classify(Ipv6.parseAddress("ff02::1")!!).label)
        assertEquals(AddressKind.PUBLIC, Ipv6Registry.classify(Ipv6.parseAddress("2a01:cb00::1")!!).kind)
        assertNull(Ipv6.parseAddress("1::2::3"))
        assertNull(Ipv6.parseAddress("12345::"))
        assertNull(Ipv6.parseAddress("1:2:3:4:5:6:7"))
        assertIs<Ipv6Input.Incomplete>(Ipv6Parser.parse("2001:db8:"))
        assertEquals(64, (Ipv6Parser.parse("[2001:db8::1]/64") as Ipv6Input.Ok).prefix)
    }

    @Test
    fun keypad() {
        fun typeAll(s: String) = s.fold("") { acc, c -> KeypadEditor.type(acc, c) }
        assertEquals("192.168.1.10/24", typeAll("192.168.1.10/24"))
        assertEquals("192.168.1.10/24", typeAll("1921681.10/24"))   // points auto après 3 chiffres
        assertEquals("25.6", typeAll("256"))                        // 256 > 255 → nouveau octet
        assertEquals("1.2.3.4", typeAll("1..2.3.4"))               // double point ignoré
        assertEquals("1.2.3", typeAll("1.2.3/"))                    // pas de / sur adresse incomplète
        assertEquals("10.0.0.1/3", typeAll("10.0.0.1/39"))         // préfixe > 32 refusé
        assertEquals("10.0.0.1/255.255.255.0", typeAll("10.0.0.1/255.255.255.0"))
        assertEquals("10.0.0.123", typeAll("10.0.0.1234"))         // 4e octet plein
        assertEquals("", typeAll("./"))
    }

    @Test
    fun formatting() {
        assertEquals("4 294 967 294", Fmt.int(4294967294))
        assertEquals("2¹²⁸ ≈ 3,40 × 10³⁸", Fmt.big(Ipv6.addressCount(0)))
    }
}

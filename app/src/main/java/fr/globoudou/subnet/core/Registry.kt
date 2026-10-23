package fr.globoudou.subnet.core

import java.math.BigInteger

/** Grande famille d'adresse, utilisée pour la couleur de l'étiquette. */
enum class AddressKind { PUBLIC, PRIVATE, SHARED, LOOPBACK, LINK_LOCAL, MULTICAST, DOCUMENTATION, SPECIAL, RESERVED, BROADCAST }

/** Étiquette affichée : libellé court, explication, référence (RFC). */
data class AddressCategory(
    val kind: AddressKind,
    val label: String,
    val detail: String,
    val rfc: String?,
)

private class V4Block(val net: Long, val prefix: Int, val category: AddressCategory)

/** Registre IANA des blocs IPv4 à usage spécial (correspondance du préfixe le plus long). */
object Ipv4Registry {

    private fun b(cidr: String, kind: AddressKind, label: String, detail: String, rfc: String?): V4Block {
        val (a, p) = cidr.split('/')
        return V4Block(Ipv4.parseAddress(a)!!, p.toInt(), AddressCategory(kind, label, detail, rfc))
    }

    private val blocks = listOf(
        b("0.0.0.0/8", AddressKind.SPECIAL, "« Ce réseau »", "Adresse source uniquement, non routable", "RFC 791"),
        b("0.0.0.0/32", AddressKind.SPECIAL, "Non spécifiée", "0.0.0.0 : toutes les interfaces / aucune adresse", "RFC 1122"),
        b("10.0.0.0/8", AddressKind.PRIVATE, "Privée", "Réseau privé, non routé sur Internet", "RFC 1918"),
        b("100.64.0.0/10", AddressKind.SHARED, "CGNAT", "Espace partagé des opérateurs (NAT de niveau opérateur)", "RFC 6598"),
        b("127.0.0.0/8", AddressKind.LOOPBACK, "Loopback", "Boucle locale de la machine", "RFC 1122"),
        b("169.254.0.0/16", AddressKind.LINK_LOCAL, "Lien local", "Auto-configuration (APIPA), non routée", "RFC 3927"),
        b("172.16.0.0/12", AddressKind.PRIVATE, "Privée", "Réseau privé, non routé sur Internet", "RFC 1918"),
        b("192.0.0.0/24", AddressKind.SPECIAL, "Protocoles IETF", "Réservé aux affectations de protocoles", "RFC 6890"),
        b("192.0.0.0/29", AddressKind.SPECIAL, "DS-Lite", "Tunnel IPv4 dans IPv6 (B4/AFTR)", "RFC 7335"),
        b("192.0.2.0/24", AddressKind.DOCUMENTATION, "Documentation", "TEST-NET-1, exemples uniquement", "RFC 5737"),
        b("192.88.99.0/24", AddressKind.RESERVED, "Relais 6to4", "Anycast 6to4, obsolète", "RFC 7526"),
        b("192.168.0.0/16", AddressKind.PRIVATE, "Privée", "Réseau privé, non routé sur Internet", "RFC 1918"),
        b("198.18.0.0/15", AddressKind.SPECIAL, "Tests de performance", "Banc d'essai d'équipements réseau", "RFC 2544"),
        b("198.51.100.0/24", AddressKind.DOCUMENTATION, "Documentation", "TEST-NET-2, exemples uniquement", "RFC 5737"),
        b("203.0.113.0/24", AddressKind.DOCUMENTATION, "Documentation", "TEST-NET-3, exemples uniquement", "RFC 5737"),
        b("224.0.0.0/4", AddressKind.MULTICAST, "Multicast", "Diffusion de groupe (classe D)", "RFC 5771"),
        b("224.0.0.0/24", AddressKind.MULTICAST, "Multicast lien local", "Contrôle réseau local (OSPF, VRRP, mDNS…), TTL 1", "RFC 5771"),
        b("224.0.1.0/24", AddressKind.MULTICAST, "Multicast inter-réseaux", "Contrôle inter-réseaux (NTP…)", "RFC 5771"),
        b("232.0.0.0/8", AddressKind.MULTICAST, "Multicast SSM", "Source-Specific Multicast", "RFC 4607"),
        b("233.0.0.0/8", AddressKind.MULTICAST, "Multicast GLOP", "Adressage dérivé d'un numéro d'AS", "RFC 3180"),
        b("239.0.0.0/8", AddressKind.MULTICAST, "Multicast administré", "Portée locale à l'organisation", "RFC 2365"),
        b("240.0.0.0/4", AddressKind.RESERVED, "Réservée", "Classe E, usage futur", "RFC 1112"),
        b("255.255.255.255/32", AddressKind.BROADCAST, "Broadcast limité", "Diffusion sur le lien local uniquement", "RFC 919"),
    )

    private val public = AddressCategory(AddressKind.PUBLIC, "Publique", "Adresse routable sur Internet", null)

    fun classify(ip: Long): AddressCategory =
        blocks.filter { (ip and Ipv4.prefixToMask(it.prefix)) == it.net }
            .maxByOrNull { it.prefix }?.category ?: public
}

private class V6Block(val net: BigInteger, val prefix: Int, val category: AddressCategory)

/** Registre des blocs IPv6 notables (correspondance du préfixe le plus long). */
object Ipv6Registry {

    private fun b(cidr: String, kind: AddressKind, label: String, detail: String, rfc: String?): V6Block {
        val (a, p) = cidr.split('/')
        return V6Block(Ipv6.parseAddress(a)!!, p.toInt(), AddressCategory(kind, label, detail, rfc))
    }

    private val blocks = listOf(
        b("::/128", AddressKind.SPECIAL, "Non spécifiée", "Absence d'adresse", "RFC 4291"),
        b("::1/128", AddressKind.LOOPBACK, "Loopback", "Boucle locale de la machine", "RFC 4291"),
        b("::ffff:0:0/96", AddressKind.SPECIAL, "IPv4 mappée", "Adresse IPv4 représentée en IPv6", "RFC 4291"),
        b("64:ff9b::/96", AddressKind.SPECIAL, "NAT64", "Préfixe bien connu de traduction IPv4/IPv6", "RFC 6052"),
        b("64:ff9b:1::/48", AddressKind.SPECIAL, "NAT64 local", "Traduction IPv4/IPv6 locale", "RFC 8215"),
        b("100::/64", AddressKind.SPECIAL, "Discard", "Trafic à jeter (blackhole)", "RFC 6666"),
        b("2000::/3", AddressKind.PUBLIC, "Unicast global", "Adresse publique routable sur Internet", "RFC 4291"),
        b("2001::/32", AddressKind.SPECIAL, "Teredo", "Tunnel IPv6 sur UDP/IPv4", "RFC 4380"),
        b("2001:db8::/32", AddressKind.DOCUMENTATION, "Documentation", "Exemples uniquement", "RFC 3849"),
        b("2002::/16", AddressKind.SPECIAL, "6to4", "Tunnel automatique 6to4 (obsolète)", "RFC 3056"),
        b("3fff::/20", AddressKind.DOCUMENTATION, "Documentation", "Exemples uniquement", "RFC 9637"),
        b("fc00::/7", AddressKind.PRIVATE, "ULA (privée)", "Adresse locale unique, non routée sur Internet", "RFC 4193"),
        b("fe80::/10", AddressKind.LINK_LOCAL, "Lien local", "Valide sur le seul lien, non routée", "RFC 4291"),
        b("fec0::/10", AddressKind.RESERVED, "Site local", "Obsolète", "RFC 3879"),
        b("ff00::/8", AddressKind.MULTICAST, "Multicast", "Diffusion de groupe", "RFC 4291"),
    )

    private val reserved = AddressCategory(AddressKind.RESERVED, "Réservée IETF", "Hors de l'espace unicast global attribué", "RFC 4291")

    fun classify(ip: BigInteger): AddressCategory {
        val found = blocks.filter { Ipv6.networkOf(ip, it.prefix) == it.net }.maxByOrNull { it.prefix }
            ?: return reserved
        if (found.category.kind == AddressKind.MULTICAST) {
            val scope = ip.shiftRight(112).toInt() and 0xF
            val scopeName = when (scope) {
                1 -> "interface"
                2 -> "lien local"
                3 -> "domaine"
                4 -> "administration"
                5 -> "site"
                8 -> "organisation"
                0xE -> "globale"
                else -> "réservée ($scope)"
            }
            return found.category.copy(label = "Multicast · portée $scopeName")
        }
        return found.category
    }
}

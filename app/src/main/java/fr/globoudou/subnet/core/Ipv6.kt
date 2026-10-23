package fr.globoudou.subnet.core

import java.math.BigInteger

/** Calculs IPv6 purs, adresses en BigInteger 0..2^128-1. */
object Ipv6 {

    val MAX: BigInteger = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)
    private val MAPPED_PREFIX: BigInteger = BigInteger("ffff", 16).shiftLeft(32)

    fun mask(prefix: Int): BigInteger {
        require(prefix in 0..128)
        return MAX.shiftRight(128 - prefix).shiftLeft(128 - prefix)
    }

    fun networkOf(ip: BigInteger, prefix: Int): BigInteger = ip.and(mask(prefix))

    fun lastOf(ip: BigInteger, prefix: Int): BigInteger = networkOf(ip, prefix).or(MAX.xor(mask(prefix)))

    fun groups(v: BigInteger): IntArray = IntArray(8) { i -> v.shiftRight(16 * (7 - i)).toInt() and 0xFFFF }

    fun parseAddress(text: String): BigInteger? = (parseDetailed(text) as? AddressParse.Ok)?.value

    sealed class AddressParse {
        data class Ok(val value: BigInteger) : AddressParse()
        data class Error(val message: String) : AddressParse()
    }

    fun parseDetailed(raw: String): AddressParse {
        var s = raw.trim().lowercase()
        if (s.isEmpty()) return AddressParse.Error("Adresse vide")
        val zone = s.indexOf('%')
        if (zone >= 0) s = s.substring(0, zone)

        // IPv4 embarquée en fin d'adresse (::ffff:192.168.1.1)
        var tailV4: Long? = null
        if (s.contains('.')) {
            val lastColon = s.lastIndexOf(':')
            if (lastColon < 0) return AddressParse.Error("Ce n'est pas une adresse IPv6")
            tailV4 = Ipv4.parseAddress(s.substring(lastColon + 1))
                ?: return AddressParse.Error("Partie IPv4 embarquée invalide")
            s = s.substring(0, lastColon + 1) + "0:0"
        }

        if (s.any { !(it in '0'..'9' || it in 'a'..'f' || it == ':') }) return AddressParse.Error("Caractère invalide")
        val dbl = s.indexOf("::")
        if (dbl >= 0 && s.indexOf("::", dbl + 1) >= 0) return AddressParse.Error("« :: » ne peut apparaître qu'une fois")
        if (s.contains(":::")) return AddressParse.Error("Trop de « : » consécutifs")

        fun splitGroups(part: String): List<String>? {
            if (part.isEmpty()) return emptyList()
            val g = part.split(':')
            if (g.any { it.isEmpty() || it.length > 4 }) return null
            return g
        }

        val groups: List<String> = if (dbl >= 0) {
            val head = splitGroups(s.substring(0, dbl)) ?: return AddressParse.Error("Groupe invalide (1 à 4 chiffres hexadécimaux)")
            val tail = splitGroups(s.substring(dbl + 2)) ?: return AddressParse.Error("Groupe invalide (1 à 4 chiffres hexadécimaux)")
            if (head.size + tail.size > 7) return AddressParse.Error("Trop de groupes avec « :: »")
            head + List(8 - head.size - tail.size) { "0" } + tail
        } else {
            val g = splitGroups(s) ?: return AddressParse.Error("Groupe invalide (1 à 4 chiffres hexadécimaux)")
            if (g.size != 8) return AddressParse.Error(if (g.size < 8) "Il manque des groupes (8 attendus, ou « :: »)" else "Trop de groupes (8 maximum)")
            g
        }

        var v = BigInteger.ZERO
        for (g in groups) v = v.shiftLeft(16).or(BigInteger(g, 16))
        if (tailV4 != null) v = v.shiftRight(32).shiftLeft(32).or(BigInteger.valueOf(tailV4))
        return AddressParse.Ok(v)
    }

    /** Forme compressée canonique (RFC 5952). */
    fun compress(v: BigInteger): String {
        val g = groups(v)
        // IPv4 mappée : notation mixte recommandée
        if (v.shiftRight(32) == BigInteger("ffff", 16)) {
            return "::ffff:" + Ipv4.format(v.and(BigInteger.valueOf(0xFFFF_FFFFL)).toLong())
        }
        var bestStart = -1
        var bestLen = 0
        var i = 0
        while (i < 8) {
            if (g[i] == 0) {
                var j = i
                while (j < 8 && g[j] == 0) j++
                if (j - i > bestLen) { bestStart = i; bestLen = j - i }
                i = j
            } else i++
        }
        if (bestLen < 2) return g.joinToString(":") { Integer.toHexString(it) }
        val head = (0 until bestStart).joinToString(":") { Integer.toHexString(g[it]) }
        val tail = (bestStart + bestLen until 8).joinToString(":") { Integer.toHexString(g[it]) }
        return "$head::$tail"
    }

    fun expand(v: BigInteger): String = groups(v).joinToString(":") { it.toString(16).padStart(4, '0') }

    fun reverseZone(network: BigInteger, prefix: Int): String {
        val nibbles = prefix / 4
        if (nibbles == 0) return "ip6.arpa"
        val hex = network.toString(16).padStart(32, '0')
        return hex.substring(0, nibbles).reversed().toCharArray().joinToString(".") + ".ip6.arpa"
    }

    fun addressCount(prefix: Int): BigInteger = BigInteger.ONE.shiftLeft(128 - prefix)
}

data class Ipv6Result(val ip: BigInteger, val prefix: Int) {
    val network: BigInteger = Ipv6.networkOf(ip, prefix)
    val last: BigInteger = Ipv6.lastOf(ip, prefix)
    val count: BigInteger = Ipv6.addressCount(prefix)
    val subnets64: BigInteger? = if (prefix <= 64) BigInteger.ONE.shiftLeft(64 - prefix) else null
    val category: AddressCategory = Ipv6Registry.classify(ip)
    val reverseZone: String = Ipv6.reverseZone(network, prefix)
    val networkCidr: String get() = "${Ipv6.compress(network)}/$prefix"
}

sealed class Ipv6Input {
    data class Incomplete(val hint: String) : Ipv6Input()
    data class Error(val message: String) : Ipv6Input()
    data class Ok(val ip: BigInteger, val prefix: Int?, val ipText: String) : Ipv6Input()
}

object Ipv6Parser {
    fun parse(raw: String): Ipv6Input {
        var text = raw.trim().lineSequence().firstOrNull()?.trim().orEmpty()
        if (text.isEmpty()) return Ipv6Input.Incomplete("Saisis une adresse, ex. 2001:db8::1/64")
        text = text.removePrefix("[")
        val close = text.indexOf(']')
        if (close >= 0) text = text.removeRange(close, close + 1)
        val sep = text.indexOfFirst { it == '/' || it == ' ' || it == '\t' }
        val ipText = (if (sep < 0) text else text.substring(0, sep)).trim()
        val rest = if (sep < 0) null else text.substring(sep + 1).trim { it == ' ' || it == '/' || it == '\t' }

        val ip = when (val r = Ipv6.parseDetailed(ipText)) {
            is Ipv6.AddressParse.Ok -> r.value
            is Ipv6.AddressParse.Error -> {
                // saisie en cours : on attend la suite plutôt que d'afficher une erreur
                if (rest == null && (ipText.endsWith(':') || ipText.endsWith('.')) && r.message != "Caractère invalide")
                    return Ipv6Input.Incomplete("Adresse en cours de saisie…")
                if (rest == null && !ipText.contains("::") && ipText.count { it == ':' } < 7 && r.message.startsWith("Il manque"))
                    return Ipv6Input.Incomplete("Adresse en cours de saisie… (« :: » abrège les zéros)")
                return Ipv6Input.Error(r.message)
            }
        }
        if (rest == null) return Ipv6Input.Ok(ip, null, ipText)
        if (rest.isEmpty()) return Ipv6Input.Incomplete("Indique un préfixe (0-128)")
        if (rest.any { it !in '0'..'9' } || rest.length > 3) return Ipv6Input.Error("Préfixe invalide (0-128)")
        val p = rest.toInt()
        if (p > 128) return Ipv6Input.Error("Préfixe hors plage (0-128)")
        return Ipv6Input.Ok(ip, p, ipText)
    }
}

package fr.globoudou.subnet.core

/**
 * Calculs IPv4 purs (aucune dépendance Android), testables sur JVM.
 * Les adresses sont manipulées comme des Long non signés 0..2^32-1.
 */
object Ipv4 {

    const val MAX = 0xFFFF_FFFFL

    fun format(v: Long): String =
        "${(v ushr 24) and 255}.${(v ushr 16) and 255}.${(v ushr 8) and 255}.${v and 255}"

    fun octets(v: Long): IntArray = intArrayOf(
        ((v ushr 24) and 255).toInt(),
        ((v ushr 16) and 255).toInt(),
        ((v ushr 8) and 255).toInt(),
        (v and 255).toInt(),
    )

    fun prefixToMask(prefix: Int): Long {
        require(prefix in 0..32)
        return if (prefix == 0) 0L else (MAX shl (32 - prefix)) and MAX
    }

    /** Longueur de préfixe d'un masque, ou null si le masque n'est pas contigu. */
    fun maskToPrefix(mask: Long): Int? {
        val inverted = mask.inv() and MAX
        // un masque contigu a la forme 1…10…0 : son complément + 1 est une puissance de deux
        if ((inverted and (inverted + 1)) != 0L) return null
        return java.lang.Long.bitCount(mask)
    }

    /** Analyse stricte d'une adresse complète (4 octets décimaux 0-255, zéros de tête tolérés). */
    fun parseAddress(text: String): Long? = when (val r = parseAddressDetailed(text.trim())) {
        is AddressParse.Ok -> r.value
        else -> null
    }

    sealed class AddressParse {
        data class Ok(val value: Long) : AddressParse()
        object Incomplete : AddressParse()
        data class Error(val message: String) : AddressParse()
    }

    fun parseAddressDetailed(text: String): AddressParse {
        if (text.isEmpty()) return AddressParse.Incomplete
        if (text.any { it != '.' && !it.isAsciiDigit() }) return AddressParse.Error("Caractère invalide dans l'adresse")
        val parts = text.split('.')
        if (parts.size > 4) return AddressParse.Error("Trop d'octets (4 maximum)")
        var value = 0L
        for ((i, p) in parts.withIndex()) {
            if (p.isEmpty()) {
                return if (i == parts.lastIndex && parts.size < 4) AddressParse.Incomplete
                else if (i == parts.lastIndex) AddressParse.Incomplete
                else AddressParse.Error("Octet ${i + 1} vide")
            }
            if (p.length > 3) return AddressParse.Error("Octet ${i + 1} trop long")
            val n = p.toInt()
            if (n > 255) return AddressParse.Error("Octet ${i + 1} hors plage (0-255)")
            value = (value shl 8) or n.toLong()
        }
        if (parts.size < 4) return AddressParse.Incomplete
        return AddressParse.Ok(value)
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'

    fun hostCount(prefix: Int): Long = when (prefix) {
        32 -> 1
        31 -> 2
        else -> (1L shl (32 - prefix)) - 2
    }

    fun addressCount(prefix: Int): Long = 1L shl (32 - prefix)

    fun networkOf(ip: Long, prefix: Int): Long = ip and prefixToMask(prefix)

    fun broadcastOf(ip: Long, prefix: Int): Long = networkOf(ip, prefix) or (prefixToMask(prefix).inv() and MAX)

    fun classLetter(ip: Long): Char {
        val a = ((ip ushr 24) and 255).toInt()
        return when {
            a < 128 -> 'A'
            a < 192 -> 'B'
            a < 224 -> 'C'
            a < 240 -> 'D'
            else -> 'E'
        }
    }

    /** Classe par défaut (classful) d'une adresse, utilisée quand l'utilisateur ne donne pas de préfixe. */
    fun classfulPrefix(ip: Long): Int = when (classLetter(ip)) {
        'A' -> 8
        'B' -> 16
        'C' -> 24
        else -> 32
    }

    fun reverseZone(network: Long, prefix: Int): String {
        val o = octets(network)
        val significant = when {
            prefix >= 24 -> 3
            prefix >= 16 -> 2
            prefix >= 8 -> 1
            else -> 0
        }
        if (significant == 0) return "in-addr.arpa"
        return (significant - 1 downTo 0).joinToString(".") { o[it].toString() } + ".in-addr.arpa"
    }

    /** Découpe une plage [start, end] en blocs CIDR alignés, du plus bas au plus haut. */
    fun rangeToCidrs(start: Long, end: Long): List<Pair<Long, Int>> {
        val out = mutableListOf<Pair<Long, Int>>()
        var cur = start
        while (cur <= end) {
            var size = 32
            while (size > 0) {
                val bigger = size - 1
                val mask = prefixToMask(bigger)
                if ((cur and mask) != cur) break
                if (cur + addressCount(bigger) - 1 > end) break
                size = bigger
            }
            out += cur to size
            cur += addressCount(size)
        }
        return out
    }
}

/** Résultat complet d'un calcul IPv4. */
data class Ipv4Result(
    val ip: Long,
    val prefix: Int,
) {
    val mask: Long = Ipv4.prefixToMask(prefix)
    val wildcard: Long = mask.inv() and Ipv4.MAX
    val network: Long = ip and mask
    val broadcast: Long = network or wildcard
    val first: Long = if (prefix >= 31) network else network + 1
    val last: Long = if (prefix >= 31) broadcast else broadcast - 1
    val hosts: Long = Ipv4.hostCount(prefix)
    val total: Long = Ipv4.addressCount(prefix)
    val classLetter: Char = Ipv4.classLetter(ip)
    val category: AddressCategory = Ipv4Registry.classify(ip)
    val isNetworkAddress: Boolean = prefix <= 30 && ip == network
    val isBroadcastAddress: Boolean = prefix <= 30 && ip == broadcast
    val reverseZone: String = Ipv4.reverseZone(network, prefix)
    val networkCidr: String get() = "${Ipv4.format(network)}/$prefix"
}

/** Résultat de l'analyse du champ de saisie IPv4. */
sealed class Ipv4Input {
    /** Saisie vide ou en cours (pas encore d'erreur à afficher). */
    data class Incomplete(val hint: String) : Ipv4Input()
    data class Error(val message: String) : Ipv4Input()
    /** prefix == null : l'utilisateur n'a pas indiqué de préfixe. */
    data class Ok(val ip: Long, val prefix: Int?, val ipText: String) : Ipv4Input()
}

object Ipv4Parser {

    /**
     * Saisie tolérante :
     *  192.168.1.10            → préfixe non précisé
     *  192.168.1.10/24         → préfixe CIDR
     *  192.168.1.10/255.255.255.0, "192.168.1.10 255.255.255.0", "192.168.1.10 24", "192.168.1.10, 24"
     *  espaces superflus, crochets, retours à la ligne (collage) ignorés.
     * Les octets doivent être 0-255 et les masques contigus.
     */
    fun parse(raw: String): Ipv4Input {
        val text = raw.trim().lineSequence().firstOrNull()?.trim().orEmpty()
            .removePrefix("[").removeSuffix("]")
        if (text.isEmpty()) return Ipv4Input.Incomplete("Saisis une adresse, ex. 192.168.1.10/24")

        val sepIndex = text.indexOfFirst { it == '/' || it == ' ' || it == ',' || it == '\t' || it == ';' }
        val ipText = if (sepIndex < 0) text else text.substring(0, sepIndex).trim()
        val rest = if (sepIndex < 0) null else text.substring(sepIndex + 1).trim { it == ' ' || it == '/' || it == ',' || it == '\t' || it == ';' }

        val ip = when (val a = Ipv4.parseAddressDetailed(ipText)) {
            is Ipv4.AddressParse.Ok -> a.value
            is Ipv4.AddressParse.Error -> return Ipv4Input.Error(a.message)
            Ipv4.AddressParse.Incomplete ->
                return if (rest != null) Ipv4Input.Error("Adresse incomplète") else Ipv4Input.Incomplete("Adresse en cours de saisie…")
        }
        if (rest == null) return Ipv4Input.Ok(ip, null, ipText)
        if (rest.isEmpty()) return Ipv4Input.Incomplete("Indique un préfixe (0-32) ou un masque")

        if (rest.contains('.')) {
            return when (val m = Ipv4.parseAddressDetailed(rest)) {
                is Ipv4.AddressParse.Ok -> {
                    val p = Ipv4.maskToPrefix(m.value)
                        ?: return Ipv4Input.Error("Masque non contigu : seuls les masques du type 255.255.255.0 sont acceptés")
                    Ipv4Input.Ok(ip, p, ipText)
                }
                is Ipv4.AddressParse.Error -> Ipv4Input.Error("Masque : " + m.message.replaceFirstChar { it.lowercase() })
                Ipv4.AddressParse.Incomplete -> Ipv4Input.Incomplete("Masque en cours de saisie…")
            }
        }
        if (rest.any { it !in '0'..'9' }) return Ipv4Input.Error("Préfixe invalide")
        if (rest.length > 2) return Ipv4Input.Error("Préfixe hors plage (0-32)")
        val p = rest.toInt()
        if (p > 32) return Ipv4Input.Error("Préfixe hors plage (0-32)")
        return Ipv4Input.Ok(ip, p, ipText)
    }
}

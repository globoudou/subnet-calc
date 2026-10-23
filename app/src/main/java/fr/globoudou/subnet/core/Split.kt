package fr.globoudou.subnet.core

data class Need(val name: String, val hosts: Long)

data class Allocation(
    val needIndex: Int,
    val name: String,
    val requested: Long,
    val network: Long,
    val prefix: Int,
) {
    val broadcast: Long get() = Ipv4.broadcastOf(network, prefix)
    val first: Long get() = if (prefix >= 31) network else network + 1
    val last: Long get() = if (prefix >= 31) broadcast else broadcast - 1
    val usable: Long get() = Ipv4.hostCount(prefix)
    val size: Long get() = Ipv4.addressCount(prefix)
}

sealed class VlsmResult {
    data class Ok(
        val base: Long,
        val basePrefix: Int,
        /** dans l'ordre de saisie des besoins */
        val allocations: List<Allocation>,
        /** blocs libres restants, triés par adresse */
        val free: List<Pair<Long, Int>>,
    ) : VlsmResult() {
        val used: Long get() = allocations.sumOf { it.size }
        val total: Long get() = Ipv4.addressCount(basePrefix)
    }

    data class Error(val message: String) : VlsmResult()
}

object Splitter {

    /** Plus petit préfixe offrant au moins [hosts] adresses d'hôtes utilisables (bloc minimum /30). */
    fun prefixForHosts(hosts: Long): Int? {
        if (hosts < 1) return null
        for (p in 30 downTo 0) {
            if (Ipv4.hostCount(p) >= hosts) return p
        }
        return null
    }

    fun vlsm(base: Long, basePrefix: Int, needs: List<Need>): VlsmResult {
        val net = Ipv4.networkOf(base, basePrefix)
        val end = Ipv4.broadcastOf(net, basePrefix)
        if (needs.isEmpty()) return VlsmResult.Ok(net, basePrefix, emptyList(), listOf(net to basePrefix))

        val sized = needs.mapIndexed { i, n ->
            val p = prefixForHosts(n.hosts)
                ?: return VlsmResult.Error(if (n.hosts < 1) "« ${n.name.ifBlank { "Besoin ${i + 1}" }} » : au moins 1 hôte" else "« ${n.name} » : trop d'hôtes demandés")
            Triple(i, n, p)
        }
        // du plus grand au plus petit : garantit l'alignement de chaque bloc
        val order = sized.sortedWith(compareBy<Triple<Int, Need, Int>> { it.third }.thenBy { it.first })
        val needed = order.sumOf { Ipv4.addressCount(it.third) }
        val available = Ipv4.addressCount(basePrefix)
        if (needed > available) {
            return VlsmResult.Error("Espace insuffisant : ${Fmt.int(needed)} adresses nécessaires pour ${Fmt.int(available)} disponibles dans le /$basePrefix")
        }
        var cursor = net
        val allocs = ArrayList<Allocation>()
        for ((i, n, p) in order) {
            allocs += Allocation(i, n.name, n.hosts, cursor, p)
            cursor += Ipv4.addressCount(p)
        }
        val free = if (cursor <= end) Ipv4.rangeToCidrs(cursor, end) else emptyList()
        return VlsmResult.Ok(net, basePrefix, allocs.sortedBy { it.needIndex }, free)
    }

    /** Nouveau préfixe pour découper en au moins [parts] sous-réseaux égaux, ou null si impossible. */
    fun prefixForParts(basePrefix: Int, parts: Int): Int? {
        if (parts < 1) return null
        var bits = 0
        while ((1L shl bits) < parts) bits++
        val p = basePrefix + bits
        return if (p > 32) null else p
    }

    /** Sous-réseau n°[index] quand on découpe base/basePrefix en /newPrefix. */
    fun subnetAt(base: Long, basePrefix: Int, newPrefix: Int, index: Long): Long =
        Ipv4.networkOf(base, basePrefix) + index * Ipv4.addressCount(newPrefix)

    fun subnetCount(basePrefix: Int, newPrefix: Int): Long = 1L shl (newPrefix - basePrefix)
}

/**
 * Échelle non linéaire du curseur de préfixe : chaque octet occupe une part croissante
 * de la course, pour être plus précis sur le dernier octet (/24 → /32).
 */
object PrefixScale {
    /** Poids d'un pas entre p-1 et p, pour p dans 1..32. */
    fun weight(p: Int): Float = when (p) {
        in 1..8 -> 1f
        in 9..16 -> 1.5f
        in 17..24 -> 2.5f
        else -> 4f
    }

    private val cumulative: FloatArray = FloatArray(33).also { arr ->
        for (p in 1..32) arr[p] = arr[p - 1] + weight(p)
    }

    private val total = cumulative[32]

    /** Position 0..1 sur la piste pour le préfixe [p]. */
    fun position(p: Int): Float = cumulative[p.coerceIn(0, 32)] / total

    /** Préfixe le plus proche d'une position 0..1. */
    fun prefixAt(fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f) * total
        var best = 0
        var bestDist = Float.MAX_VALUE
        for (p in 0..32) {
            val d = kotlin.math.abs(cumulative[p] - f)
            if (d < bestDist) { bestDist = d; best = p }
        }
        return best
    }
}

/** Mise en forme des nombres à la française. */
object Fmt {
    fun int(n: Long): String {
        val s = kotlin.math.abs(n).toString()
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
            sb.append(c)
        }
        return (if (n < 0) "-" else "") + sb
    }

    fun big(n: java.math.BigInteger): String {
        if (n.bitLength() <= 62) return int(n.toLong())
        val exp = n.bitLength() - 1
        val isPow2 = n.bitCount() == 1
        val digits = n.toString()
        val mantissa = digits[0] + "," + digits.substring(1, 3)
        val sci = "$mantissa × 10${superscript(digits.length - 1)}"
        return if (isPow2) "2${superscript(exp)} ≈ $sci" else "≈ $sci"
    }

    fun superscript(n: Int): String {
        val map = "⁰¹²³⁴⁵⁶⁷⁸⁹"
        return n.toString().map { map[it - '0'] }.joinToString("")
    }
}

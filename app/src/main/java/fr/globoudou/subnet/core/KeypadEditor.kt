package fr.globoudou.subnet.core

/**
 * Logique du clavier IPv4 : empêche les saisies impossibles et insère
 * automatiquement le point quand un octet est complet (3 chiffres ou > 255).
 */
object KeypadEditor {

    fun type(text: String, ch: Char): String {
        val slash = text.indexOf('/')
        val inPrefix = slash >= 0
        val addr = if (inPrefix) text.substring(0, slash) else text
        val pre = if (inPrefix) text.substring(slash + 1) else ""
        return when (ch) {
            in '0'..'9' -> when {
                !inPrefix -> dotted(addr, ch)?.let { text + it } ?: text
                pre.contains('.') -> dotted(pre, ch)?.let { text + it } ?: text
                else -> {
                    val cand = pre + ch
                    // 1-2 chiffres ≤ 32 : préfixe ; 3 chiffres ≤ 255 : début de masque
                    if ((cand.length <= 2 && cand.toInt() <= 32) || (cand.length == 3 && cand.toInt() <= 255)) text + ch
                    else text
                }
            }
            '.' -> {
                val part = if (inPrefix) pre else addr
                if (part.isEmpty() || part.last() == '.' || part.count { it == '.' } >= 3) text else "$text."
            }
            '/' -> if (inPrefix || addr.count { it == '.' } != 3 || addr.endsWith('.')) text else "$text/"
            else -> text
        }
    }

    /** Suffixe à ajouter pour taper [ch] dans une notation pointée, ou null si impossible. */
    private fun dotted(part: String, ch: Char): String? {
        val dots = part.count { it == '.' }
        val seg = part.substringAfterLast('.')
        val full = seg.length >= 3 || (seg.isNotEmpty() && (seg + ch).toInt() > 255)
        return when {
            !full -> ch.toString()
            dots < 3 -> ".$ch"
            else -> null
        }
    }

    fun backspace(text: String): String = text.dropLast(1)
}

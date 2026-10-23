package fr.globoudou.subnet.data

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.net.Inet4Address
import java.net.Inet6Address

private const val PREFS = "subnet"

/** Historique compact (25 entrées max, la plus récente en tête). */
class HistoryStore(context: Context, private val key: String) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var items: List<String> by mutableStateOf(load())
        private set

    private fun load(): List<String> =
        prefs.getString(key, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    fun add(entry: String) {
        if (items.firstOrNull() == entry) return
        items = (listOf(entry) + items.filter { it != entry }).take(25)
        prefs.edit().putString(key, items.joinToString("\n")).apply()
    }

    fun clear() {
        items = emptyList()
        prefs.edit().remove(key).apply()
    }
}

@Composable
fun rememberHistory(key: String): HistoryStore {
    val ctx = LocalContext.current.applicationContext
    return remember(key) { HistoryStore(ctx, key) }
}

/** Petit réglage booléen persistant (ex. vue binaire dépliée). */
class Flag(context: Context, private val key: String, default: Boolean) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    var value: Boolean by mutableStateOf(prefs.getBoolean(key, default))
        private set

    fun toggle() {
        value = !value
        prefs.edit().putBoolean(key, value).apply()
    }
}

@Composable
fun rememberFlag(key: String, default: Boolean): Flag {
    val ctx = LocalContext.current.applicationContext
    return remember(key) { Flag(ctx, key, default) }
}

/** Adresse de l'appareil sur le réseau actif (Wi-Fi, mobile, Ethernet…), au format adresse/préfixe. */
object LocalAddress {
    fun current(context: Context, v6: Boolean): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = cm.activeNetwork ?: return null
        val links = cm.getLinkProperties(network)?.linkAddresses ?: return null
        return if (!v6) {
            links.firstOrNull { it.address is Inet4Address }?.let { "${it.address.hostAddress}/${it.prefixLength}" }
        } else {
            val v6s = links.filter { it.address is Inet6Address }
            val best = v6s.firstOrNull { !it.address.isLinkLocalAddress && !it.address.isSiteLocalAddress }
                ?: v6s.firstOrNull()
            best?.let { "${it.address.hostAddress?.substringBefore('%')}/${it.prefixLength}" }
        }
    }
}

package fr.globoudou.subnet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.globoudou.subnet.core.Fmt
import fr.globoudou.subnet.core.Ipv4
import fr.globoudou.subnet.core.Ipv4Input
import fr.globoudou.subnet.core.Ipv4Parser
import fr.globoudou.subnet.core.Ipv4Result
import fr.globoudou.subnet.core.KeypadEditor
import fr.globoudou.subnet.data.LocalAddress
import fr.globoudou.subnet.data.rememberFlag
import fr.globoudou.subnet.data.rememberHistory
import kotlinx.coroutines.delay

@Composable
fun Ipv4Screen(onSplit: (String) -> Unit) {
    val context = LocalContext.current
    val clip = rememberClip()
    val history = rememberHistory("history_v4")
    val binary = rememberFlag("binary_expanded", false)

    var text by rememberSaveable { mutableStateOf("192.168.1.10/24") }
    var fallbackPrefix by rememberSaveable { mutableIntStateOf(24) }
    var keypadVisible by rememberSaveable { mutableStateOf(true) }
    var showHistory by remember { mutableStateOf(false) }

    val input = remember(text) { Ipv4Parser.parse(text) }
    val ok = input as? Ipv4Input.Ok
    val prefix = ok?.prefix ?: fallbackPrefix

    // Dernier résultat valide : reste affiché (atténué) pendant la saisie
    var lastIp by rememberSaveable { mutableStateOf(Ipv4.parseAddress("192.168.1.10")!!) }
    val shownIp = ok?.ip ?: lastIp
    LaunchedEffect(ok?.ip) { ok?.ip?.let { lastIp = it } }
    val result = remember(shownIp, prefix) { Ipv4Result(shownIp, prefix) }
    val stale = ok == null

    fun setPrefix(p: Int) {
        fallbackPrefix = p
        if (ok != null) text = "${ok.ipText}/$p"
    }

    // Historique : on enregistre un calcul resté stable 1,5 s
    val canonical = ok?.let { "${Ipv4.format(it.ip)}/$prefix" }
    LaunchedEffect(canonical) {
        if (canonical != null) {
            delay(1500)
            history.add(canonical)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("IPv4") {
            BarIcon(AppIcons.History, "Historique") { showHistory = true }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputCard(
                text = text,
                input = input,
                keypadVisible = keypadVisible,
                onToggleKeypad = { keypadVisible = !keypadVisible },
                onClear = { text = "" },
            )
            PrefixCard(prefix = prefix, result = result, implicit = ok != null && ok.prefix == null, onPrefix = ::setPrefix)

            Column(Modifier.alpha(if (stale) 0.45f else 1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryBlock(result.category, extraTags(result))
                HeroCard(
                    label = "Réseau",
                    value = result.networkCidr,
                    onCopy = { clip.copy("Réseau", result.networkCidr) },
                    action = if (prefix < 30) ({ onSplit(result.networkCidr) }) else null,
                )
                TileGrid(
                    listOf(
                        "Première adresse" to Ipv4.format(result.first),
                        "Dernière adresse" to Ipv4.format(result.last),
                        "Broadcast" to Ipv4.format(result.broadcast),
                        "Hôtes utilisables" to Fmt.int(result.hosts),
                        "Masque" to Ipv4.format(result.mask),
                        "Wildcard" to Ipv4.format(result.wildcard),
                    ),
                ) { label, value -> clip.copy(label, value.replace(" ", "")) }
                ValueTile("Zone DNS inverse", result.reverseZone, Modifier.fillMaxWidth(), valueSize = 14) {
                    clip.copy("Zone inverse", result.reverseZone)
                }
                BinaryCard(Ipv4.octets(result.ip), prefix, binary.value) { binary.toggle() }
            }
        }
        AnimatedVisibility(keypadVisible, enter = expandVertically(), exit = shrinkVertically()) {
            Ipv4Keypad(
                onChar = { text = KeypadEditor.type(text, it) },
                onBackspace = { text = KeypadEditor.backspace(text) },
                onClear = { text = "" },
                onPaste = {
                    val p = clip.paste()?.trim()
                    if (p.isNullOrEmpty()) clip.toast("Presse-papiers vide") else text = p.take(40)
                },
                onMyIp = {
                    val mine = LocalAddress.current(context, v6 = false)
                    if (mine == null) clip.toast("Aucune adresse IPv4 active") else text = mine
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
            )
        }
    }

    if (showHistory) {
        HistorySheet(
            items = history.items,
            describe = { e -> (Ipv4Parser.parse(e) as? Ipv4Input.Ok)?.let { Ipv4Result(it.ip, it.prefix ?: 32).category.label } },
            onPick = { text = it; showHistory = false },
            onClear = { history.clear() },
            onDismiss = { showHistory = false },
        )
    }
}

private fun extraTags(r: Ipv4Result): List<Pair<String, Boolean>> = buildList {
    if (r.classLetter <= 'C') add("Classe ${r.classLetter}" to false)
    when (r.prefix) {
        31 -> add("Point à point · RFC 3021" to false)
        32 -> add("Hôte unique" to false)
    }
    if (r.isNetworkAddress) add("Adresse de réseau" to true)
    if (r.isBroadcastAddress) add("Adresse de broadcast" to true)
}

@Composable
private fun InputCard(
    text: String,
    input: Ipv4Input,
    keypadVisible: Boolean,
    onToggleKeypad: () -> Unit,
    onClear: () -> Unit,
) {
    val caret = rememberInfiniteTransition(label = "caret")
    val caretAlpha by caret.animateFloat(1f, 0f, infiniteRepeatable(tween(530), RepeatMode.Reverse), label = "caretAlpha")
    val net = Subnet.colors.net
    SectionCard(
        Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "Afficher le clavier") {
            if (!keypadVisible) onToggleKeypad()
        },
    ) {
        Overline("Adresse / préfixe ou masque")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                val slash = text.indexOf('/')
                val addr = if (slash >= 0) text.substring(0, slash) else text
                val pre = if (slash >= 0) text.substring(slash) else ""
                if (text.isEmpty()) {
                    Text("0.0.0.0/0", fontFamily = Mono, fontSize = 26.sp, color = MaterialTheme.colorScheme.outline)
                } else {
                    Text(addr, fontFamily = Mono, fontSize = if (text.length > 18) 20.sp else 26.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text(pre, fontFamily = Mono, fontSize = if (text.length > 18) 20.sp else 26.sp, fontWeight = FontWeight.Medium, color = net, maxLines = 1)
                }
                if (keypadVisible) {
                    Box(Modifier.padding(start = 2.dp).width(2.dp).height(30.dp).alpha(caretAlpha).background(net))
                }
            }
            IconButton(onClick = onToggleKeypad) {
                Icon(
                    if (keypadVisible) AppIcons.ChevronDown else AppIcons.Keyboard,
                    contentDescription = if (keypadVisible) "Masquer le clavier" else "Afficher le clavier",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(AppIcons.Close, contentDescription = "Tout effacer", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
        when (input) {
            is Ipv4Input.Error -> Text(input.message, fontSize = 13.sp, color = Subnet.colors.error)
            is Ipv4Input.Incomplete -> Text(input.hint, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            is Ipv4Input.Ok -> Text(
                if (input.prefix == null) "Préfixe non saisi : réglé au curseur" else "Formats acceptés : /24, masque 255.255.255.0, espace…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrefixCard(prefix: Int, result: Ipv4Result, implicit: Boolean, onPrefix: (Int) -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(AppIcons.Minus, "Préfixe moins un", { if (prefix > 0) onPrefix(prefix - 1) })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("/$prefix", fontFamily = Mono, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, color = Subnet.colors.net)
                Text(Ipv4.format(result.mask), fontFamily = Mono, fontSize = 14.sp)
                Text(
                    "${Fmt.int(result.hosts)} hôte${if (result.hosts > 1) "s" else ""} · ${Fmt.int(result.total)} adresse${if (result.total > 1) "s" else ""}" +
                        if (implicit) " · réglé au curseur" else "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoundButton(AppIcons.Plus, "Préfixe plus un", { if (prefix < 32) onPrefix(prefix + 1) })
        }
        PrefixSlider(prefix, onPrefix)
    }
}

@Composable
fun HeroCard(label: String, value: String, onCopy: () -> Unit, action: (() -> Unit)?, valueSize: Int = 22) {
    Surface(shape = RoundedCornerShape(20.dp), color = Subnet.colors.hero, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = if (action != null) 4.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Overline(label)
                    Text(value, fontFamily = Mono, fontSize = valueSize.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onCopy) {
                    Icon(AppIcons.Copy, contentDescription = "Copier", tint = Subnet.colors.onNetContainer, modifier = Modifier.size(20.dp))
                }
            }
            if (action != null) {
                TextButton(onClick = action, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(AppIcons.Split, null, Modifier.size(16.dp))
                    Text("  Découper ce réseau")
                }
            }
        }
    }
}

@Composable
fun TileGrid(items: List<Pair<String, String>>, onCopy: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    ValueTile(label, value, Modifier.weight(1f)) { onCopy(label, value) }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

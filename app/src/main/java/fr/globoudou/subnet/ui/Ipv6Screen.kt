package fr.globoudou.subnet.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.globoudou.subnet.core.Fmt
import fr.globoudou.subnet.core.Ipv6
import fr.globoudou.subnet.core.Ipv6Input
import fr.globoudou.subnet.core.Ipv6Parser
import fr.globoudou.subnet.core.Ipv6Result
import fr.globoudou.subnet.data.LocalAddress
import fr.globoudou.subnet.data.rememberHistory
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Ipv6Screen() {
    val context = LocalContext.current
    val view = LocalView.current
    val focus = LocalFocusManager.current
    val clip = rememberClip()
    val history = rememberHistory("history_v6")

    var text by rememberSaveable { mutableStateOf("2001:db8:abcd:12::1/64") }
    var fallbackPrefix by rememberSaveable { mutableIntStateOf(64) }
    var lastIp by rememberSaveable { mutableStateOf("2001:db8:abcd:12::1") }
    var showHistory by remember { mutableStateOf(false) }

    val input = remember(text) { Ipv6Parser.parse(text) }
    val ok = input as? Ipv6Input.Ok
    val prefix = ok?.prefix ?: fallbackPrefix
    val shownIp = ok?.ip ?: Ipv6.parseAddress(lastIp)!!
    LaunchedEffect(ok?.ip) { ok?.ip?.let { lastIp = Ipv6.compress(it) } }
    val result = remember(shownIp, prefix) { Ipv6Result(shownIp, prefix) }

    fun setPrefix(p: Int) {
        val c = p.coerceIn(0, 128)
        if (c != prefix) view.performHapticFeedback(if (c % 16 == 0) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.CLOCK_TICK)
        fallbackPrefix = c
        if (ok != null) text = "${ok.ipText}/$c"
    }

    val canonical = ok?.let { "${Ipv6.compress(it.ip)}/$prefix" }
    LaunchedEffect(canonical) {
        if (canonical != null) {
            delay(1500)
            history.add(canonical)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("IPv6") {
            BarIcon(AppIcons.Wifi, "Mon adresse IPv6") {
                val mine = LocalAddress.current(context, v6 = true)
                if (mine == null) clip.toast("Aucune adresse IPv6 active") else text = mine
            }
            BarIcon(AppIcons.History, "Historique") { showHistory = true }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(60) },
                label = { Text("Adresse IPv6 / préfixe") },
                textStyle = TextStyle(fontFamily = Mono, fontSize = 17.sp),
                isError = input is Ipv6Input.Error,
                supportingText = {
                    when (input) {
                        is Ipv6Input.Error -> Text(input.message)
                        is Ipv6Input.Incomplete -> Text(input.hint)
                        is Ipv6Input.Ok -> Text(if (input.prefix == null) "Préfixe non saisi : réglé ci-dessous" else "Formes compressée, complète, [crochets], %zone acceptées")
                    }
                },
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val p = clip.paste()?.trim()
                            if (p.isNullOrEmpty()) clip.toast("Presse-papiers vide") else text = p.take(60)
                        }) { Icon(AppIcons.Paste, "Coller", Modifier.size(20.dp)) }
                        if (text.isNotEmpty()) IconButton(onClick = { text = "" }) { Icon(AppIcons.Close, "Effacer", Modifier.size(18.dp)) }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundButton(AppIcons.Minus, "Préfixe moins un", { setPrefix(prefix - 1) })
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("/$prefix", fontFamily = Mono, fontSize = 40.sp, fontWeight = FontWeight.SemiBold, color = Subnet.colors.net)
                        Text(
                            result.subnets64?.let { "${Fmt.big(it)} sous-réseau${if (it > java.math.BigInteger.ONE) "x" else ""} /64" }
                                ?: "${Fmt.big(result.count)} adresse${if (prefix < 128) "s" else ""}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RoundButton(AppIcons.Plus, "Préfixe plus un", { setPrefix(prefix + 1) })
                }
                Slider(
                    value = prefix.toFloat(),
                    onValueChange = { setPrefix(it.roundToInt()) },
                    valueRange = 0f..128f,
                    colors = SliderDefaults.colors(thumbColor = Subnet.colors.net, activeTrackColor = Subnet.colors.net),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in listOf(32, 48, 56, 64, 128)) {
                        FilterChip(selected = p == prefix, onClick = { setPrefix(p) }, label = { Text("/$p", fontFamily = Mono) })
                    }
                }
            }

            Column(Modifier.alpha(if (ok == null) 0.45f else 1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryBlock(result.category)
                HeroCard(
                    label = "Réseau",
                    value = result.networkCidr,
                    valueSize = if (result.networkCidr.length > 24) 17 else 20,
                    onCopy = { clip.copy("Réseau", result.networkCidr) },
                    action = null,
                )
                val rows = listOf(
                    "Adresse (compressée)" to Ipv6.compress(result.ip),
                    "Adresse (développée)" to Ipv6.expand(result.ip),
                    "Première adresse" to Ipv6.compress(result.network),
                    "Dernière adresse" to Ipv6.compress(result.last),
                    "Nombre d'adresses" to Fmt.big(result.count),
                    "Zone DNS inverse" to result.reverseZone,
                )
                rows.forEach { (label, value) ->
                    ValueTile(label, value, Modifier.fillMaxWidth(), valueSize = if (value.length > 32) 13 else 15) {
                        clip.copy(label, value.replace(" ", ""))
                    }
                }
            }
        }
    }

    if (showHistory) {
        HistorySheet(
            items = history.items,
            describe = { e -> (Ipv6Parser.parse(e) as? Ipv6Input.Ok)?.let { Ipv6Result(it.ip, it.prefix ?: 128).category.label } },
            onPick = { text = it; showHistory = false },
            onClear = { history.clear() },
            onDismiss = { showHistory = false },
        )
    }
}

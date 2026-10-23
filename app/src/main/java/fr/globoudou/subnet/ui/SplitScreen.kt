package fr.globoudou.subnet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.globoudou.subnet.core.Allocation
import fr.globoudou.subnet.core.Fmt
import fr.globoudou.subnet.core.Ipv4
import fr.globoudou.subnet.core.Ipv4Input
import fr.globoudou.subnet.core.Ipv4Parser
import fr.globoudou.subnet.core.Need
import fr.globoudou.subnet.core.Splitter
import fr.globoudou.subnet.core.VlsmResult

data class NeedRow(val name: String, val hosts: String)

private val needsSaver = listSaver<SnapshotStateList<NeedRow>, String>(
    save = { list -> list.flatMap { listOf(it.name, it.hosts) } },
    restore = { flat -> flat.chunked(2).map { NeedRow(it[0], it[1]) }.toMutableStateList() },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(base: String, onBaseChange: (String) -> Unit) {
    val clip = rememberClip()
    var mode by rememberSaveable { mutableIntStateOf(1) }
    val needs = rememberSaveable(saver = needsSaver) {
        mutableStateListOf(NeedRow("LAN", "100"), NeedRow("Wi-Fi", "50"), NeedRow("DMZ", "20"), NeedRow("Admin", "2"))
    }

    val parsed = remember(base) { Ipv4Parser.parse(base) }
    val ok = parsed as? Ipv4Input.Ok
    val basePrefix = ok?.let { it.prefix ?: Ipv4.classfulPrefix(it.ip) }
    val baseNet = ok?.let { Ipv4.networkOf(it.ip, basePrefix!!) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Découpage") {
            if (mode == 1) BarIcon(AppIcons.Copy, "Copier le plan d'adressage") {
                if (ok != null && baseNet != null && basePrefix != null) {
                    val txt = vlsmText(baseNet, basePrefix, needs)
                    if (txt.isNotEmpty()) clip.copy("Plan d'adressage", txt)
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = base,
                onValueChange = { onBaseChange(it.take(40)) },
                label = { Text("Réseau à découper") },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Mono, fontSize = 18.sp),
                isError = parsed is Ipv4Input.Error,
                supportingText = {
                    when {
                        parsed is Ipv4Input.Error -> Text(parsed.message)
                        parsed is Ipv4Input.Incomplete -> Text(parsed.hint)
                        ok != null && baseNet != null && basePrefix != null ->
                            Text("${Ipv4.format(baseNet)}/$basePrefix · ${Fmt.int(Ipv4.addressCount(basePrefix))} adresses" + if (ok.prefix == null) " (préfixe de classe)" else "")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Parts égales") }
                SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Besoins (VLSM)") }
            }

            if (baseNet != null && basePrefix != null) {
                if (mode == 1) VlsmSection(baseNet, basePrefix, needs, clip)
                else PartsSection(baseNet, basePrefix, clip)
            }
        }
    }
}

private fun parseNeeds(rows: List<NeedRow>): Pair<List<Int>, List<Need>> {
    val idx = rows.indices.filter { (rows[it].hosts.toLongOrNull() ?: 0) > 0 }
    return idx to idx.map { Need(rows[it].name.ifBlank { "Réseau ${it + 1}" }, rows[it].hosts.toLong()) }
}

private fun vlsmText(net: Long, prefix: Int, rows: List<NeedRow>): String {
    val (_, needs) = parseNeeds(rows)
    val r = Splitter.vlsm(net, prefix, needs) as? VlsmResult.Ok ?: return ""
    return buildString {
        appendLine("Plan VLSM de ${Ipv4.format(net)}/$prefix")
        r.allocations.forEach { a ->
            appendLine("${a.name} (${a.requested} hôtes) : ${Ipv4.format(a.network)}/${a.prefix}  ${Ipv4.format(a.first)} - ${Ipv4.format(a.last)}  bcast ${Ipv4.format(a.broadcast)}")
        }
        if (r.free.isNotEmpty()) appendLine("Libre : " + r.free.joinToString(", ") { "${Ipv4.format(it.first)}/${it.second}" })
    }.trim()
}

private fun shortRange(first: Long, last: Long, bcast: Long, prefix: Int): String =
    if (prefix >= 24) ".${first and 255} → .${last and 255}" + (if (prefix <= 30) " · bcast .${bcast and 255}" else "")
    else "${Ipv4.format(first)} → ${Ipv4.format(last)}"

@Composable
private fun VlsmSection(net: Long, prefix: Int, rows: SnapshotStateList<NeedRow>, clip: Clip) {
    val palette = Subnet.colors.segment
    val (validIdx, needs) = parseNeeds(rows)
    val result = remember(net, prefix, needs) { Splitter.vlsm(net, prefix, needs) }
    val ok = result as? VlsmResult.Ok
    // allocation par index de ligne
    val byRow: Map<Int, Allocation> = ok?.allocations?.associateBy { validIdx[it.needIndex] } ?: emptyMap()

    if (ok != null) OccupancyBar(ok, validIdx, palette)
    if (result is VlsmResult.Error) {
        Surface(shape = RoundedCornerShape(14.dp), color = Subnet.colors.hostContainer, modifier = Modifier.fillMaxWidth()) {
            Text(result.message, Modifier.padding(14.dp), color = Subnet.colors.onHostContainer, fontSize = 14.sp)
        }
    }

    rows.forEachIndexed { i, row ->
        val color = palette[i % palette.size]
        val alloc = byRow[i]
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { rows[i] = row.copy(name = it.take(24)) },
                    singleLine = true,
                    placeholder = { Text("Nom") },
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = row.hosts,
                    onValueChange = { v -> rows[i] = row.copy(hosts = v.filter { it.isDigit() }.take(10)) },
                    singleLine = true,
                    placeholder = { Text("hôtes") },
                    suffix = { Text("h", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    textStyle = TextStyle(fontFamily = Mono, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(110.dp),
                )
                IconButton(onClick = { rows.removeAt(i) }) {
                    Icon(AppIcons.Trash, contentDescription = "Supprimer ${row.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            if (alloc != null) {
                val cidr = "${Ipv4.format(alloc.network)}/${alloc.prefix}"
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { clip.copy(row.name.ifBlank { "Sous-réseau" }, cidr) }.padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(cidr, fontFamily = Mono, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("${Fmt.int(alloc.usable)} dispo", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(shortRange(alloc.first, alloc.last, alloc.broadcast, alloc.prefix), fontFamily = Mono, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if ((row.hosts.toLongOrNull() ?: 0) <= 0) {
                Text("Indique le nombre d'hôtes", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    OutlinedButton(
        onClick = { rows.add(NeedRow("", "")) },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(AppIcons.Plus, null, Modifier.size(18.dp))
        Text("  Ajouter un besoin")
    }

    if (ok != null && ok.free.isNotEmpty()) {
        SectionCard {
            Row {
                Overline("Espace libre", Modifier.weight(1f))
                Text("${Fmt.int(ok.total - ok.used)} adr.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ok.free.forEach { (n, p) ->
                val cidr = "${Ipv4.format(n)}/$p"
                Text(
                    cidr,
                    fontFamily = Mono,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { clip.copy("Bloc libre", cidr) }.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun OccupancyBar(r: VlsmResult.Ok, validIdx: List<Int>, palette: List<Color>) {
    val free = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Overline("Occupation · ${Fmt.int(r.used)} / ${Fmt.int(r.total)}", Modifier.weight(1f))
            Text("${Fmt.int(r.total - r.used)} libres", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val segments = r.allocations.map { Triple(it.network, it.size, palette[validIdx[it.needIndex] % palette.size]) } +
                r.free.map { (n, p) -> Triple(n, Ipv4.addressCount(p), free) }
            segments.sortedBy { it.first }.forEach { (_, size, color) ->
                Box(Modifier.weight(size.toFloat() / r.total.toFloat()).fillMaxHeight().background(color))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartsSection(net: Long, basePrefix: Int, clip: Clip) {
    if (basePrefix >= 32) {
        Text("Un /32 ne peut pas être découpé.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var newPrefix by rememberSaveable(basePrefix) { mutableIntStateOf((basePrefix + 2).coerceAtMost(32)) }
    val count = Splitter.subnetCount(basePrefix, newPrefix)

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundButton(AppIcons.Minus, "Moins de sous-réseaux", { if (newPrefix > basePrefix + 1) newPrefix-- })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("/$newPrefix", fontFamily = Mono, fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = Subnet.colors.net)
                Text(
                    "${Fmt.int(count)} sous-réseau${if (count > 1) "x" else ""} de ${Fmt.int(Ipv4.hostCount(newPrefix))} hôte${if (Ipv4.hostCount(newPrefix) > 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoundButton(AppIcons.Plus, "Plus de sous-réseaux", { if (newPrefix < 32) newPrefix++ })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in listOf(2, 4, 8, 16, 32, 64)) {
                val p = Splitter.prefixForParts(basePrefix, n) ?: continue
                FilterChip(selected = p == newPrefix, onClick = { newPrefix = p }, label = { Text("$n parts") })
            }
        }
    }

    val shown = minOf(count, 256L)
    SectionCard {
        for (k in 0 until shown) {
            val n = Splitter.subnetAt(net, basePrefix, newPrefix, k)
            val cidr = "${Ipv4.format(n)}/$newPrefix"
            val b = Ipv4.broadcastOf(n, newPrefix)
            val first = if (newPrefix >= 31) n else n + 1
            val last = if (newPrefix >= 31) b else b - 1
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { clip.copy("Sous-réseau", cidr) }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${k + 1}", fontFamily = Mono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
                Column(Modifier.weight(1f)) {
                    Text(cidr, fontFamily = Mono, fontSize = 15.sp)
                    Text(shortRange(first, last, b, newPrefix), fontFamily = Mono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (count > shown) {
            Text("… et ${Fmt.int(count - shown)} autres", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

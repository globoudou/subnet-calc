package fr.globoudou.subnet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.globoudou.subnet.core.AddressCategory
import fr.globoudou.subnet.core.PrefixScale
import kotlin.math.roundToInt

// ---------------------------------------------------------------- presse-papiers

class Clip(private val context: Context) {
    private val cm = context.getSystemService(ClipboardManager::class.java)

    fun copy(label: String, text: String) {
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13+ affiche déjà sa propre confirmation
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "$label copié", Toast.LENGTH_SHORT).show()
    }

    fun paste(): String? =
        cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

@Composable
fun rememberClip(): Clip {
    val ctx = LocalContext.current
    return remember(ctx) { Clip(ctx) }
}

// ---------------------------------------------------------------- briques

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = color,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier = modifier, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp, color = color)
}

@Composable
fun TopBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun BarIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

/** Valeur copiable d'un appui. */
@Composable
fun ValueTile(label: String, value: String, modifier: Modifier = Modifier, valueSize: Int = 15, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, fontFamily = Mono, fontSize = valueSize.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
        color = content,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
    )
}

/** Étiquettes de nature d'adresse + explication. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryBlock(category: AddressCategory, extra: List<Pair<String, Boolean>> = emptyList()) {
    val (bg, fg) = kindColors(category.kind)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(category.label + (category.rfc?.let { " · $it" } ?: ""), bg, fg)
            for ((label, warn) in extra) {
                if (warn) Pill(label, Subnet.colors.hostContainer, Subnet.colors.onHostContainer)
                else Pill(label, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(category.detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RoundButton(icon: ImageVector, description: String, onClick: () -> Unit, size: Int = 48) {
    val view = LocalView.current
    Surface(
        onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
        shape = RoundedCornerShape(50),
        color = Subnet.colors.key,
        modifier = Modifier.size(size.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, modifier = Modifier.size(20.dp)) }
    }
}

// ---------------------------------------------------------------- curseur de préfixe

/**
 * Curseur de préfixe IPv4 à échelle non linéaire : l'octet d (/24-/32) occupe ~45 % de la course.
 * Retour haptique à chaque cran, plus marqué aux frontières d'octet.
 */
@Composable
fun PrefixSlider(prefix: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val current by rememberUpdatedState(prefix)
    val emit by rememberUpdatedState(onChange)
    val track = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val active = Subnet.colors.net
    val bg = MaterialTheme.colorScheme.surfaceContainer
    val thumbPad = 16.dp

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription = "Longueur du préfixe"
                    progressBarRangeInfo = ProgressBarRangeInfo(prefix.toFloat(), 0f..32f, 31)
                    setProgress { v -> emit(v.roundToInt().coerceIn(0, 32)); true }
                }
                .pointerInput(Unit) {
                    val pad = thumbPad.toPx()
                    fun prefixAt(x: Float): Int = PrefixScale.prefixAt((x - pad) / (size.width - 2 * pad))
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var last = current
                        fun update(x: Float) {
                            val p = prefixAt(x)
                            if (p != last) {
                                last = p
                                view.performHapticFeedback(
                                    if (p % 8 == 0) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.CLOCK_TICK,
                                )
                                emit(p)
                            }
                        }
                        val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        if (start != null) {
                            update(start.position.x)
                            horizontalDrag(start.id) { change ->
                                update(change.position.x)
                                change.consume()
                            }
                        } else {
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (up != null && !up.pressed && !up.isConsumed) update(up.position.x)
                        }
                    }
                }
                .drawBehind {
                    val pad = thumbPad.toPx()
                    val w = size.width - 2 * pad
                    val y = size.height / 2
                    val stroke = 6.dp.toPx()
                    drawLine(track, Offset(pad, y), Offset(pad + w, y), stroke, StrokeCap.Round)
                    val px = pad + w * PrefixScale.position(prefix)
                    drawLine(active, Offset(pad, y), Offset(px, y), stroke, StrokeCap.Round)
                    for (p in 0..32) {
                        val x = pad + w * PrefixScale.position(p)
                        if (p % 8 == 0) {
                            drawLine(tick, Offset(x, y + 9.dp.toPx()), Offset(x, y + 17.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                        } else {
                            drawCircle(tick.copy(alpha = 0.6f), 1.5.dp.toPx(), Offset(x, y + 13.dp.toPx()))
                        }
                    }
                    drawCircle(bg, 14.dp.toPx(), Offset(px, y))
                    drawCircle(active, 10.dp.toPx(), Offset(px, y))
                },
        )
        Box(Modifier.fillMaxWidth().height(18.dp)) {
            for (p in listOf(0, 8, 16, 24, 32)) {
                AnchoredLabel("/$p", PrefixScale.position(p), thumbPad.value, MaterialTheme.colorScheme.onSurfaceVariant, 12)
            }
            val letters = listOf("a", "b", "c", "d")
            for (i in 0..3) {
                val mid = (PrefixScale.position(i * 8) + PrefixScale.position(i * 8 + 8)) / 2
                AnchoredLabel("octet ${letters[i]}", mid, thumbPad.value, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), 10)
            }
        }
    }
}

@Composable
private fun AnchoredLabel(text: String, fraction: Float, padDp: Float, color: Color, size: Int) {
    Text(
        text,
        color = color,
        fontSize = size.sp,
        fontFamily = Mono,
        maxLines = 1,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val width = constraints.maxWidth
            val pad = padDp.dp.roundToPx()
            val cx = (pad + (width - 2 * pad) * fraction).roundToInt()
            layout(width, placeable.height) {
                placeable.place((cx - placeable.width / 2).coerceIn(0, (width - placeable.width).coerceAtLeast(0)), 0)
            }
        },
    )
}

// ---------------------------------------------------------------- vue binaire

@Composable
fun BinaryCard(octets: IntArray, prefix: Int, expanded: Boolean, onToggle: () -> Unit) {
    val c = Subnet.colors
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    SectionCard {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = if (expanded) "Replier" else "Déplier") { onToggle() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Overline("Vue binaire", Modifier.weight(1f))
            Legend(c.net, "Réseau $prefix")
            Spacer(Modifier.width(10.dp))
            Legend(c.host, "Hôtes ${32 - prefix}")
            Icon(AppIcons.ChevronDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp).size(20.dp).rotate(rotation))
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (col in 0..1) {
                            val o = row * 2 + col
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    for (i in 0 until 8) {
                                        val idx = o * 8 + i
                                        val bit = (octets[o] shr (7 - i)) and 1
                                        val isNet = idx < prefix
                                        Box(
                                            Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(5.dp))
                                                .background(if (isNet) c.netContainer else c.hostContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(bit.toString(), fontFamily = Mono, fontSize = 13.sp, color = if (isNet) c.onNetContainer else c.onHostContainer)
                                        }
                                    }
                                }
                                Text(
                                    octets[o].toString(),
                                    Modifier.fillMaxWidth(),
                                    fontFamily = Mono,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ---------------------------------------------------------------- clavier IPv4

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(
    modifier: Modifier,
    container: Color,
    content: Color,
    description: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    inner: @Composable () -> Unit,
) {
    val view = LocalView.current
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .combinedClickable(
                role = Role.Button,
                onClick = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() },
                onLongClick = onLongClick?.let { lc -> { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); lc() } },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) { inner() }
    }
}

@Composable
private fun RowScope.DigitKey(ch: Char, onChar: (Char) -> Unit) =
    Key(Modifier.weight(1f), Subnet.colors.key, MaterialTheme.colorScheme.onSurface, ch.toString(), { onChar(ch) }) {
        Text(ch.toString(), fontFamily = Mono, fontSize = 22.sp)
    }

@Composable
private fun RowScope.AccentKey(ch: Char, desc: String, onChar: (Char) -> Unit) =
    Key(Modifier.weight(1f), Subnet.colors.keyAccent, Subnet.colors.net, desc, { onChar(ch) }) {
        Text(ch.toString(), fontFamily = Mono, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }

@Composable
fun Ipv4Keypad(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onMyIp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Subnet.colors
    val fg = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DigitKey('1', onChar); DigitKey('2', onChar); DigitKey('3', onChar)
            Key(Modifier.weight(1f), c.keyAlt, fg, "Effacer (appui long : tout effacer)", onBackspace, onClear) {
                Icon(AppIcons.Backspace, null, Modifier.size(24.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DigitKey('4', onChar); DigitKey('5', onChar); DigitKey('6', onChar); AccentKey('.', "Point", onChar) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DigitKey('7', onChar); DigitKey('8', onChar); DigitKey('9', onChar); AccentKey('/', "Barre oblique, préfixe", onChar) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Key(Modifier.weight(1f), c.keyAlt, fg, "Coller", onPaste) { Text("Coller", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            DigitKey('0', onChar)
            Key(Modifier.weight(2f), c.keyAlt, fg, "Mon adresse IP", onMyIp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(AppIcons.Wifi, null, Modifier.size(18.dp))
                    Text("Mon IP", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- historique

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    items: List<String>,
    describe: (String) -> String?,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Historique", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) TextButton(onClick = onClear) { Text("Tout effacer") }
        }
        if (items.isEmpty()) {
            Text(
                "Les calculs apparaîtront ici automatiquement.",
                Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.padding(bottom = 24.dp)) {
            items.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(e) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(e, fontFamily = Mono, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    describe(e)?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                }
            }
        }
    }
}

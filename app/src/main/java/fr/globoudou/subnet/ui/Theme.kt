package fr.globoudou.subnet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import fr.globoudou.subnet.core.AddressKind

val Mono = FontFamily.Monospace

@Immutable
data class ExtraColors(
    val net: Color,
    val netContainer: Color,
    val onNetContainer: Color,
    val host: Color,
    val hostContainer: Color,
    val onHostContainer: Color,
    val hero: Color,
    val key: Color,
    val keyAlt: Color,
    val keyAccent: Color,
    val publicContainer: Color,
    val onPublicContainer: Color,
    val tealContainer: Color,
    val onTealContainer: Color,
    val violetContainer: Color,
    val onVioletContainer: Color,
    val error: Color,
    val segment: List<Color>,
)

private val DarkExtra = ExtraColors(
    net = Color(0xFF60A5FA),
    netContainer = Color(0xFF1E3A5F),
    onNetContainer = Color(0xFF93C5FD),
    host = Color(0xFFFDBA74),
    hostContainer = Color(0xFF3D2A17),
    onHostContainer = Color(0xFFFDBA74),
    hero = Color(0xFF1C2B3D),
    key = Color(0xFF212A33),
    keyAlt = Color(0xFF2A343F),
    keyAccent = Color(0xFF1C2B3D),
    publicContainer = Color(0xFF14321F),
    onPublicContainer = Color(0xFF86EFAC),
    tealContainer = Color(0xFF123534),
    onTealContainer = Color(0xFF5EEAD4),
    violetContainer = Color(0xFF2A2346),
    onVioletContainer = Color(0xFFC4B5FD),
    error = Color(0xFFFCA5A5),
    segment = listOf(Color(0xFF60A5FA), Color(0xFFFDBA74), Color(0xFFA78BFA), Color(0xFF34D399), Color(0xFFF472B6), Color(0xFF5EEAD4)),
)

private val LightExtra = ExtraColors(
    net = Color(0xFF1D4ED8),
    netContainer = Color(0xFFDBEAFE),
    onNetContainer = Color(0xFF1E3A8A),
    host = Color(0xFFC2410C),
    hostContainer = Color(0xFFFFEDD5),
    onHostContainer = Color(0xFF9A3412),
    hero = Color(0xFFE6EEFB),
    key = Color(0xFFEEF1F4),
    keyAlt = Color(0xFFDDE3E9),
    keyAccent = Color(0xFFDBEAFE),
    publicContainer = Color(0xFFDCFCE7),
    onPublicContainer = Color(0xFF166534),
    tealContainer = Color(0xFFCCFBF1),
    onTealContainer = Color(0xFF115E59),
    violetContainer = Color(0xFFEDE9FE),
    onVioletContainer = Color(0xFF5B21B6),
    error = Color(0xFFB91C1C),
    segment = listOf(Color(0xFF2563EB), Color(0xFFEA580C), Color(0xFF7C3AED), Color(0xFF059669), Color(0xFFDB2777), Color(0xFF0D9488)),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F1419),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondaryContainer = Color(0xFF2A343F),
    onSecondaryContainer = Color(0xFFE6EDF3),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF0F1419),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF212A33),
    onSurfaceVariant = Color(0xFF9AA7B4),
    surfaceContainerLowest = Color(0xFF0B0F13),
    surfaceContainerLow = Color(0xFF141B22),
    surfaceContainer = Color(0xFF182028),
    surfaceContainerHigh = Color(0xFF1D252E),
    surfaceContainerHighest = Color(0xFF232C36),
    outline = Color(0xFF3A4653),
    outlineVariant = Color(0xFF2A343F),
    error = Color(0xFFFCA5A5),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondaryContainer = Color(0xFFDDE3E9),
    onSecondaryContainer = Color(0xFF111827),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFF6F8FA),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF1F4),
    onSurfaceVariant = Color(0xFF4B5563),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F4F7),
    surfaceContainerHighest = Color(0xFFE9EDF1),
    outline = Color(0xFFB8C1CB),
    outlineVariant = Color(0xFFD5DBE1),
    error = Color(0xFFB91C1C),
)

val LocalExtraColors = staticCompositionLocalOf { DarkExtra }

object Subnet {
    val colors: ExtraColors
        @Composable get() = LocalExtraColors.current
}

@Composable
fun SubnetTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalExtraColors provides if (dark) DarkExtra else LightExtra) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}

/** Couleurs (fond, texte) d'une étiquette de catégorie d'adresse. */
@Composable
fun kindColors(kind: AddressKind): Pair<Color, Color> {
    val c = Subnet.colors
    val s = MaterialTheme.colorScheme
    return when (kind) {
        AddressKind.PUBLIC -> c.publicContainer to c.onPublicContainer
        AddressKind.PRIVATE -> c.netContainer to c.onNetContainer
        AddressKind.SHARED -> c.tealContainer to c.onTealContainer
        AddressKind.MULTICAST -> c.violetContainer to c.onVioletContainer
        AddressKind.DOCUMENTATION -> c.hostContainer to c.onHostContainer
        AddressKind.LOOPBACK, AddressKind.LINK_LOCAL, AddressKind.SPECIAL,
        AddressKind.RESERVED, AddressKind.BROADCAST -> s.surfaceContainerHighest to s.onSurface
    }
}

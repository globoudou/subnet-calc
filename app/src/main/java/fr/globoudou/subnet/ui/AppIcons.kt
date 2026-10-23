package fr.globoudou.subnet.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icônes au trait (grille 24), dessinées à partir de chemins SVG. */
object AppIcons {
    private fun icon(name: String, vararg paths: String, width: Float = 2f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            b.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val History = icon("history", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M12 7v5l3 2")
    val Copy = icon(
        "copy",
        "M10 8h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2z",
        "M16 8V6a2 2 0 0 0 -2 -2H6a2 2 0 0 0 -2 2v8a2 2 0 0 0 2 2h2",
    )
    val Close = icon("close", "M6 6l12 12", "M18 6L6 18", width = 2.2f)
    val Backspace = icon("backspace", "M21 5H9l-6 7 6 7h12a1 1 0 0 0 1 -1V6a1 1 0 0 0 -1 -1z", "M17 9l-5 6", "M12 9l5 6")
    val Minus = icon("minus", "M5 12h14", width = 2.4f)
    val Plus = icon("plus", "M5 12h14", "M12 5v14", width = 2.4f)
    val Wifi = icon("wifi", "M5 12.5a10 10 0 0 1 14 0", "M8.5 16a5 5 0 0 1 7 0", "M2 9a14.5 14.5 0 0 1 20 0", "M12 19.5h0.01")
    val ChevronDown = icon("chevron", "M6 9l6 6 6 -6")
    val Keyboard = icon(
        "keyboard",
        "M3 6h18a1 1 0 0 1 1 1v10a1 1 0 0 1 -1 1H3a1 1 0 0 1 -1 -1V7a1 1 0 0 1 1 -1z",
        "M6 10h0.01", "M10 10h0.01", "M14 10h0.01", "M18 10h0.01", "M7 14h10",
    )
    val Paste = icon("paste", "M9 3h6v4H9z", "M9 5H7a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2V7a2 2 0 0 0 -2 -2h-2")
    val Trash = icon("trash", "M4 7h16", "M10 11v6", "M14 11v6", "M6 7l1 13h10l1 -13", "M9 7V4h6v3")
    val Network = icon("network", "M9 2h6v6H9z", "M2 16h6v6H2z", "M16 16h6v6h-6z", "M12 8v4", "M5 16v-2h14v2")
    val Split = icon("split", "M4 4h16v16H4z", "M4 12h16", "M12 12v8", "M16 12v8")
    val Globe = icon(
        "globe",
        "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M3 12h18",
        "M12 3a14 14 0 0 1 0 18", "M12 3a14 14 0 0 0 0 18",
    )
    val Share = icon("share", "M4 12v7a1 1 0 0 0 1 1h14a1 1 0 0 0 1 -1v-7", "M12 15V3", "M7 8l5 -5 5 5")
}

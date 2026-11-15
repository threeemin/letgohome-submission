package com.meraki.letgohome.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

internal enum class CampIcon(path: String) {
    HOME("M3 11 L12 3 L21 11 V21 H15 V15 H9 V21 H3 Z"),
    POWER("M13 2 L4 14 H11 L10 22 L20 9 H13 Z"),
    PIN("M20 10 C20 15 12 22 12 22 C12 22 4 15 4 10 A8 8 0 0 1 20 10 Z M12 7 A3 3 0 1 0 12 13 A3 3 0 0 0 12 7"),
    SETTINGS("M9 3 H15 L16 6 L19 6 L22 11 L20 13 L20 16 L17 20 L14 19 L12 22 L8 20 L7 17 L4 16 L3 11 L6 9 L6 6 Z M12 8 A4 4 0 1 0 12 16 A4 4 0 0 0 12 8"),
    CAR("M2 12 L4.5 7 Q5 5 8 5 H16 Q19 5 19.5 7 L22 12 V19 H19 V17 H5 V19 H2 Z M2 12 H22 M6 14.5 H8 M16 14.5 H18"),
    TENT("M12 3 L22 21 H2 Z M12 12 L15 21 H9 Z"),
    SHIELD("M12 3 C15 5 18 6 21 7 V12 C21 16 17 20 12 22 C7 20 3 16 3 12 V7 C6 6 9 5 12 3 Z"),
    BATTERY("M9 4 V2 H15 V4 M7 4 H17 Q19 4 19 6 V20 Q19 22 17 22 H7 Q5 22 5 20 V6 Q5 4 7 4 Z M8 14 H16 M8 18 H16"),
    WARNING("M12 3 L22 21 H2 Z M12 9 V14 M12 17 V17.2");

    val outline = PathParser().parsePathString(path).toPath()
}

@Composable
internal fun CampGlyph(icon: CampIcon, modifier: Modifier, color: Color = CampBlue) {
    Canvas(modifier) {
        withTransform({ scale(size.width / 24f, size.height / 24f, Offset.Zero) }) {
            drawPath(icon.outline, color, style = Stroke(1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private val Mountains = PathParser().parsePathString(
    "M4 53 L36 38 Q42 35 50 38 L58 42 L100 20 Q110 14 121 19 L135 25 Q140 27 145 26 Q154 24 167 29 L198 42 L251 26 Q263 22 273 27 L294 36 L316 43",
).toPath()
private val Ground = PathParser().parsePathString(
    "M4 89 H316 M15 89 Q17 80 26 82 Q29 72 37 74 Q44 75 46 82 Q53 79 58 89 M267 89 Q270 81 278 83 Q282 74 289 75 Q297 76 299 83 Q307 81 310 89",
).toPath()
private val CampCar = PathParser().parsePathString(
    "M84 83 V76 Q85 70 94 68 L107 66 L119 57 Q124 53 132 53 H151 Q159 53 164 57 L173 66 Q176 69 176 76 V83 H84 Z M109 66 L120 58 H151 Q156 58 159 61 L164 66 Z M89 74 H98 M165 74 H173",
).toPath()
private val CampTent = PathParser().parsePathString(
    "M190 89 Q200 80 211 61 L224 48 H259 Q273 78 285 89 Z M224 48 Q216 79 205 89 M224 48 Q233 76 245 89",
).toPath()

@Composable
internal fun CampScenery(modifier: Modifier) {
    Canvas(modifier) {
        val factor = minOf(size.width / 320f, size.height / 96f)
        withTransform({
            translate((size.width - 320f * factor) / 2f, (size.height - 96f * factor) / 2f)
            scale(factor, factor, Offset.Zero)
        }) {
            val pen = Stroke(1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(Mountains, CampBlue.copy(alpha = 0.8f), style = pen)
            drawPath(CampTent, CampGold.copy(alpha = 0.24f))
            drawPath(CampTent, CampBlue, style = pen)
            drawPath(Ground, CampBlue, style = pen)
            drawPath(CampCar, CampCard)
            drawPath(CampCar, CampBlue, style = pen)
            listOf(101f, 160f).forEach { x ->
                drawCircle(CampBlue, 7f, Offset(x, 82f))
                drawCircle(CampCard, 3.5f, Offset(x, 82f))
            }
        }
    }
}

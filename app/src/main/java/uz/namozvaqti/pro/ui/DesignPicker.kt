package uz.namozvaqti.pro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dizayn tanlagich: ikkita katta karta (kichik namuna rasmi bilan). Bosilganda dizayn DARHOL almashadi —
 * butun ilova (shu tanlagichning o'zi ham) yangi ko'rinishda qayta chiziladi.
 * Namunalar o'z rangida chiziladi (joriy rejimga bog'liq emas).
 */
@Composable
fun DesignPicker(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DesignOption(
            DesignMode.SKY, "Kun-tun", "Osmon quyosh va oyga qarab o'zgaradi",
            selected = DesignMode.fromKey(current) == DesignMode.SKY, Modifier.weight(1f),
        ) { onSelect(DesignMode.SKY.key) }
        DesignOption(
            DesignMode.GLASS, "Blur glass", "Sodda shaffof shisha, ko'zga yengil",
            selected = DesignMode.fromKey(current) == DesignMode.GLASS, Modifier.weight(1f),
        ) { onSelect(DesignMode.GLASS.key) }
    }
}

@Composable
private fun DesignOption(mode: DesignMode, title: String, caption: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .pressable(scaleDown = 0.96f, onClick = onClick)
            .glassSurface(shape)
            .border(if (selected) 2.dp else 0.dp, if (selected) Glass.Pink else Color.Transparent, shape)
            .padding(10.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(14.dp))) {
            Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                if (mode == DesignMode.SKY) drawSkyPreview() else drawGlassPreview()
            }
        }
        Text(title + if (selected) "  ✓" else "", style = Type.BodyBold.copy(fontSize = 15.sp, color = if (selected) Glass.PinkSoft else Glass.Ink), modifier = Modifier.padding(top = 8.dp))
        Text(caption, style = Type.Caption.copy(fontSize = 11.sp))
    }
}

/** Kun-tun namunasi: tungi osmon, oy, yulduzlar va masjid siluet (pushti-siyohrang). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSkyPreview() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF140A2B), Color(0xFF3A1A5C), Color(0xFF8A3E8C)), 0f, h))
    listOf(0.12f to 0.18f, 0.30f to 0.40f, 0.62f to 0.14f, 0.82f to 0.34f, 0.46f to 0.25f).forEach { (x, y) ->
        drawCircle(Color.White.copy(alpha = 0.75f), 1.4.dp.toPx(), Offset(w * x, h * y))
    }
    drawCircle(Color(0xFFFFE6F3), 11.dp.toPx(), Offset(w * 0.74f, h * 0.30f))
    drawCircle(Color(0xFF3A1A5C), 9.dp.toPx(), Offset(w * 0.74f + 5.dp.toPx(), h * 0.30f - 3.dp.toPx()))
    val hill = Path().apply {
        moveTo(0f, h); lineTo(0f, h * 0.78f)
        cubicTo(w * 0.25f, h * 0.66f, w * 0.45f, h * 0.84f, w * 0.7f, h * 0.74f)
        lineTo(w, h * 0.80f); lineTo(w, h); close()
    }
    drawPath(hill, Color(0xFF0B0517))
    val dome = Path().apply {
        moveTo(w * 0.16f, h * 0.80f); lineTo(w * 0.16f, h * 0.66f)
        cubicTo(w * 0.16f, h * 0.46f, w * 0.40f, h * 0.46f, w * 0.40f, h * 0.66f)
        lineTo(w * 0.40f, h * 0.80f); close()
    }
    drawPath(dome, Color(0xFF0B0517))
    drawRect(Color(0xFF0B0517), Offset(w * 0.12f, h * 0.50f), Size(w * 0.035f, h * 0.32f))
    drawRect(Color(0xFF0B0517), Offset(w * 0.41f, h * 0.50f), Size(w * 0.035f, h * 0.32f))
}

/** Blur glass namunasi: tekis to'q zumrad fon, xira naqsh va ikkita shaffof shisha karta + oltin shar. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlassPreview() {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF072622))
    val star = Path().apply {
        for (cx in listOf(w * 0.2f, w * 0.62f, w * 1.0f)) for (cy in listOf(h * 0.2f, h * 0.85f)) {
            moveTo(cx, cy - 11.dp.toPx()); lineTo(cx + 3.dp.toPx(), cy - 3.dp.toPx()); lineTo(cx + 11.dp.toPx(), cy)
            lineTo(cx + 3.dp.toPx(), cy + 3.dp.toPx()); lineTo(cx, cy + 11.dp.toPx()); lineTo(cx - 3.dp.toPx(), cy + 3.dp.toPx())
            lineTo(cx - 11.dp.toPx(), cy); lineTo(cx - 3.dp.toPx(), cy - 3.dp.toPx()); close()
        }
    }
    drawPath(star, Color(0xFFE3BC6A).copy(alpha = 0.22f), style = Stroke(1.dp.toPx()))
    fun card(x: Float, y: Float, cw: Float, ch: Float) {
        val r = CornerRadius(9.dp.toPx())
        drawRoundRect(Color(0xFF072622).copy(alpha = 0.4f), Offset(x, y), Size(cw, ch), r)
        drawRoundRect(Color.White.copy(alpha = 0.09f), Offset(x, y), Size(cw, ch), r)
        drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(x, y), Size(cw, ch), r, style = Stroke(1.dp.toPx()))
    }
    card(w * 0.07f, h * 0.12f, w * 0.86f, h * 0.30f)
    card(w * 0.07f, h * 0.50f, w * 0.86f, h * 0.38f)
    drawRect(Color(0xFFF1D58C).copy(alpha = 0.9f), Offset(w * 0.12f, h * 0.20f), Size(w * 0.26f, 5.dp.toPx()))
    drawCircle(Color(0xFFE3BC6A).copy(alpha = 0.35f), 7.dp.toPx(), Offset(w * 0.83f, h * 0.69f))
    drawCircle(Color(0xFFF1D58C), 7.dp.toPx(), Offset(w * 0.83f, h * 0.69f), style = Stroke(1.2.dp.toPx()))
}

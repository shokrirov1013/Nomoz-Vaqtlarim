package uz.namozvaqti.pro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Yagona doimiy dizayn: ikonkaga mos zumrad-oltin "Liquid Glass".
 * Qorong'i fon ko'zni charchatmaydi; kartalar shaffof, nozik chegarali va yorug' aksli.
 * Hamma chizish keshlanadi (drawWithCache) va soya ishlatilmaydi — shuning uchun silliq ishlaydi.
 */
object Glass {
    val Bg = Color(0xFF06231F)
    val BgTop = Color(0xFF0B3B34)
    val BgMid = Color(0xFF082E29)
    val BgBottom = Color(0xFF041A18)

    val Ink = Color(0xFFF3EFE3)        // asosiy matn (iliq oq)
    val InkSoft = Color(0xFFA9C7BD)    // ikkinchi darajali matn
    val Gold = Color(0xFFE3BC6A)       // asosiy urg'u
    val GoldBright = Color(0xFFF1D58C) // urg'uli matn
    val GoldDeep = Color(0xFFC9973F)
    val OnGold = Color(0xFF06231F)     // oltin ustidagi matn
    val Mint = Color(0xFF8FD6C0)       // nozik chiziq/glow
    val Emerald = Color(0xFF2FB38F)
    val Tint = Color(0xFFE9C46A)       // ajratilgan kartalar uchun iliq tus
    val Danger = Color(0xFFFF9DB0)
    val DialogBg = Color(0xFF0E3B34)
}

object Type {
    private val family = FontFamily.SansSerif
    val H1 = TextStyle(fontFamily = family, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Glass.Ink)
    val H2 = TextStyle(fontFamily = family, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Glass.Ink)
    val Body = TextStyle(fontFamily = family, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Glass.Ink)
    val BodyBold = Body.copy(fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontFamily = family, fontSize = 13.sp, color = Glass.InkSoft)
    val Big = TextStyle(fontFamily = family, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Glass.Ink)
}

@Composable
fun NamozTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Glass.Gold,
            onPrimary = Glass.OnGold,
            secondary = Glass.Emerald,
            surface = Glass.Bg,
            onSurface = Glass.Ink,
            background = Glass.Bg,
            onBackground = Glass.Ink,
            surfaceVariant = Glass.DialogBg,
            onSurfaceVariant = Glass.InkSoft,
            outline = Glass.Mint.copy(alpha = 0.5f),
            surfaceContainer = Glass.DialogBg,
            surfaceContainerHigh = Glass.DialogBg,
            surfaceContainerHighest = Glass.DialogBg,
        ),
        content = content,
    )
}

/** Bosilganda to'lqin (ripple) chizmaydigan, umumiy interaksiya manbali bosish — `composed`dan tezroq. */
private val SharedSource = MutableInteractionSource()

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(interactionSource = SharedSource, indication = null, onClick = onClick)

private val GoldBrush = Brush.linearGradient(listOf(Glass.GoldBright, Glass.GoldDeep))

/** Fon: zumrad gradient, yumshoq yorug' dog'lar va nozik islomiy geometrik naqsh. Bir marta chiziladi va keshlanadi. */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val base = Brush.verticalGradient(listOf(Glass.BgTop, Glass.BgMid, Glass.BgBottom))
                fun glow(cx: Float, cy: Float, r: Float, c: Color) =
                    Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), r)
                val g1 = glow(w * 0.95f, h * 0.10f, w * 0.85f, Glass.Emerald.copy(alpha = 0.26f))
                val g2 = glow(w * 0.05f, h * 0.55f, w * 0.75f, Glass.Mint.copy(alpha = 0.10f))
                val g3 = glow(w * 0.20f, h * 0.98f, w * 0.85f, Glass.Gold.copy(alpha = 0.13f))
                val cell = 88.dp.toPx()
                val pattern = Path()
                var y = 0f
                while (y < h + cell) {
                    var x = 0f
                    while (x < w + cell) {
                        addStar(pattern, x + cell / 2, y + cell / 2, cell * 0.30f)
                        x += cell
                    }
                    y += cell
                }
                val stroke = Stroke(width = 1.dp.toPx())
                onDrawBehind {
                    drawRect(base)
                    drawCircle(g1, w * 0.85f, Offset(w * 0.95f, h * 0.10f))
                    drawCircle(g2, w * 0.75f, Offset(w * 0.05f, h * 0.55f))
                    drawCircle(g3, w * 0.85f, Offset(w * 0.20f, h * 0.98f))
                    drawPath(pattern, Glass.Gold.copy(alpha = 0.055f), style = stroke)
                }
            },
        content = content,
    )
}

/** Sakkiz uchli yulduz (ikki aylantirilgan kvadrat) — klassik islomiy naqsh. */
private fun addStar(path: Path, cx: Float, cy: Float, r: Float) {
    for (rot in listOf(0.0, PI / 4)) {
        for (i in 0..3) {
            val a = rot + i * PI / 2 + PI / 4
            val px = cx + (r * cos(a)).toFloat() * 1.41f
            val py = cy + (r * sin(a)).toFloat() * 1.41f
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
    }
}

/**
 * Shaffof oyna yuzasi: yengil rangli qatlam + nozik gradient chegara + yuqori yorug'lik chizig'i.
 * Hammasi bitta chizish o'tishida va keshlanadi (soya/blur yo'q).
 */
fun Modifier.glassSurface(shape: Shape, tint: Color = Color.White, strength: Float = 1f): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val fill = Brush.verticalGradient(
            listOf(tint.copy(alpha = 0.13f * strength), tint.copy(alpha = 0.055f * strength)),
        )
        val border = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.36f), Color.White.copy(alpha = 0.07f), Glass.Gold.copy(alpha = 0.26f)),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        val sheen = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
            startX = size.width * 0.1f,
            endX = size.width * 0.9f,
        )
        val stroke = Stroke(width = 1.dp.toPx())
        val lineY = 1.dp.toPx()
        onDrawBehind {
            drawOutline(outline, fill)
            drawOutline(outline, border, style = stroke)
            drawLine(sheen, Offset(size.width * 0.1f, lineY), Offset(size.width * 0.9f, lineY), strokeWidth = 1.dp.toPx())
        }
    }

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    padding: Dp = 16.dp,
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier.glassSurface(RoundedCornerShape(corner), tint)
    if (onClick != null) m = m.clickableNoRipple(onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    small: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(if (small) 14.dp else 18.dp)
    val base = if (filled) modifier.background(GoldBrush, shape)
    else modifier.glassSurface(shape, Glass.Tint, strength = 0.9f)
    val fg = if (filled) Glass.OnGold else Glass.GoldBright
    Row(
        base
            .heightIn(min = if (small) 40.dp else 54.dp)
            .clickable(interactionSource = SharedSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg.copy(alpha = if (enabled) 1f else 0.5f), modifier = Modifier.size(20.dp))
            Box(Modifier.size(8.dp))
        }
        Text(
            text,
            style = Type.BodyBold.copy(color = fg.copy(alpha = if (enabled) 1f else 0.45f), fontSize = if (small) 14.sp else 16.sp),
        )
    }
}

/** Tanlanadigan "tabletka" (hafta kunlari, rejimlar). */
@Composable
fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    val base = if (selected) modifier.background(GoldBrush, shape) else modifier.glassSurface(shape)
    Box(
        base.clickableNoRipple(onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.BodyBold.copy(fontSize = 14.sp, color = if (selected) Glass.OnGold else Glass.GoldBright))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.H2, modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp))
}

/** Hilol va masjid siluet (sarlavha bezagi) — ikonkaga mos oltin hilol. */
@Composable
fun CrescentMosqueArt(modifier: Modifier = Modifier) {
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val glow = Brush.radialGradient(
                listOf(Glass.Gold.copy(alpha = 0.30f), Color.Transparent), Offset(w * 0.55f, w * 0.28f), w * 0.5f,
            )
            val outer = Path().apply { addOval(Rect(Offset(w * 0.30f, 0f), Size(w * 0.55f, w * 0.55f))) }
            val inner = Path().apply { addOval(Rect(Offset(w * 0.45f, -w * 0.02f), Size(w * 0.5f, w * 0.5f))) }
            val crescent = Path.combine(PathOperation.Difference, outer, inner)
            val gold = Brush.linearGradient(listOf(Glass.GoldBright, Glass.GoldDeep), Offset(w * 0.3f, 0f), Offset(w * 0.8f, w * 0.55f))
            val base = h
            val sil = Path().apply {
                moveTo(w * 0.28f, base)
                lineTo(w * 0.28f, base - h * 0.28f)
                cubicTo(w * 0.28f, base - h * 0.62f, w * 0.72f, base - h * 0.62f, w * 0.72f, base - h * 0.28f)
                lineTo(w * 0.72f, base)
                close()
                addRect(Rect(Offset(w * 0.14f, base - h * 0.55f), Size(w * 0.09f, h * 0.55f)))
                moveTo(w * 0.14f, base - h * 0.55f); lineTo(w * 0.185f, base - h * 0.70f); lineTo(w * 0.23f, base - h * 0.55f); close()
                addRect(Rect(Offset(w * 0.77f, base - h * 0.55f), Size(w * 0.09f, h * 0.55f)))
                moveTo(w * 0.77f, base - h * 0.55f); lineTo(w * 0.815f, base - h * 0.70f); lineTo(w * 0.86f, base - h * 0.55f); close()
            }
            val silBrush = Brush.verticalGradient(listOf(Glass.Mint.copy(alpha = 0.38f), Glass.Emerald.copy(alpha = 0.10f)))
            onDrawBehind {
                drawCircle(glow, w * 0.5f, Offset(w * 0.55f, w * 0.28f))
                drawPath(crescent, gold)
                drawPath(sil, silBrush)
            }
        },
    )
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Glass.Mint.copy(alpha = 0.14f)))
}


/** Shisha almashtirgich (switch): shaffof "tabletka" trek, yoqilganda oltin shisha va yorqin bosh. */
@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val off by androidx.compose.animation.core.animateDpAsState(
        if (checked) 22.dp else 0.dp, androidx.compose.animation.core.tween(150), label = "thumb",
    )
    val track = RoundedCornerShape(50)
    Box(
        modifier
            .size(width = 52.dp, height = 30.dp)
            .then(if (checked) Modifier.glassSurface(track, Glass.Tint, 2.4f) else Modifier.glassSurface(track, Color.White, 0.7f))
            .clickableNoRipple { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(off.roundToPx(), 0) }
                .size(22.dp)
                .background(if (checked) GoldBrush else ThumbOffBrush, androidx.compose.foundation.shape.CircleShape),
        )
    }
}

private val ThumbOffBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.75f), Glass.Mint.copy(alpha = 0.45f)))

/** Shisha belgi (tick): bo'sh shisha halqa yoki oltin shisha doira ichida aniq ✓. */
@Composable
fun GlassTick(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val shape = androidx.compose.foundation.shape.CircleShape
    Box(
        modifier
            .size(size)
            .then(if (checked) Modifier.glassSurface(shape, Glass.Tint, 2.6f) else Modifier.glassSurface(shape, Color.White, 0.6f))
            .drawBehind {
                if (checked) {
                    val w = this.size.width
                    val h = this.size.height
                    val p = Path().apply {
                        moveTo(w * 0.27f, h * 0.53f)
                        lineTo(w * 0.43f, h * 0.68f)
                        lineTo(w * 0.73f, h * 0.34f)
                    }
                    drawPath(
                        p, Glass.GoldBright,
                        style = Stroke(width = w * 0.11f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
                    )
                }
            },
    )
}

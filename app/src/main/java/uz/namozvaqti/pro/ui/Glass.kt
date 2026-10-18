package uz.namozvaqti.pro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * Yagona doimiy dizayn: qora fonda pushti-siyohrang "Liquid Glass" (suyuq shisha).
 * Qorong'i fon ko'zni charchatmaydi; kartalar shaffof, nozik chegarali, yuqori-chap burchakda yorug' aksli.
 * Hamma chizish keshlanadi (drawWithCache) va soya ishlatilmaydi — shuning uchun silliq ishlaydi.
 */
object Glass {
    val Bg = Color(0xFF07030F)
    val BgTop = Color(0xFF140A2B)
    val BgMid = Color(0xFF0C0619)
    val BgBottom = Color(0xFF05020B)

    val Ink = Color(0xFFF7F1FF)        // asosiy matn
    val InkSoft = Color(0xFFB9AAD6)    // ikkinchi darajali matn
    val Pink = Color(0xFFFF8AC9)       // asosiy urg'u
    val PinkSoft = Color(0xFFFFC0E4)   // urg'uli matn
    val Magenta = Color(0xFFD060C0)    // chuqur pushti
    val OnPink = Color(0xFF2A0F45)     // pushti ustidagi matn
    val Lilac = Color(0xFFCDBCFF)      // nozik chiziq va nur
    val Violet = Color(0xFF8F6BFF)     // siyohrang
    val Tint = Color(0xFFE9A7FF)       // ajratilgan kartalar uchun tus
    val Danger = Color(0xFFFF8FA8)
    val DialogBg = Color(0xFF1B1033)
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
            primary = Glass.Pink,
            onPrimary = Glass.OnPink,
            secondary = Glass.Violet,
            surface = Glass.Bg,
            onSurface = Glass.Ink,
            background = Glass.Bg,
            onBackground = Glass.Ink,
            surfaceVariant = Glass.DialogBg,
            onSurfaceVariant = Glass.InkSoft,
            outline = Glass.Lilac.copy(alpha = 0.5f),
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

/**
 * Bosish animatsiyasi: bosilganda tugma prujinali "yumshoq" kichrayadi va qo'yib yuborilganda sakrab qaytadi.
 * Faqat graphicsLayer o'zgaradi (qayta o'lchash va qayta kompozitsiya yo'q), shuning uchun FPS tushmaydi.
 * MUHIM: zanjirda bu modifier fon/chegara modifierlaridan OLDIN turishi kerak — shunda butun tugma kichrayadi.
 */
fun Modifier.pressable(enabled: Boolean = true, scaleDown: Float = 0.95f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) scaleDown else 1f, spring(dampingRatio = 0.5f, stiffness = 650f), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/** Kartalar sahifaga kirganda pastdan yumshoq suzib chiqadi va ravshanlashadi (graphicsLayer, qayta o'lchashsiz). */
fun Modifier.appear(delayMs: Int = 0): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        a.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 230f))
    }
    graphicsLayer {
        alpha = a.value.coerceIn(0f, 1f)
        translationY = (1f - a.value) * 26f * density
    }
}

/** To'ldirilgan shisha tugma rangi: yumshoq pushti -> lavanda. */
private val PinkBrush = Brush.linearGradient(listOf(Color(0xFFFFC4E3), Color(0xFFD9C5FF)))

/** Ichki sahifalar foni: qora, nozik pushti/siyohrang nur va juda xira islomiy yulduz naqsh. Bir marta chiziladi. */
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
                val g1 = glow(w * 0.95f, h * 0.10f, w * 0.85f, Glass.Violet.copy(alpha = 0.26f))
                val g2 = glow(w * 0.05f, h * 0.55f, w * 0.75f, Glass.Pink.copy(alpha = 0.12f))
                val g3 = glow(w * 0.20f, h * 0.98f, w * 0.85f, Glass.Violet.copy(alpha = 0.16f))
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
                    drawPath(pattern, Glass.Lilac.copy(alpha = 0.06f), style = stroke)
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
 * Suyuq shisha yuzasi: yengil rangli qatlam (yuqori-chapdan pastga so'nadi) + nozik gradient chegara +
 * yuqori yorug'lik chizig'i. Hammasi bitta chizish o'tishida va keshlanadi (soya/blur yo'q).
 */
fun Modifier.glassSurface(shape: Shape, tint: Color = Color.White, strength: Float = 1f): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val fill = Brush.linearGradient(
            listOf(tint.copy(alpha = (0.11f * strength).coerceAtMost(0.9f)), tint.copy(alpha = (0.03f * strength).coerceAtMost(0.9f))),
            start = Offset.Zero,
            end = Offset(size.width * 0.55f, size.height),
        )
        val border = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.08f), Glass.Pink.copy(alpha = 0.34f)),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        val sheen = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.62f), Color.Transparent),
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

/** Yuqori yarmida yumshoq oq yorug'lik — to'ldirilgan "suyuq shisha" tugmalar uchun. */
fun Modifier.gloss(shape: Shape, alpha: Float = 0.34f): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val br = Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent), startY = 0f, endY = size.height * 0.62f)
        onDrawBehind { drawOutline(outline, br) }
    }

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    padding: Dp = 16.dp,
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null,
    animateIn: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = if (animateIn) modifier.appear() else modifier
    if (onClick != null) m = m.pressable(scaleDown = 0.975f, onClick = onClick)
    Column(m.glassSurface(RoundedCornerShape(corner), tint).padding(padding), content = content)
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
    val shape = RoundedCornerShape(if (small) 16.dp else 22.dp)
    val deco = if (filled) Modifier
        .background(PinkBrush, shape)
        .gloss(shape)
        .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
    else Modifier.glassSurface(shape, Glass.Tint, strength = 0.9f)
    val fg = if (filled) Glass.OnPink else Glass.PinkSoft
    Row(
        modifier
            .pressable(enabled, 0.95f, onClick)
            .then(deco)
            .heightIn(min = if (small) 40.dp else 54.dp)
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

/** Yumaloq shisha ikonka tugmasi (masalan, Suralar / Tahajjud). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color = Glass.PinkSoft,
) {
    Box(
        modifier.size(size).pressable(scaleDown = 0.9f, onClick = onClick).glassSurface(CircleShape, Glass.Tint, 1.1f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** Tanlanadigan "tabletka" (hafta kunlari, rejimlar). */
@Composable
fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, raw: Boolean = false) {
    val shape = RoundedCornerShape(50)
    val deco = if (selected) Modifier.background(PinkBrush, shape).gloss(shape, 0.3f).border(1.dp, Color.White.copy(alpha = 0.4f), shape)
    else Modifier.glassSurface(shape)
    Box(
        modifier.pressable(scaleDown = 0.93f, onClick = onClick).then(deco).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        val st = Type.BodyBold.copy(fontSize = 14.sp, color = if (selected) Glass.OnPink else Glass.PinkSoft)
        // raw = true: yozuv tanlagichi — belgilangan yozuvga qaramay doim o'z harfida ko'rinadi
        if (raw) androidx.compose.material3.Text(text, style = st) else Text(text, style = st)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.H2, modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp))
}

/** Hilol va masjid siluet (sarlavha bezagi) — pushti-siyohrang hilol. */
@Composable
fun CrescentMosqueArt(modifier: Modifier = Modifier) {
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val glow = Brush.radialGradient(
                listOf(Glass.Violet.copy(alpha = 0.40f), Color.Transparent), Offset(w * 0.55f, w * 0.28f), w * 0.5f,
            )
            val outer = Path().apply { addOval(Rect(Offset(w * 0.30f, 0f), Size(w * 0.55f, w * 0.55f))) }
            val inner = Path().apply { addOval(Rect(Offset(w * 0.45f, -w * 0.02f), Size(w * 0.5f, w * 0.5f))) }
            val crescent = Path.combine(PathOperation.Difference, outer, inner)
            val tone = Brush.linearGradient(listOf(Glass.PinkSoft, Glass.Violet), Offset(w * 0.3f, 0f), Offset(w * 0.8f, w * 0.55f))
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
            val silBrush = Brush.verticalGradient(listOf(Glass.Violet.copy(alpha = 0.55f), Glass.Pink.copy(alpha = 0.10f)))
            onDrawBehind {
                drawCircle(glow, w * 0.5f, Offset(w * 0.55f, w * 0.28f))
                drawPath(crescent, tone)
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
    Box(modifier.fillMaxWidth().height(1.dp).background(Glass.Lilac.copy(alpha = 0.14f)))
}

// ------------------------------------------------------------------ shisha shar (orb)

/**
 * Suyuq shisha shar. [progress] 0..1: 0 = bo'sh shaffof shisha, oraliqda pushti "suyuqlik" shu balandlikkacha ko'tarilgan,
 * 1 = to'liq pushti-siyohrang va (mark bo'lsa) ichida ✓. [level] — progress 0 bo'lganda ko'rsatiladigan o'zgarmas suyuqlik balandligi.
 */
internal fun DrawScope.drawOrb(progress: Float, level: Float, active: Boolean, mark: Boolean) {
    val w = size.width
    val h = size.height
    val r = size.minDimension / 2f
    val c = Offset(w / 2f, h / 2f)
    val hl = Offset(w * 0.34f, h * 0.26f)
    val full = progress >= 0.999f
    // shisha tana
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(0.62f), Color.White.copy(0.16f), Color.White.copy(0.07f)), hl, r * 1.25f),
        r, c,
    )
    val liquid = if (progress > 0.001f) progress else level
    if (full) {
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(0.85f), Glass.Pink.copy(0.92f), Glass.Violet.copy(0.96f)), hl, r * 1.35f),
            r, c,
        )
    } else if (liquid > 0.02f) {
        val clip = Path().apply { addOval(Rect(Offset.Zero, size)) }
        clipPath(clip) {
            val y = h * (1f - liquid.coerceIn(0f, 1f))
            val amp = h * 0.09f
            val wave = Path().apply {
                moveTo(0f, y)
                cubicTo(w * 0.22f, y - amp, w * 0.30f, y - amp, w * 0.5f, y)
                cubicTo(w * 0.70f, y + amp, w * 0.78f, y + amp, w, y)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(wave, Brush.verticalGradient(listOf(Glass.PinkSoft, Glass.Violet), startY = y, endY = h))
        }
    }
    val rimW = 1.2.dp.toPx()
    if (active && !full) drawCircle(Glass.Pink.copy(0.22f), r + 2.5.dp.toPx(), c, style = Stroke(3.dp.toPx()))
    drawCircle(
        if (progress > 0.5f || active) Glass.PinkSoft.copy(0.88f) else Color.White.copy(0.42f),
        r - rimW / 2f, c, style = Stroke(rimW),
    )
    // yorug' dog' (specular)
    drawOval(
        Brush.verticalGradient(listOf(Color.White.copy(0.75f), Color.Transparent), startY = h * 0.12f, endY = h * 0.38f),
        topLeft = Offset(w * 0.2f, h * 0.12f), size = Size(w * 0.42f, h * 0.24f),
    )
    if (mark && progress > 0.72f) {
        val a = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)
        val p = Path().apply {
            moveTo(w * 0.27f, h * 0.53f); lineTo(w * 0.43f, h * 0.68f); lineTo(w * 0.73f, h * 0.34f)
        }
        drawPath(p, Color.White.copy(alpha = a), style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Belgilash tugmasi: bo'sh shisha shar / suyuqlik to'lib borayotgan shar ([level]) / belgilanganda suyuqlik to'lib ✓ chiqadi (animatsiya). */
@Composable
fun GlassOrb(
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    level: Float = 0f,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val p by animateFloatAsState(if (checked) 1f else 0f, tween(650, easing = FastOutSlowInEasing), label = "orb")
    val m = if (onClick != null) modifier.pressable(scaleDown = 0.86f, onClick = onClick) else modifier
    Box(m.size(size).drawBehind { drawOrb(p, level, active, true) })
}

/** Shisha belgi (tick): bo'sh shisha shar yoki pushti suyuq shar ichida aniq ✓ (belgilanganda suyuqlik to'ladi). */
@Composable
fun GlassTick(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val p by animateFloatAsState(if (checked) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "tick")
    Box(modifier.size(size).drawBehind { drawOrb(p, 0f, false, true) })
}

/** Shisha almashtirgich (switch): shaffof "tabletka" trek, yoqilganda pushti shisha va yorqin shar. */
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
                .drawBehind { drawOrb(if (checked) 1f else 0f, 0f, false, false) },
        )
    }
}

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/** Dizayn rejimi: kun-tun osmoni (pushti-siyohrang) yoki sodda blur glass (zumrad-oltin). Foydalanuvchi tanlaydi. */
enum class DesignMode(val key: String) {
    SKY("sky"),
    GLASS("glass");

    companion object {
        fun fromKey(key: String?): DesignMode = entries.firstOrNull { it.key == key } ?: SKY
    }
}

/** Bitta dizayn palitrasi. Nomlar ikkala rejimda bir xil: `pink` — asosiy urg'u (SKY: pushti, GLASS: oltin) va h.k. */
class Palette(
    val bg: Color, val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val ink: Color, val inkSoft: Color,
    val pink: Color, val pinkSoft: Color, val magenta: Color, val onPink: Color,
    val lilac: Color, val violet: Color, val tint: Color, val danger: Color, val dialogBg: Color,
    val fillStart: Color, val fillEnd: Color,
    val bubbleStart: Color, val bubbleEnd: Color,
) {
    private val family = FontFamily.SansSerif
    val h1 = TextStyle(fontFamily = family, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ink)
    val h2 = TextStyle(fontFamily = family, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = ink)
    val body = TextStyle(fontFamily = family, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = ink)
    val bodyBold = body.copy(fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(fontFamily = family, fontSize = 13.sp, color = inkSoft)
    val big = TextStyle(fontFamily = family, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = ink)
}

/**
 * Ilova dizayni. Ikki rejim:
 *  • SKY   — qora fonda pushti-siyohrang "Liquid Glass", orqada quyosh/oyga qarab o'zgaradigan jonli osmon.
 *  • GLASS — tekis to'q zumrad fonda sodda blur glass: shaffof xira shisha kartalar, oltin urg'u, yaltiroqsiz.
 * Rejim [mode] kuzatiladigan holat: o'zgarsa, rang o'qigan hamma joy (kompozitsiya va chizish) o'zi yangilanadi.
 * Hamma chizish keshlanadi (drawWithCache) va soya ishlatilmaydi — shuning uchun silliq ishlaydi.
 */
object Glass {
    var mode: DesignMode by mutableStateOf(DesignMode.SKY)
        private set

    /** Saqlangan sozlamadagi kalit bo'yicha rejimni o'rnatadi (asosiy oqimdan chaqiring). */
    fun apply(key: String?) {
        val m = DesignMode.fromKey(key)
        if (m != mode) mode = m
    }

    private val sky = Palette(
        bg = Color(0xFF07030F), bgTop = Color(0xFF140A2B), bgMid = Color(0xFF0C0619), bgBottom = Color(0xFF05020B),
        ink = Color(0xFFF7F1FF), inkSoft = Color(0xFFB9AAD6),
        pink = Color(0xFFFF8AC9), pinkSoft = Color(0xFFFFC0E4), magenta = Color(0xFFD060C0), onPink = Color(0xFF2A0F45),
        lilac = Color(0xFFCDBCFF), violet = Color(0xFF8F6BFF), tint = Color(0xFFE9A7FF), danger = Color(0xFFFF8FA8),
        dialogBg = Color(0xFF1B1033),
        fillStart = Color(0xFFFFC4E3), fillEnd = Color(0xFFD9C5FF),
        bubbleStart = Color(0xFFFF8AC9).copy(alpha = 0.55f), bubbleEnd = Color(0xFF8F6BFF).copy(alpha = 0.5f),
    )

    private val glass = Palette(
        bg = Color(0xFF072622), bgTop = Color(0xFF072622), bgMid = Color(0xFF072622), bgBottom = Color(0xFF072622),
        ink = Color(0xFFF3EFE3), inkSoft = Color(0xFFA9C7BD),
        pink = Color(0xFFE3BC6A), pinkSoft = Color(0xFFF1D58C), magenta = Color(0xFFC9973F), onPink = Color(0xFF06231F),
        lilac = Color(0xFF8FD6C0), violet = Color(0xFF2FB38F), tint = Color(0xFFE3BC6A), danger = Color(0xFFFF9A8A),
        dialogBg = Color(0xFF0E3B34),
        fillStart = Color(0xFFF1D58C), fillEnd = Color(0xFFE3BC6A),
        bubbleStart = Color(0xFFF9E7B0).copy(alpha = 0.6f), bubbleEnd = Color(0xFFE3BC6A).copy(alpha = 0.4f),
    )

    val p: Palette get() = if (mode == DesignMode.GLASS) glass else sky

    val Bg get() = p.bg
    val BgTop get() = p.bgTop
    val BgMid get() = p.bgMid
    val BgBottom get() = p.bgBottom

    val Ink get() = p.ink                // asosiy matn
    val InkSoft get() = p.inkSoft        // ikkinchi darajali matn
    val Pink get() = p.pink              // asosiy urg'u
    val PinkSoft get() = p.pinkSoft      // urg'uli matn
    val Magenta get() = p.magenta        // chuqur urg'u
    val OnPink get() = p.onPink          // to'ldirilgan urg'u ustidagi matn
    val Lilac get() = p.lilac            // nozik chiziq va nur
    val Violet get() = p.violet          // ikkinchi urg'u
    val Tint get() = p.tint              // ajratilgan kartalar uchun tus
    val Danger get() = p.danger
    val DialogBg get() = p.dialogBg

    /** To'ldirilgan tugma rangi: SKY — yumshoq pushti->lavanda, GLASS — oltin. */
    val fillBrush: Brush get() = Brush.linearGradient(listOf(p.fillStart, p.fillEnd))
}

object Type {
    val H1: TextStyle get() = Glass.p.h1
    val H2: TextStyle get() = Glass.p.h2
    val Body: TextStyle get() = Glass.p.body
    val BodyBold: TextStyle get() = Glass.p.bodyBold
    val Caption: TextStyle get() = Glass.p.caption
    val Big: TextStyle get() = Glass.p.big
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

/**
 * Hamma sahifalar foni. SKY: qora, nozik pushti/siyohrang nur va juda xira yulduz naqsh.
 * GLASS: tekis to'q zumrad (gradient va nursiz) va biroz ko'rinarli oltin islomiy yulduz naqsh —
 * shaffof kartalar ostida xiralashib ko'rinadi. Bir marta chiziladi.
 */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                val flat = Glass.mode == DesignMode.GLASS
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
                if (flat) {
                    val bg = Glass.Bg
                    val line = Glass.Pink.copy(alpha = 0.12f)
                    onDrawBehind {
                        drawRect(bg)
                        drawPath(pattern, line, style = stroke)
                    }
                } else {
                    val base = Brush.verticalGradient(listOf(Glass.BgTop, Glass.BgMid, Glass.BgBottom))
                    fun glow(cx: Float, cy: Float, r: Float, c: Color) =
                        Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), r)
                    val g1 = glow(w * 0.95f, h * 0.10f, w * 0.85f, Glass.Violet.copy(alpha = 0.26f))
                    val g2 = glow(w * 0.05f, h * 0.55f, w * 0.75f, Glass.Pink.copy(alpha = 0.12f))
                    val g3 = glow(w * 0.20f, h * 0.98f, w * 0.85f, Glass.Violet.copy(alpha = 0.16f))
                    onDrawBehind {
                        drawRect(base)
                        drawCircle(g1, w * 0.85f, Offset(w * 0.95f, h * 0.10f))
                        drawCircle(g2, w * 0.75f, Offset(w * 0.05f, h * 0.55f))
                        drawCircle(g3, w * 0.85f, Offset(w * 0.20f, h * 0.98f))
                        drawPath(pattern, Glass.Lilac.copy(alpha = 0.06f), style = stroke)
                    }
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
 * Shisha yuza.
 *  • SKY: yengil rangli qatlam (yuqori-chapdan pastga so'nadi) + nozik gradient chegara + yuqori yorug'lik chizig'i.
 *  • GLASS (sodda blur glass): fon rangidan xira "muz" qatlami (ostidagi naqsh yumshab ko'rinadi) + shaffof oq qatlam +
 *    yuqori-chap burchakda zo'rg'a seziladigan yumshoq nur + ingichka oq chegara. Yaltiroq chiziq va soya yo'q.
 * [lit] = true bo'lsa, rejimdan qat'i nazar SKY'dagi yonuvchi ko'rinish (pastki panel uchun).
 * Hammasi bitta chizish o'tishida va keshlanadi (haqiqiy blur/soya yo'q — FPS uchun).
 */
fun Modifier.glassSurface(shape: Shape, tint: Color = Color.White, strength: Float = 1f, lit: Boolean = false): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(width = 1.dp.toPx())
        if (Glass.mode == DesignMode.GLASS && !lit) {
            val k = strength.coerceIn(0.5f, 2.6f)
            val a = (if (tint == Color.White) 0.075f else 0.10f) * k
            val frost = Glass.Bg.copy(alpha = 0.40f)
            val fill = Brush.verticalGradient(
                listOf(tint.copy(alpha = (a * 1.35f).coerceAtMost(0.5f)), tint.copy(alpha = (a * 0.8f).coerceAtMost(0.5f))),
            )
            val light = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.18f, 0f),
                radius = (size.width * 0.75f).coerceAtLeast(1f),
            )
            val border = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.12f)))
            onDrawBehind {
                drawOutline(outline, frost)
                drawOutline(outline, fill)
                drawOutline(outline, light)
                drawOutline(outline, border, style = stroke)
            }
        } else {
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
            val lineY = 1.dp.toPx()
            onDrawBehind {
                drawOutline(outline, fill)
                drawOutline(outline, border, style = stroke)
                drawLine(sheen, Offset(size.width * 0.1f, lineY), Offset(size.width * 0.9f, lineY), strokeWidth = 1.dp.toPx())
            }
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
    val flat = Glass.mode == DesignMode.GLASS
    val deco = when {
        filled && !flat -> Modifier
            .background(Glass.fillBrush, shape)
            .gloss(shape)
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
        // sodda blur glass: oltin tusli shaffof shisha, oltin chegara (yaltiroqsiz)
        filled -> Modifier.glassSurface(shape, Glass.Pink, 1.7f).border(1.dp, Glass.Pink.copy(alpha = 0.6f), shape)
        flat -> Modifier.glassSurface(shape)
        else -> Modifier.glassSurface(shape, Glass.Tint, strength = 0.9f)
    }
    val fg = if (filled && !flat) Glass.OnPink else Glass.PinkSoft
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
    val flat = Glass.mode == DesignMode.GLASS
    val deco = when {
        selected && !flat -> Modifier.background(Glass.fillBrush, shape).gloss(shape, 0.3f).border(1.dp, Color.White.copy(alpha = 0.4f), shape)
        selected -> Modifier.glassSurface(shape, Glass.Pink, 1.8f).border(1.dp, Glass.Pink.copy(alpha = 0.6f), shape)
        else -> Modifier.glassSurface(shape)
    }
    Box(
        modifier.pressable(scaleDown = 0.93f, onClick = onClick).then(deco).padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        val fg = if (flat) { if (selected) Glass.PinkSoft else Glass.Ink } else { if (selected) Glass.OnPink else Glass.PinkSoft }
        val st = Type.BodyBold.copy(fontSize = 14.sp, color = fg)
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
            val flat = Glass.mode == DesignMode.GLASS
            val glow = Brush.radialGradient(
                listOf(Glass.Violet.copy(alpha = 0.40f), Color.Transparent), Offset(w * 0.55f, w * 0.28f), w * 0.5f,
            )
            val outer = Path().apply { addOval(Rect(Offset(w * 0.30f, 0f), Size(w * 0.55f, w * 0.55f))) }
            val inner = Path().apply { addOval(Rect(Offset(w * 0.45f, -w * 0.02f), Size(w * 0.5f, w * 0.5f))) }
            val crescent = Path.combine(PathOperation.Difference, outer, inner)
            val tone = Brush.linearGradient(
                listOf(Glass.PinkSoft, if (flat) Glass.Magenta else Glass.Violet), Offset(w * 0.3f, 0f), Offset(w * 0.8f, w * 0.55f),
            )
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
            val silBrush = if (flat) Brush.verticalGradient(listOf(Glass.Lilac.copy(alpha = 0.40f), Glass.Violet.copy(alpha = 0.10f)))
            else Brush.verticalGradient(listOf(Glass.Violet.copy(alpha = 0.55f), Glass.Pink.copy(alpha = 0.10f)))
            onDrawBehind {
                if (!flat) drawCircle(glow, w * 0.5f, Offset(w * 0.55f, w * 0.28f))
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
    if (Glass.mode == DesignMode.GLASS) {
        drawFlatOrb(progress, level, active, mark)
        return
    }
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

/**
 * Sodda blur glass sharchasi: yaltiroq dog'siz, tekis. Bo'sh = shaffof shisha doira, oraliqda oltin "suyuqlik" ([level] / [progress]),
 * to'lganda oltin tusli doira va ichida ✓ (mark) yoki to'q oltin to'p (mark = false: almashtirgich dastagi).
 */
private fun DrawScope.drawFlatOrb(progress: Float, level: Float, active: Boolean, mark: Boolean) {
    val w = size.width
    val h = size.height
    val r = size.minDimension / 2f
    val c = Offset(w / 2f, h / 2f)
    val full = progress >= 0.999f
    val rimW = 1.2.dp.toPx()
    if (!mark) {
        // almashtirgich dastagi: yoqilganda oltin, o'chiqda och shisha
        drawCircle(if (full) Glass.Pink.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.55f), r, c)
        return
    }
    drawCircle(Color.White.copy(alpha = 0.08f), r, c)
    val liquid = if (progress > 0.001f) progress else level
    if (full) {
        drawCircle(Glass.Pink.copy(alpha = 0.26f), r, c)
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
            drawPath(wave, Glass.Pink.copy(alpha = 0.5f))
        }
    }
    if (active && !full) drawCircle(Glass.Pink.copy(alpha = 0.22f), r + 2.5.dp.toPx(), c, style = Stroke(3.dp.toPx()))
    drawCircle(
        if (progress > 0.5f || active) Glass.PinkSoft.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.30f),
        r - rimW / 2f, c, style = Stroke(rimW),
    )
    if (progress > 0.72f) {
        val a = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)
        val p = Path().apply {
            moveTo(w * 0.27f, h * 0.53f); lineTo(w * 0.43f, h * 0.68f); lineTo(w * 0.73f, h * 0.34f)
        }
        drawPath(p, Glass.PinkSoft.copy(alpha = a), style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
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

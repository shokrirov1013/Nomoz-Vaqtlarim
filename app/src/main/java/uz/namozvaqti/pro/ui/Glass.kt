package uz.namozvaqti.pro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Yagona doimiy dizayn: Oq-Pushti-Siyohrang Liquid Glass.
 * Mavzu almashtirish yo'q — butun ilova shu ranglardan foydalanadi.
 */
object Glass {
    val Bg = Color(0xFFFDFAFF)
    val BgPink = Color(0xFFFFEFF7)
    val BgLavender = Color(0xFFF0E8FF)

    val Ink = Color(0xFF2B1D4B)
    val InkSoft = Color(0xFF6A5C86)
    val Purple = Color(0xFF8A63E6)
    val PurpleDeep = Color(0xFF6A43CC)
    val Lavender = Color(0xFFC9B8F7)
    val Pink = Color(0xFFF6C3DF)
    val PinkSoft = Color(0xFFFFE3F1)
    val Danger = Color(0xFFC2416B)
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
        colorScheme = lightColorScheme(
            primary = Glass.Purple,
            onPrimary = Color.White,
            secondary = Glass.Pink,
            surface = Glass.Bg,
            onSurface = Glass.Ink,
            background = Glass.Bg,
            onBackground = Glass.Ink,
            surfaceVariant = Glass.BgLavender,
            outline = Glass.Lavender,
        ),
        content = content,
    )
}

/** Fon: yumshoq oq -> och pushti -> lavanda, nozik yorug' dog'lar va islomiy geometrik naqsh. */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Glass.Bg, Glass.BgPink, Glass.BgLavender)))
            .drawBehind {
                fun blob(cx: Float, cy: Float, r: Float, c: Color) =
                    drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
                blob(size.width * 0.1f, size.height * 0.06f, size.width * 0.75f, Glass.Pink.copy(alpha = 0.55f))
                blob(size.width * 0.95f, size.height * 0.38f, size.width * 0.7f, Glass.Lavender.copy(alpha = 0.50f))
                blob(size.width * 0.15f, size.height * 0.88f, size.width * 0.8f, Glass.Lavender.copy(alpha = 0.40f))
            }
            .drawWithCache {
                val cell = 84.dp.toPx()
                val path = Path()
                var y = 0f
                while (y < size.height + cell) {
                    var x = 0f
                    while (x < size.width + cell) {
                        addStar(path, x + cell / 2, y + cell / 2, cell * 0.30f)
                        x += cell
                    }
                    y += cell
                }
                onDrawBehind {
                    drawPath(path, Glass.Purple.copy(alpha = 0.07f), style = Stroke(width = 1.1.dp.toPx()))
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

/** Shaffof oyna kartasi: yengil oq qatlam, nozik chegara, yuqori yorug'lik aksi va yumshoq soya. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    padding: Dp = 16.dp,
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    var m = modifier
        .shadow(8.dp, shape, ambientColor = Glass.Purple.copy(alpha = 0.10f), spotColor = Glass.Purple.copy(alpha = 0.18f))
        .clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.80f), tint.copy(alpha = 0.55f))))
        .border(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Glass.Lavender.copy(alpha = 0.55f), Color.White.copy(alpha = 0.75f))),
            shape,
        )
        .drawWithContent {
            drawContent()
            drawRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.Transparent), endY = size.height * 0.30f),
            )
        }
    if (onClick != null) m = m.clickable(onClick = onClick)
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
    val bg = if (filled) Brush.linearGradient(listOf(Glass.Purple, Glass.PurpleDeep))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Glass.PinkSoft.copy(alpha = 0.6f)))
    val border = if (filled) BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)) else BorderStroke(1.dp, Glass.Lavender)
    Row(
        modifier
            .heightIn(min = if (small) 40.dp else 54.dp)
            .shadow(if (enabled && filled) 6.dp else 0.dp, shape, ambientColor = Glass.Purple.copy(alpha = 0.2f), spotColor = Glass.Purple.copy(alpha = 0.3f))
            .clip(shape)
            .background(bg)
            .border(border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (filled) Color.White else Glass.PurpleDeep, modifier = Modifier.size(20.dp))
            Box(Modifier.size(8.dp))
        }
        Text(
            text,
            style = Type.BodyBold.copy(
                color = (if (filled) Color.White else Glass.PurpleDeep).copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = if (small) 14.sp else 16.sp,
            ),
        )
    }
}

/** Tanlanadigan "tabletka" (hafta kunlari, rejimlar). */
@Composable
fun GlassChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) Brush.linearGradient(listOf(Glass.Purple, Glass.PurpleDeep)) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.5f))))
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.6f) else Glass.Lavender.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.BodyBold.copy(fontSize = 14.sp, color = if (selected) Color.White else Glass.PurpleDeep))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.H2, modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp))
}

/** Hilol va masjid siluet (sarlavha bezagi). */
@Composable
fun CrescentMosqueArt(modifier: Modifier = Modifier) {
    Box(
        modifier.drawBehind {
            val w = size.width
            val h = size.height
            // Hilol
            val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.30f, 0f), Size(w * 0.55f, w * 0.55f))) }
            val inner = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(w * 0.45f, -w * 0.02f), Size(w * 0.5f, w * 0.5f))) }
            val crescent = Path.combine(PathOperation.Difference, outer, inner)
            drawPath(crescent, Brush.linearGradient(listOf(Glass.Lavender, Glass.Purple)), alpha = 0.95f)
            // Masjid silueti
            val base = h
            val sil = Path().apply {
                // gumbaz
                moveTo(w * 0.28f, base)
                lineTo(w * 0.28f, base - h * 0.28f)
                cubicTo(w * 0.28f, base - h * 0.62f, w * 0.72f, base - h * 0.62f, w * 0.72f, base - h * 0.28f)
                lineTo(w * 0.72f, base)
                close()
                // chap minora
                addRect(androidx.compose.ui.geometry.Rect(Offset(w * 0.14f, base - h * 0.55f), Size(w * 0.09f, h * 0.55f)))
                moveTo(w * 0.14f, base - h * 0.55f)
                lineTo(w * 0.185f, base - h * 0.70f)
                lineTo(w * 0.23f, base - h * 0.55f)
                close()
                // o'ng minora
                addRect(androidx.compose.ui.geometry.Rect(Offset(w * 0.77f, base - h * 0.55f), Size(w * 0.09f, h * 0.55f)))
                moveTo(w * 0.77f, base - h * 0.55f)
                lineTo(w * 0.815f, base - h * 0.70f)
                lineTo(w * 0.86f, base - h * 0.55f)
                close()
            }
            drawPath(sil, Brush.verticalGradient(listOf(Glass.Lavender.copy(alpha = 0.75f), Glass.Pink.copy(alpha = 0.55f))))
        },
    )
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Glass.Lavender.copy(alpha = 0.35f)))
}

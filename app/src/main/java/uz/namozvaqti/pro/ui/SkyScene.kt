package uz.namozvaqti.pro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import uz.namozvaqti.core.Gender
import uz.namozvaqti.core.SkyMath
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

/**
 * Bosh sahifa foni. Quyosh va oy HAQIQIY joyida (telefon koordinatasi va soatidan hisoblanadi, internet kerak emas):
 * ufqdan chiqadi, botadi; osmon rangi tong, kunduz, shafaq va tunga qarab silliq o'zgaradi; yulduzlar faqat
 * qorong'ida, masjid derazalari faqat shom va tunda yonadi. Namoz o'qiyotgan siluet foydalanuvchi jinsiga mos
 * (erkak: do'ppi, ayol: ro'mol) va unga quyosh/oy tomonidan soya tushadi.
 *
 * Hamma chizish "virtual" 360 birlik kenglikda; ekran kengligiga proporsional kattalashtiriladi.
 */
@Composable
fun SkyBackdrop(lat: Double, lon: Double, gender: Gender, minute: LocalDateTime, modifier: Modifier = Modifier) {
    val state = remember(minute, lat, lon) { SkyState.at(minute, lat, lon) }
    val sunPath = remember(minute.toLocalDate(), lat, lon) { SkyAssets.sunPath(minute, lat, lon) }
    Canvas(modifier.fillMaxSize()) {
        val s = size.width / 360f
        withTransform({ scale(s, s, Offset.Zero) }) {
            try {
                drawSky(state, sunPath, size.height / s, gender)
            } catch (e: Exception) {
                drawRect(Glass.Bg, size = Size(360f, size.height / s))
            }
        }
    }
}

/** Sarlavha uchun: osmon holati yozuvi ("Tun", "Kunduz"...) va hozir quyosh ufq ustidami. */
fun skyPhase(minute: LocalDateTime, lat: Double, lon: Double): Pair<String, Boolean> {
    val st = SkyState.at(minute, lat, lon)
    return st.label to (st.sunAlt > -1f)
}

/** Bosh sahifa sahnasi balandligi (virtual birlik): shisha kartalar shundan pastdan boshlanadi. */
const val SKY_SCENE_HEIGHT = 318f

// ------------------------------------------------------------------ holat

internal class SkyKey(val alt: Float, val top: Color, val mid: Color, val low: Color, val glow: Float)

private fun c(hex: Long) = Color(0xFF000000 or hex)

private val KEYS = listOf(
    SkyKey(-18f, c(0x04020A), c(0x0A0520), c(0x140A35), 0f),
    SkyKey(-10f, c(0x07031A), c(0x150A3D), c(0x2E1268), .25f),
    SkyKey(-5f, c(0x100644), c(0x2F1676), c(0x8F2F8E), .7f),
    SkyKey(-1f, c(0x1B0E5E), c(0x5A2A9C), c(0xEE6AA6), 1f),
    SkyKey(4f, c(0x241472), c(0x7040B4), c(0xFF9C85), 1f),
    SkyKey(12f, c(0x271A86), c(0x6A4CC8), c(0xE8A2DE), .5f),
    SkyKey(30f, c(0x2A2096), c(0x6A58D6), c(0xCFA6F2), 0f),
)

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)

private fun skyKeyAt(alt: Float): SkyKey {
    if (alt <= KEYS.first().alt) return KEYS.first()
    if (alt >= KEYS.last().alt) return KEYS.last()
    for (i in 0 until KEYS.size - 1) {
        val a = KEYS[i]
        val b = KEYS[i + 1]
        if (alt >= a.alt && alt < b.alt) {
            val k = (alt - a.alt) / (b.alt - a.alt)
            return SkyKey(alt, lerp(a.top, b.top, k), lerp(a.mid, b.mid, k), lerp(a.low, b.low, k), a.glow + (b.glow - a.glow) * k)
        }
    }
    return KEYS.last()
}

internal class SkyState(
    val sunAlt: Float,
    val sunAz: Float,
    val moonAlt: Float,
    val moonAz: Float,
    val moonPhase: Float,
    val hour: Float,
) {
    val key = skyKeyAt(sunAlt)
    val day = clamp01((sunAlt + 6f) / 20f)
    val lightK = clamp01((sunAlt + 8f) / 22f)
    val low = clamp01(1f - sunAlt / 25f)
    val starOp = clamp01((-3f - sunAlt) / 12f)
    val cloudOp = clamp01((sunAlt + 3f) / 14f) * .55f
    val sunOp = if (sunAlt > -6f) clamp01((sunAlt + 5f) / 6f) else 0f
    val moonOp = if (moonAlt > -3f) clamp01((moonAlt + 2f) / 5f) * (1f - .5f * day) else 0f

    /** Qisqa sarlavha: "Tun", "Quyosh chiqdi", "Kunduz"... */
    val label: String
        get() = when {
            sunAlt < -12f -> "Tun"
            sunAlt < -1f -> if (hour < 12f) "Tong oldidan" else "Shom shafaqi"
            sunAlt < 8f -> if (hour < 12f) "Quyosh chiqdi" else "Quyosh botmoqda"
            else -> if (hour < 12f) "Ertalab" else "Kunduz"
        }

    companion object {
        fun at(t: LocalDateTime, lat: Double, lon: Double): SkyState {
            val ms = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val sun = SkyMath.sunPosition(ms, lat, lon)
            val moon = SkyMath.moonPosition(ms, lat, lon)
            val ph = SkyMath.moonPhase(ms)
            return SkyState(
                sun.altitude.toFloat(), sun.azimuth.toFloat(), moon.altitude.toFloat(), moon.azimuth.toFloat(),
                ph.phase.toFloat(), t.hour + t.minute / 60f,
            )
        }
    }
}

// ------------------------------------------------------------------ tayyor shakllar

private object SkyAssets {
    private fun p(d: String): Path = PathParser().parsePathString(d).toPath()
    private fun r(x: Float, y: Float, w: Float, h: Float) = Path().apply { addRect(Rect(x, y, x + w, y + h)) }
    private fun rr(x: Float, y: Float, w: Float, h: Float) = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(x, y, x + w, y + h), androidx.compose.ui.geometry.CornerRadius(w / 2f)))
    }

    val mountainBack = p("M0 268L0 250L26 238L52 250L88 232L128 250L170 236L214 252L258 234L302 250L338 238L360 246V268Z")
    val mountainFront = p("M0 276L0 262L36 252L78 264L124 250L170 262L214 254L258 266L306 252L360 262V280Z")
    val midHill = p("M110 304C150 292 200 286 252 286S330 289 360 288V330H110Z")

    // ---- masjid (mahalliy koordinata: kenglik 240, taglik y=150)
    val halo: Path = Path.combine(
        PathOperation.Difference,
        Path().apply { addOval(Rect(120f - 70f, 50f - 70f, 120f + 70f, 50f + 70f)) },
        Path().apply { addOval(Rect(120f - 62f, 62f - 62f, 120f + 62f, 62f + 62f)) },
    )
    val mosqueBody = listOf(
        r(12f, 104f, 64f, 46f), r(164f, 104f, 64f, 46f),
        p("M14 106C14 88 25 77 45 72C65 77 76 88 76 106Z"), p("M164 106C164 88 175 77 195 72C215 77 226 88 226 106Z"),
        r(84f, 70f, 72f, 30f),
        p("M80 74C80 44 96 24 120 5C144 24 160 44 160 74Z"),
        r(92f, 86f, 56f, 64f), r(90f, 82f, 60f, 5f),
        p("M5 150L7.5 40L14.5 40L17 150Z"), r(1.5f, 58f, 19f, 4f), r(3.5f, 90f, 15f, 3.6f), r(5.2f, 34f, 11.6f, 7f), p("M6 34L11 10L16 34Z"),
        p("M223 150L225.5 40L232.5 40L235 150Z"), r(219.5f, 58f, 19f, 4f), r(221.5f, 90f, 15f, 3.6f), r(223.2f, 34f, 11.6f, 7f), p("M224 34L229 10L234 34Z"),
    )
    val ribs = listOf(
        "M120 6C114 26 108 50 106 74", "M120 6C126 26 132 50 134 74", "M120 6C108 24 96 46 91 74", "M120 6C132 24 144 46 149 74",
        "M45 74C42 80 40 90 40 104", "M45 74C48 80 50 90 50 104", "M195 74C192 80 190 90 190 104", "M195 74C198 80 200 90 200 104",
    ).map { p(it) }
    val bands = listOf("M84 72H156", "M92 84H148", "M12 106H76", "M164 106H228", "M1.5 60H20.5", "M219.5 60H238.5").map { p(it) }
    val finials = listOf("M120 5V-3", "M45 64V58", "M195 64V58", "M11 10V3", "M229 10V3").map { p(it) }
    val finialDots = listOf(Offset(120f, -5f) to 2.2f, Offset(45f, 56f) to 1.5f, Offset(195f, 56f) to 1.5f, Offset(11f, 1.5f) to 1.5f, Offset(229f, 1.5f) to 1.5f)
    val windows: List<Path> = buildList {
        add(p("M104 150V114C104 102 112 96 120 90C128 96 136 102 136 114V150Z"))
        for (x in listOf(96, 108, 117, 126, 138)) add(p("M$x 90V82a3 3 0 0 1 6 0v8z"))
        for (x in listOf(20, 34, 48, 62, 170, 184, 198, 212)) add(p("M$x 134V120a4 4 0 0 1 8 0v14z"))
        add(rr(9f, 70f, 4f, 9f)); add(rr(227f, 70f, 4f, 9f))
    }
    val door = p("M110 150V118C110 110 114 104 120 99C126 104 130 110 130 118V150Z")

    // ---- yulduzlar (deterministik)
    class Star(val x: Float, val y: Float, val r: Float, val a: Float)
    val stars: List<Star> = run {
        var seed = 7L
        fun rnd(): Float { seed = seed * 16807L % 2147483647L; return seed / 2147483647f }
        List(80) { val x = rnd() * 360f; val y = Math.pow(rnd().toDouble(), 1.5).toFloat() * 215f; Star(x, y, rnd() * 1.1f + .3f, (.4f + rnd() * .6f) * (1f - y / 300f)) }
    }
    val sparkles = listOf(Offset(205f, 58f) to 2f, Offset(338f, 150f) to 1.8f, Offset(30f, 196f) to 2f, Offset(96f, 120f) to 1.6f)

    /** Yerdagi sakkiz uchli yulduz naqshi (juda xira), bir marta quriladi. */
    val groundPattern: Path = Path().apply {
        val cell = 54f
        var y = 296f
        while (y < 1000f) {
            var x = 0f
            while (x < 380f) {
                val cx = x + 27f; val cy = y + 27f
                moveTo(cx, cy - 20f); lineTo(cx + 6f, cy - 6f); lineTo(cx + 20f, cy); lineTo(cx + 6f, cy + 6f)
                lineTo(cx, cy + 20f); lineTo(cx - 6f, cy + 6f); lineTo(cx - 20f, cy); lineTo(cx - 6f, cy - 6f); close()
                x += cell
            }
            y += cell
        }
    }

    fun sunPath(t: LocalDateTime, lat: Double, lon: Double): Path {
        val zone = ZoneId.systemDefault()
        val start = t.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val path = Path()
        var first = true
        var min = 0
        while (min <= 1440) {
            val pos = SkyMath.sunPosition(start + min * 60_000L, lat, lon)
            if (pos.altitude >= -0.5) {
                val x = skyX(pos.azimuth.toFloat()); val y = skyY(pos.altitude.toFloat())
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            }
            min += 6
        }
        return path
    }
}

private fun skyX(az: Float) = 180f + (az / 105f).coerceIn(-1.1f, 1.1f) * 172f
private fun skyY(alt: Float) = 228f - alt * (228f - 112f) / 42f

// ------------------------------------------------------------------ chizish

private fun DrawScope.softEllipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    withTransform({ translate(cx, cy); scale(rx, ry, Offset.Zero) }) {
        drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), Offset.Zero, 1f), 1f, Offset.Zero)
    }
}

private fun DrawScope.drawSky(st: SkyState, sunPath: Path, virtualH: Float, gender: Gender) {
    val k = st.key
    // osmon
    drawRect(
        Brush.verticalGradient(
            0f to k.top, .52f to k.mid, .82f to k.low, 1f to lerp(k.low, c(0x07030F), .4f),
            startY = 0f, endY = 340f,
        ),
        size = Size(360f, 340f),
    )
    if (st.starOp > 0.01f) {
        for (s in SkyAssets.stars) drawCircle(Color.White.copy(alpha = (s.a * st.starOp).coerceIn(0f, 1f)), s.r, Offset(s.x, s.y))
        for ((o, r) in SkyAssets.sparkles) {
            val sp = Path().apply {
                moveTo(o.x, o.y - r * 2.4f); lineTo(o.x + r * .5f, o.y - r * .5f); lineTo(o.x + r * 2.4f, o.y); lineTo(o.x + r * .5f, o.y + r * .5f)
                lineTo(o.x, o.y + r * 2.4f); lineTo(o.x - r * .5f, o.y + r * .5f); lineTo(o.x - r * 2.4f, o.y); lineTo(o.x - r * .5f, o.y - r * .5f); close()
            }
            drawPath(sp, c(0xFFD9EE).copy(alpha = .85f * st.starOp))
        }
    }
    if (st.cloudOp > 0.01f) {
        val cc = lerp(k.low, Color.White, .35f).copy(alpha = st.cloudOp)
        softEllipse(70f, 150f, 56f, 13f, cc); softEllipse(108f, 142f, 38f, 14f, cc)
        softEllipse(290f, 196f, 64f, 12f, cc); softEllipse(258f, 190f, 38f, 14f, cc); softEllipse(190f, 92f, 50f, 10f, cc)
    }
    if (k.glow > 0.01f) softEllipse(skyX(st.sunAz), 228f, 190f, 70f, lerp(c(0xFF9A6A), c(0xFF7AB8), .5f).copy(alpha = (k.glow * .85f).coerceIn(0f, 1f)))
    drawPath(
        sunPath, Color.White.copy(alpha = .22f * clamp01(.4f + st.day)),
        style = Stroke(1.2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f))),
    )
    if (st.moonOp > 0.01f) moonOrb(skyX(st.moonAz), skyY(st.moonAlt), st.moonPhase, st.moonOp)
    if (st.sunOp > 0.01f) sunOrb(skyX(st.sunAz), skyY(st.sunAlt), st.low, st.sunOp)

    withTransform({ translate(0f, -34f) }) { drawLandscape(st, virtualH + 40f, gender) }
}

private fun DrawScope.drawLandscape(st: SkyState, groundBottom: Float, gender: Gender) {
    val k = st.key
    val d = st.day
    val m1 = lerp(c(0x1A0A33), c(0x2A1766), d * .8f)
    val m2 = lerp(c(0x07030F), c(0x150A38), d * .8f)
    val f1 = lerp(c(0x4A1C80), lerp(k.low, c(0x5A3AA8), .5f), d * .7f)
    val f2 = lerp(c(0x2A0F55), c(0x3A2380), d * .6f)
    val far = Brush.verticalGradient(listOf(f1.copy(alpha = .9f), f2), startY = 232f, endY = 290f)
    drawPath(SkyAssets.mountainBack, far, alpha = .7f)
    drawPath(SkyAssets.mountainFront, far)
    drawPath(SkyAssets.midHill, Brush.verticalGradient(listOf(m1, m2), startY = 286f, endY = 330f), alpha = .9f)

    // ---- masjid (hilol nuri ichida)
    val windowOp = clamp01((3f - st.sunAlt) / 10f)
    withTransform({ translate(256f, 288f); scale(.74f, .74f, Offset.Zero); translate(-120f, -150f) }) {
        drawPath(SkyAssets.halo, Brush.verticalGradient(listOf(c(0xFFE0F0), c(0xA98BFF)), startY = -20f, endY = 120f), alpha = .62f)
        drawPath(SkyAssets.halo, c(0xFFB4DE), alpha = .22f, style = Stroke(7f, join = StrokeJoin.Round))
        val near = Brush.verticalGradient(listOf(m1, m2), startY = 0f, endY = 150f)
        val rim = Brush.verticalGradient(
            0f to c(0xFFB4DE).copy(alpha = .85f), .5f to c(0xFF8AC9).copy(alpha = .22f), 1f to Color.Transparent, startY = 0f, endY = 150f,
        )
        for (sh in SkyAssets.mosqueBody) {
            drawPath(sh, near)
            drawPath(sh, rim, style = Stroke(1.25f, join = StrokeJoin.Round))
        }
        for (sh in SkyAssets.ribs) drawPath(sh, c(0xE9DDFF).copy(alpha = .17f), style = Stroke(.6f, cap = StrokeCap.Round))
        for (sh in SkyAssets.bands) drawPath(sh, c(0xEBD3A2).copy(alpha = .42f), style = Stroke(.8f))
        for (sh in SkyAssets.finials) drawPath(sh, c(0xFFB4DE), style = Stroke(1.3f, cap = StrokeCap.Round))
        for ((o, r) in SkyAssets.finialDots) drawCircle(c(0xFFB4DE), r, o)
        if (windowOp > 0.01f) {
            val lit = c(0xFFC9E6)
            for (w in SkyAssets.windows) {
                drawPath(w, lit.copy(alpha = .22f * windowOp), style = Stroke(4f, join = StrokeJoin.Round))
                drawPath(w, lit.copy(alpha = .92f * windowOp))
            }
        }
        drawPath(SkyAssets.door, c(0x0A0418), alpha = .85f)
    }

    // ---- yer
    val ground = Path().apply {
        moveTo(0f, 300f)
        cubicTo(40f, 294f, 90f, 296f, 140f, 298f)
        cubicTo(190f, 300f, 250f, 301f, 300f, 297f)
        cubicTo(330f, 294f, 345f, 292f, 360f, 293f)
        lineTo(360f, groundBottom); lineTo(0f, groundBottom); close()
    }
    val g1 = lerp(c(0x12082A), lerp(c(0x2A1766), k.low, .2f), d * .6f + .1f)
    drawPath(ground, Brush.verticalGradient(0f to g1, .35f to g1, 1f to c(0x05020A), startY = 296f, endY = 296f + 484f))
    drawPath(SkyAssets.groundPattern, Brush.verticalGradient(listOf(c(0xC9B4FF).copy(alpha = .10f), Color.Transparent), startY = 296f, endY = 700f), style = Stroke(.8f))
    // quyosh/oy aksi (shaffof oynalar ostidan ko'rinadi)
    if (st.sunAlt > -6f) {
        val g = .32f * clamp01((st.sunAlt + 5f) / 8f)
        val sx = skyX(st.sunAz); val sy = skyY(st.sunAlt)
        drawCircle(Brush.radialGradient(listOf(c(0xFFB27A).copy(alpha = g), Color.Transparent), Offset(sx, 600f - sy), 86f), 86f, Offset(sx, 600f - sy))
    }
    if (st.moonAlt > -3f) {
        val mx = skyX(st.moonAz); val my = skyY(st.moonAlt)
        drawCircle(Brush.radialGradient(listOf(c(0xD9C8FF).copy(alpha = .5f * st.moonOp), Color.Transparent), Offset(mx, 600f - my), 64f), 64f, Offset(mx, 600f - my))
    }
    softEllipse(skyX(st.sunAz), 330f, 150f, 22f, lerp(k.low, c(0xFF9AD5), .3f).copy(alpha = (.16f + .2f * k.glow).coerceIn(0f, 1f)))
    drawPath(
        Path().apply {
            moveTo(0f, 300f); cubicTo(40f, 294f, 90f, 296f, 140f, 298f); cubicTo(190f, 300f, 250f, 301f, 300f, 297f); cubicTo(330f, 294f, 345f, 292f, 360f, 293f)
        },
        lerp(k.low, c(0xFF9AD5), .5f).copy(alpha = .5f), style = Stroke(1f),
    )

    drawFigure(st, gender)
}

// ------------------------------------------------------------------ quyosh va oy (suyuq shisha shar)

private fun DrawScope.sunOrb(x: Float, y: Float, low: Float, op: Float) {
    val glowR = 50f + low * 14f
    withTransform({ translate(x, y) }) {
        drawCircle(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = .85f * .62f * op),
                .3f to lerp(c(0xFFF0C8), c(0xFF8A6B), low).copy(alpha = .5f * .62f * op),
                1f to Color.Transparent, center = Offset.Zero, radius = glowR,
            ),
            glowR, Offset.Zero,
        )
        drawCircle(Color.White.copy(alpha = .13f * op), 17.6f)
        drawCircle(Color.White.copy(alpha = .62f * op), 17.6f, style = Stroke(1f))
        drawCircle(
            Brush.radialGradient(
                listOf(c(0xFFFBE9).copy(alpha = op), lerp(c(0xFFE9A8), c(0xFFB27A), low).copy(alpha = op), lerp(c(0xFFB070), c(0xFF6F8E), low).copy(alpha = .9f * op)),
                center = Offset(-6f, -8.5f), radius = 28f,
            ),
            15.7f,
        )
        drawArc(Color.White.copy(alpha = .5f * op), 20f, 140f, false, Offset(-14f, -14f), Size(28f, 28f), style = Stroke(1.3f, cap = StrokeCap.Round))
        withTransform({ rotate(-32f, Offset(-5f, -7.6f)) }) {
            drawOval(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = .9f * op), Color.Transparent), startY = -11.2f, endY = -4f),
                Offset(-12.6f, -11.2f), Size(15.2f, 7.2f),
            )
        }
        drawCircle(Color.White.copy(alpha = .55f * op), 2.2f, Offset(6.4f, 6.2f))
    }
}

private fun DrawScope.moonOrb(x: Float, y: Float, phase: Float, op: Float) {
    withTransform({ translate(x, y) }) {
        drawCircle(Brush.radialGradient(listOf(c(0xD9C8FF).copy(alpha = .5f * .8f * op), Color.Transparent), Offset.Zero, 40f), 40f, Offset.Zero)
        drawCircle(c(0xC8B9FF).copy(alpha = .16f * op), 15.8f)
        drawCircle(Color.White.copy(alpha = .55f * op), 15.8f, style = Stroke(1f))
        val r = 13f
        val kk = cos(2.0 * PI * phase).toFloat()
        val rx = maxOf(abs(kk) * r, .01f)
        val shape = Path().apply {
            arcTo(Rect(-r, -r, r, r), -90f, 180f, true)
            arcTo(Rect(-rx, -r, rx, r), 90f, if (kk > 0f) -180f else 180f, false)
            close()
        }
        withTransform({ scale(1.06f, 1.06f, Offset.Zero); if (phase >= .5f) scale(-1f, 1f, Offset.Zero) }) {
            drawPath(shape, Brush.linearGradient(listOf(c(0xFFFAFD), c(0xE8C8FF)), Offset(-r, -r), Offset(r, r)), alpha = .96f * op)
        }
        drawArc(Color.White.copy(alpha = .4f * op), 20f, 140f, false, Offset(-13.4f, -13.4f), Size(26.8f, 26.8f), style = Stroke(1.2f, cap = StrokeCap.Round))
        withTransform({ rotate(-32f, Offset(-5.4f, -8f)) }) {
            drawOval(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = .72f * op), Color.Transparent), startY = -11f, endY = -5f),
                Offset(-11.8f, -11f), Size(12.8f, 6f),
            )
        }
    }
}

// ------------------------------------------------------------------ namoz o'qiyotgan siluet (qiyom)

private class Joints(
    val head: Offset, val neck: Offset, val sh: Offset, val el: Offset, val hd: Offset, val hip: Offset, val kn: Offset, val an: Offset,
)

/** Qiyom (tik turib o'qish) holati; mahalliy koordinata 0..100, yer y=100, qibla (o'ng) tomonga qaragan. */
private val QIYOM = Joints(
    Offset(53f, 10f), Offset(51f, 19f), Offset(50f, 26f), Offset(53f, 42f), Offset(59f, 36f), Offset(49f, 56f), Offset(50f, 77f), Offset(50f, 97f),
)

private fun mid(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun DrawScope.drawFigureShapes(P: Joints, female: Boolean, brush: Brush, grow: Float) {
    fun line(a: Offset, b: Offset, w: Float) = drawLine(brush, a, b, w + grow, StrokeCap.Round)
    line(P.hip, P.kn, if (female) 15.5f else 13.4f)
    line(P.kn, P.an, if (female) 14.5f else 10.6f)
    line(P.hip, mid(P.hip, P.kn, .6f), if (female) 17.5f else 16.6f)
    line(P.hip, P.sh, if (female) 15.6f else 16f)
    line(mid(P.hip, P.sh, .5f), P.sh, if (female) 15.6f else 17.6f)
    drawCircle(brush, (if (female) 7f else 7.8f) + grow / 2f, P.sh)
    line(P.sh, P.neck, 5.4f)
    line(P.sh, P.el, if (female) 7.6f else 7.2f)
    line(P.el, P.hd, if (female) 6.4f else 5.8f)
    drawCircle(brush, 3.5f + grow / 2f, P.hd)
    // oyoq kafti
    drawOval(brush, Offset(P.an.x + 4f - 6.6f - grow / 2f, P.an.y + 1.6f - 2.7f - grow / 2f), Size(13.2f + grow, 5.4f + grow))
    // bosh
    val ux = P.head.x - P.neck.x; val uy = P.head.y - P.neck.y
    val ul = hypot(ux, uy); val u = Offset(ux / ul, uy / ul)
    val fv = Offset(-u.y, u.x)
    val ang = (atan2(u.y, u.x) * 180.0 / PI).toFloat() + 90f
    val rx = if (female) 9.4f else 7.3f
    val ry = if (female) 10.4f else 8.5f
    withTransform({ rotate(ang, P.head) }) {
        drawOval(brush, Offset(P.head.x - rx - grow / 2f, P.head.y - ry - grow / 2f), Size(rx * 2 + grow, ry * 2 + grow))
    }
    if (female) {
        // ro'mol: boshdan yelka va belgacha tushadi
        val w = mid(P.sh, P.hip, .62f)
        val a = Offset(P.head.x - u.x * 2f - fv.x * 8f, P.head.y - u.y * 2f - fv.y * 8f)
        val cc = Offset(P.neck.x + fv.x * 5.5f, P.neck.y + fv.y * 5.5f)
        val drape = Path().apply {
            moveTo(a.x, a.y); lineTo(w.x - fv.x * 9f, w.y - fv.y * 9f); lineTo(w.x + fv.x * 3f, w.y + fv.y * 3f); lineTo(cc.x, cc.y); close()
        }
        drawPath(drape, brush)
        drawPath(drape, brush, style = Stroke(4f + grow, join = StrokeJoin.Round))
    } else {
        // do'ppi
        withTransform({ translate(P.head.x, P.head.y); rotate(ang, Offset.Zero) }) {
            drawPath(DOPPI, brush)
            if (grow > 0f) drawPath(DOPPI, brush, style = Stroke(grow, join = StrokeJoin.Round))
        }
    }
}

private val DOPPI: Path by lazy {
    PathParser().parsePathString("M-8.2 -3.2C-9 -9.4 -6.8 -13 -3.6 -14.4C0 -15.6 5.4 -15.2 8.2 -13C9.6 -11 9.2 -6.4 8.4 -3C2.6 -4.8 -3.4 -4.6 -8.2 -3.2Z").toPath()
}

private fun DrawScope.drawFigure(st: SkyState, gender: Gender) {
    val female = gender == Gender.FEMALE
    val P = QIYOM
    val S = .78f
    val cx = 84f
    val gy = 100f
    // yorug'lik manbai: quyosh, bo'lmasa oy
    val la: Float; val laz: Float; val inten: Float
    when {
        st.sunAlt > 1f -> { la = st.sunAlt; laz = st.sunAz; inten = .6f * clamp01((st.sunAlt + 1f) / 10f) }
        st.moonAlt > 4f -> { la = st.moonAlt; laz = st.moonAz; inten = .26f * (1f - st.day) }
        else -> { la = 30f; laz = 0f; inten = 0f }
    }
    val len = minOf(1f / tan(maxOf(la, 4f) * PI.toFloat() / 180f), 2.8f)
    val n = .3f
    val sx = sin(laz * PI.toFloat() / 180f) * len
    val sy = cos(laz * PI.toFloat() / 180f) * len * n
    val sgn = if (sin(laz * PI.toFloat() / 180f) >= 0f) 1 else -1
    val rim = lerp(st.key.low, c(0xFFB4DE), .55f)
    val fl = lerp(c(0x2A1858), c(0x5A3FB0), st.day * .7f)
    val fd = lerp(c(0x0A0518), c(0x160B3A), st.day * .6f)
    val body = if (sgn > 0) Brush.horizontalGradient(listOf(fl, fd), startX = 96f, endX = 14f) else Brush.horizontalGradient(listOf(fl, fd), startX = 14f, endX = 96f)

    withTransform({ translate(cx - 50f * S, 297f - gy * S); scale(S, S, Offset.Zero) }) {
        drawMat()
        // yer bilan tegib turgan soya
        withTransform({ translate((P.an.x + P.hip.x) / 2f + 4f, 100.6f); scale(30f, 2.6f, Offset.Zero) }) {
            drawCircle(Brush.radialGradient(listOf(Color.Black.copy(alpha = .4f), Color.Transparent), Offset.Zero, 1f), 1f, Offset.Zero)
        }
        // quyosh/oy tushirgan soya (yerga yotgan, siljigan)
        if (inten > 0.01f) {
            val m = Matrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    sx, -sy, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    -sx * gy, gy * (1f + sy), 0f, 1f,
                ),
            )
            val layer = Paint().apply { alpha = inten }
            drawContext.canvas.saveLayer(Rect(-200f, -50f, 300f, 200f), layer)
            withTransform({ transform(m) }) { drawFigureShapes(P, female, SolidColor(Color.Black), 0f) }
            drawContext.canvas.restore()
        }
        // pushti kontur nuri va kontur
        val glow = Paint().apply { alpha = .28f }
        drawContext.canvas.saveLayer(Rect(-20f, -20f, 140f, 140f), glow)
        drawFigureShapes(P, female, SolidColor(rim), 7f)
        drawContext.canvas.restore()
        drawFigureShapes(P, female, SolidColor(rim), 2.6f)
        drawFigureShapes(P, female, body, 0f)
        // do'ppi naqshi yoki ro'mol burmasi
        val ux = P.head.x - P.neck.x; val uy = P.head.y - P.neck.y
        val ul = hypot(ux, uy)
        val ang = (atan2(uy / ul, ux / ul) * 180.0 / PI).toFloat() + 90f
        if (female) {
            val w = mid(P.sh, P.hip, .55f)
            val fx = -(uy / ul); val fy = ux / ul
            drawPath(
                Path().apply {
                    moveTo(P.head.x - fx * 5f - ux / ul * 4f, P.head.y - fy * 5f - uy / ul * 4f)
                    cubicTo(w.x - fx * 6f, w.y - fy * 6f - 16f, w.x - fx * 8f, w.y - fy * 8f - 6f, w.x - fx * 7f, w.y - fy * 7f)
                },
                c(0xB8A0FF).copy(alpha = .35f), style = Stroke(.7f, cap = StrokeCap.Round),
            )
        } else {
            withTransform({ translate(P.head.x, P.head.y); rotate(ang, Offset.Zero) }) {
                drawPath(Path().apply { moveTo(-7.6f, -4.4f); cubicTo(-2.8f, -5.6f, 3.4f, -5.6f, 8f, -4.4f) }, c(0xE9DDFF).copy(alpha = .85f), style = Stroke(.7f, cap = StrokeCap.Round))
                drawPath(Path().apply { moveTo(-7.2f, -8f); cubicTo(-2.6f, -9.2f, 3.2f, -9.2f, 7.6f, -8f) }, c(0xE9DDFF).copy(alpha = .55f), style = Stroke(.5f, cap = StrokeCap.Round))
                drawPath(Path().apply { moveTo(.4f, -13f); lineTo(2.4f, -11f); lineTo(.4f, -9f); lineTo(-1.6f, -11f); close() }, c(0xE9DDFF).copy(alpha = .9f))
            }
        }
    }
}

/** Joynamoz (sajjoda): pushti-siyohrang gilamcha, oltin chetli. */
private fun DrawScope.drawMat() {
    softEllipse(50f, 104f, 62f, 6f, c(0xFF7AB8).copy(alpha = .32f))
    val mat = Path().apply { moveTo(-4f, 99.4f); lineTo(104f, 99.4f); lineTo(114f, 104.6f); lineTo(-14f, 104.6f); close() }
    drawPath(mat, Brush.horizontalGradient(listOf(c(0xC8509E), c(0x8A4FE0), c(0xC8509E)), startX = -14f, endX = 114f))
    drawLine(c(0xFFE3A8).copy(alpha = .85f), Offset(-4f, 99.4f), Offset(104f, 99.4f), .9f)
    drawLine(c(0xFFE3A8).copy(alpha = .5f), Offset(-14f, 104.6f), Offset(114f, 104.6f), .7f)
    drawPath(
        Path().apply { moveTo(6f, 100.8f); lineTo(94f, 100.8f); lineTo(102f, 103.4f); lineTo(-2f, 103.4f); close() },
        c(0xFFE3A8).copy(alpha = .35f), style = Stroke(.5f),
    )
}

package uz.namozvaqti.pro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import uz.namozvaqti.core.UzbekScript

/** Tanlangan yozuv: false = lotin, true = kirill. Ilova ildizida beriladi. */
val LocalCyrillic = compositionLocalOf { false }

/**
 * Ilovadagi BARCHA matn shu orqali chiqadi: manba matn lotin (yoki rasmiy kirill) bo'lsa ham,
 * foydalanuvchi tanlagan yozuvda (lotin/kirill) ko'rsatiladi.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current,
) {
    val cyr = LocalCyrillic.current
    androidx.compose.material3.Text(
        text = UzbekScript.display(text, cyr),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}

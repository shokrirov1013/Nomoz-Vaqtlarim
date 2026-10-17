package uz.namozvaqti.pro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Qo'shimcha ikonkalar (material-icons-extended'siz, shuning uchun APK kichik). */
object TabIcons {
    /** Ochiq kitob (Suralar). */
    val Book: ImageVector by lazy {
        ImageVector.Builder("Book", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(11f, 6.2f)
                curveTo(9.2f, 4.9f, 6.6f, 4.4f, 4f, 4.8f)
                lineTo(4f, 18.2f)
                curveTo(6.6f, 17.9f, 9.2f, 18.3f, 11f, 19.6f)
                close()
                moveTo(13f, 6.2f)
                curveTo(14.8f, 4.9f, 17.4f, 4.4f, 20f, 4.8f)
                lineTo(20f, 18.2f)
                curveTo(17.4f, 17.9f, 14.8f, 18.3f, 13f, 19.6f)
                close()
            }
        }.build()
    }

    /** Hilol (Tahajjud). */
    val Crescent: ImageVector by lazy {
        ImageVector.Builder("Crescent", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 2f)
                curveToRelative(-1.05f, 0f, -2.05f, 0.16f, -3f, 0.46f)
                curveToRelative(4.06f, 1.27f, 7f, 5.06f, 7f, 9.54f)
                curveToRelative(0f, 4.48f, -2.94f, 8.27f, -7f, 9.54f)
                curveToRelative(0.95f, 0.3f, 1.95f, 0.46f, 3f, 0.46f)
                curveToRelative(5.52f, 0f, 10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(14.52f, 2f, 9f, 2f)
                close()
            }
        }.build()
    }
}

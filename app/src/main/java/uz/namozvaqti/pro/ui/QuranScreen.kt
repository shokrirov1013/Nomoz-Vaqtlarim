package uz.namozvaqti.pro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Suralar: kartani bosganda ochiladi/yopiladi. LazyColumn — faqat ko'rinadiganlari chiziladi (tez). */
@Composable
fun QuranScreen() {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Column(Modifier.padding(top = 4.dp)) {
                Text("Suralar", style = Type.H1)
                Text("Oyatal Kursi va qisqa suralar. Kartani bosing.", style = Type.Caption)
            }
        }
        items(QuranData.all, key = { it.id }) { surah ->
            SurahCard(surah, expanded = openId == surah.id) { openId = if (openId == surah.id) null else surah.id }
        }
        item(key = "note") {
            Text(
                "Matnni ishonchli mushaf bilan solishtirib o'qing. Ma'nolar qisqa tarjima, to'liq tafsir emas.",
                style = Type.Caption, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        item(key = "space") { Box(Modifier.height(100.dp)) }
    }
}

@Composable
private fun SurahCard(surah: Surah, expanded: Boolean, onToggle: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), tint = if (expanded) Glass.Tint else androidx.compose.ui.graphics.Color.White, onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(surah.title, style = Type.H2)
                Text(surah.source, style = Type.Caption)
            }
            Text(
                surah.arabicTitle,
                style = Type.H2.copy(fontSize = 22.sp, color = Glass.GoldBright, textDirection = TextDirection.Rtl),
            )
        }
        if (expanded) {
            Box(Modifier.height(10.dp))
            Divider()
            surah.verses.forEach { v ->
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(
                        v.arabic,
                        style = Type.Body.copy(fontSize = 26.sp, lineHeight = 46.sp, color = Glass.Ink, textDirection = TextDirection.Rtl),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        v.translit,
                        style = Type.Caption.copy(fontSize = 14.sp, fontStyle = FontStyle.Italic, color = Glass.GoldBright),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Divider()
            Text("Ma'nosi", style = Type.BodyBold.copy(color = Glass.Gold), modifier = Modifier.padding(top = 10.dp))
            Text(surah.meaning, style = Type.Body.copy(fontSize = 15.sp, lineHeight = 22.sp), modifier = Modifier.padding(top = 4.dp))
            Text(surah.tip, style = Type.Caption.copy(color = Glass.Mint), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

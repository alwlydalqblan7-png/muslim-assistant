package com.taifdigital.muslimassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val shortSurahs = listOf(
    "الإخلاص" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ هُوَ اللَّهُ أَحَدٌ\nاللَّهُ الصَّمَدُ\nلَمْ يَلِدْ وَلَمْ يُولَدْ\nوَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
    "الفلق" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ\nمِنْ شَرِّ مَا خَلَقَ\nوَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ\nوَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ\nوَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
    "الناس" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ\nمَلِكِ النَّاسِ\nإِلَٰهِ النَّاسِ\nمِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ\nالَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ\nمِنَ الْجِنَّةِ وَالنَّاسِ"
)

@Composable
fun OfflineQuranReading() {
    var selected by remember { mutableStateOf(0) }
    Text("سور قصيرة للقراءة دون إنترنت — ليس المصحف كاملًا")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        shortSurahs.forEachIndexed { index, entry ->
            TextButton(onClick = { selected = index }) { Text(entry.first) }
        }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Text(shortSurahs[selected].second, Modifier.padding(18.dp), fontSize = 21.sp, lineHeight = 39.sp)
    }
    Text("المصحف الكامل والاستماع والتنزيل قيد التطوير.")
}

@Composable
fun DailyDhikrCards() {
    listOf(
        "أستغفر الله العظيم وأتوب إليه",
        "سبحان الله وبحمده، سبحان الله العظيم",
        "لا حول ولا قوة إلا بالله",
        "اللهم صل وسلم على نبينا محمد"
    ).forEach { dhikr ->
        ElevatedCard(Modifier.fillMaxWidth()) { Text(dhikr, Modifier.padding(16.dp), fontSize = 18.sp) }
    }
}

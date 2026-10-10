package com.taifdigital.muslimassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val shortSurahs = listOf(
    "الإخلاص" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ هُوَ اللَّهُ أَحَدٌ\nاللَّهُ الصَّمَدُ\nلَمْ يَلِدْ وَلَمْ يُولَدْ\nوَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
    "الفلق" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ\nمِنْ شَرِّ مَا خَلَقَ\nوَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ\nوَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ\nوَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
    "الناس" to "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ\nمَلِكِ النَّاسِ\nإِلَٰهِ النَّاسِ\nمِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ\nالَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ\nمِنَ الْجِنَّةِ وَالنَّاسِ"
)

@Composable
fun OfflineQuranReading() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("quran_reader", 0) }
    var selected by remember { mutableIntStateOf(prefs.getInt("last_surah", 0).coerceIn(0, shortSurahs.lastIndex)) }
    var fontSize by remember { mutableIntStateOf(prefs.getInt("font_size", 21).coerceIn(18, 32)) }
    Text("سور قصيرة للقراءة دون إنترنت — ليس المصحف كاملًا")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        shortSurahs.forEachIndexed { index, entry ->
            TextButton(onClick = {
                selected = index
                prefs.edit().putInt("last_surah", index).apply()
            }) { Text(entry.first) }
        }
    }
    Text("آخر سورة قرأتها: ${shortSurahs[selected].first}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            fontSize = (fontSize - 2).coerceAtLeast(18)
            prefs.edit().putInt("font_size", fontSize).apply()
        }, enabled = fontSize > 18) { Text("تصغير الخط") }
        OutlinedButton(onClick = {
            fontSize = (fontSize + 2).coerceAtMost(32)
            prefs.edit().putInt("font_size", fontSize).apply()
        }, enabled = fontSize < 32) { Text("تكبير الخط") }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Text(shortSurahs[selected].second, Modifier.padding(18.dp), fontSize = fontSize.sp, lineHeight = (fontSize * 1.8f).sp)
    }
    Text("يحفظ التطبيق السورة الأخيرة وحجم الخط على هذا الجهاز فقط.")
    Text("المصحف الكامل والاستماع والتنزيل قيد التطوير.")
}

@Composable
fun DailyDhikrCards() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("dhikr_counter", 0) }
    val today = java.time.LocalDate.now().toString()
    val entries = listOf(
        "أستغفر الله العظيم وأتوب إليه",
        "سبحان الله وبحمده، سبحان الله العظيم",
        "لا حول ولا قوة إلا بالله",
        "اللهم صل وسلم على نبينا محمد"
    )
    var counts by remember(today) {
        mutableStateOf(List(entries.size) { index ->
            if (prefs.getString("day", "") == today) prefs.getInt("count_$index", 0).coerceAtLeast(0) else 0
        })
    }
    fun save(index: Int, value: Int) {
        counts = counts.toMutableList().also { it[index] = value }
        prefs.edit().putString("day", today).putInt("count_$index", value).apply()
    }
    Text("عداد الأذكار اليومي — يُصفّر تلقائيًا في اليوم التالي")
    entries.forEachIndexed { index, dhikr ->
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dhikr, fontSize = 18.sp)
                Text("عدد المرات اليوم: ${counts[index]}", fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (counts[index] < 100000) save(index, counts[index] + 1) }) { Text("+1") }
                    OutlinedButton(onClick = { save(index, 0) }, enabled = counts[index] > 0) { Text("تصفير") }
                }
            }
        }
    }
}

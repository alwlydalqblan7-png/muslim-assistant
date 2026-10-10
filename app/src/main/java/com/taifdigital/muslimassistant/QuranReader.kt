package com.taifdigital.muslimassistant

import android.content.Context
import android.util.Xml
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

data class QuranSurah(val number: Int, val name: String, val verses: List<String>, val bismillah: String?)

object QuranRepository {
    @Volatile private var cached: List<QuranSurah>? = null
    @Synchronized fun read(context: Context): List<QuranSurah> {
        cached?.let { return it }
        val surahs = mutableListOf<QuranSurah>()
        context.assets.open("quran-uthmani.xml").use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, "UTF-8")
            var number = 0
            var name = ""
            var bismillah: String? = null
            var verses = mutableListOf<String>()
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                    "sura" -> {
                        number = parser.getAttributeValue(null, "index").toInt()
                        name = parser.getAttributeValue(null, "name")
                        verses = mutableListOf()
                        bismillah = null
                    }
                    "aya" -> {
                        check(parser.getAttributeValue(null, "index").toInt() == verses.size + 1)
                        verses.add(parser.getAttributeValue(null, "text"))
                        parser.getAttributeValue(null, "bismillah")?.let { bismillah = it }
                    }
                }
                if (parser.eventType == XmlPullParser.END_TAG && parser.name == "sura") {
                    check(number == surahs.size + 1)
                    surahs.add(QuranSurah(number, name, verses.toList(), bismillah))
                }
                parser.next()
            }
        }
        check(surahs.size == 114 && surahs.sumOf { it.verses.size } == 6236)
        return surahs.toList().also { cached = it }
    }
}

@Composable
fun OfflineQuranReading() {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val prefs = remember { context.getSharedPreferences("quran_reader", 0) }
    var selected by remember { mutableIntStateOf(prefs.getInt("surah_number", if (prefs.contains("last_surah")) 112 + prefs.getInt("last_surah", 0).coerceIn(0, 2) else 1).coerceIn(1, 114)) }
    var start by remember { mutableIntStateOf(prefs.getInt("verse_start", 0).coerceAtLeast(0)) }
    var fontSize by remember { mutableIntStateOf(prefs.getInt("font_size", 21).coerceIn(18, 32)) }
    var data by remember { mutableStateOf<List<QuranSurah>>(emptyList()) }
    var error by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { runCatching { QuranRepository.read(context) } }
        result.onSuccess { data = it }.onFailure { error = true }
    }
    if (error) { Text("تعذّر فتح نص المصحف المحلي. أعد فتح التطبيق؛ لا يتم عرض نص بديل."); return }
    if (data.isEmpty()) { Text("جارٍ فتح المصحف المحلي…"); return }
    val surah = data[selected - 1]
    val first = start.coerceIn(0, surah.verses.lastIndex)
    fun position(number: Int, verse: Int = 0) {
        selected = number; start = verse
        prefs.edit().putInt("surah_number", number).putInt("verse_start", verse).apply()
    }
    Text("المصحف كاملًا: 114 سورة — قراءة دون إنترنت")
    Button(onClick = { picker = true }) { Text("اختيار سورة: ${surah.number}. ${surah.name}") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(18); prefs.edit().putInt("font_size", fontSize).apply() }, enabled = fontSize > 18) { Text("تصغير") }
        OutlinedButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(32); prefs.edit().putInt("font_size", fontSize).apply() }, enabled = fontSize < 32) { Text("تكبير") }
    }
    Text("الآيات ${first + 1}–${minOf(first + 12, surah.verses.size)} من ${surah.verses.size} — يُحفظ هذا الموضع تلقائيًا")
    if (first == 0) surah.bismillah?.let { Text(it, fontSize = fontSize.sp, lineHeight = (fontSize * 1.8).sp) }
    surah.verses.drop(first).take(12).forEachIndexed { offset, verse ->
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(verse, fontSize = fontSize.sp, lineHeight = (fontSize * 1.8).sp)
                Text("﴿${first + offset + 1}﴾")
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { if (first > 0) position(selected, (first - 12).coerceAtLeast(0)) else position(selected - 1) }, enabled = first > 0 || selected > 1) { Text("السابق") }
        Button(onClick = { if (first + 12 < surah.verses.size) position(selected, first + 12) else position(selected + 1) }, enabled = first + 12 < surah.verses.size || selected < 114) { Text("التالي") }
    }
    Text("نص عثماني من مشروع تنزيل Tanzil، الإصدار 1.1. النص محفوظ دون تغيير. العرض بالآيات وليس صفحات طبعة ورقية. الاستماع غير متاح في هذا الإصدار.")
    TextButton(onClick = { uri.openUri("https://tanzil.net") }) { Text("المصدر: Tanzil.net") }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, confirmButton = { TextButton(onClick = { picker = false }) { Text("إغلاق") } }, title = { Text("فهرس السور") }, text = {
        Column {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("اسم السورة أو رقمها") })
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(data.filter { query.isBlank() || it.name.contains(query.trim()) || it.number.toString() == query.trim() }, key = { it.number }) { item ->
                    TextButton(onClick = { position(item.number); picker = false }) { Text("${item.number}. ${item.name} — ${item.verses.size} آية") }
                }
            }
        }
    })
}

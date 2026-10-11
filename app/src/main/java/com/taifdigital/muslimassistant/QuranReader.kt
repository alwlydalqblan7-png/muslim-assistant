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

data class MushafPageStart(val surah: Int, val verse: Int)

object MushafPages {
    @Volatile private var cache: List<MushafPageStart>? = null
    @Synchronized fun read(context: Context): List<MushafPageStart> {
        cache?.let { return it }
        val result = mutableListOf<MushafPageStart>()
        context.assets.open("quran-metadata.xml").use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, "UTF-8")
            var inPages = false
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "pages") inPages = true
                    if (inPages && parser.name == "page") {
                        check(parser.getAttributeValue(null, "index").toInt() == result.size + 1)
                        result.add(MushafPageStart(
                            parser.getAttributeValue(null, "sura").toInt(),
                            parser.getAttributeValue(null, "aya").toInt()
                        ))
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "pages") {
                    inPages = false
                }
                parser.next()
            }
        }
        check(result.size == 604)
        check(result.first() == MushafPageStart(1, 1))
        check(result.last() == MushafPageStart(112, 1))
        return result.toList().also { cache = it }
    }
}

@Composable
fun OfflineQuranReading() {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val prefs = remember(context) { context.getSharedPreferences("quran_reader", 0) }
    var fontSize by remember { mutableIntStateOf(prefs.getInt("font_size", 21).coerceIn(18, 32)) }
    var data by remember { mutableStateOf<List<QuranSurah>>(emptyList()) }
    var pages by remember { mutableStateOf<List<MushafPageStart>>(emptyList()) }
    var error by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var page by remember {
        mutableIntStateOf(prefs.getInt("mushaf_page", 1).coerceIn(1, 604))
    }
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching { QuranRepository.read(context) to MushafPages.read(context) }
        }
        result.onSuccess { (surahs, starts) -> data = surahs; pages = starts }
            .onFailure { error = true }
    }
    if (error) {
        Text("تعذّر فتح بيانات المصحف المحلية. لا يُعرض نص بديل.")
        return
    }
    if (data.isEmpty() || pages.isEmpty()) {
        Text("جارٍ فتح صفحات المصحف…")
        return
    }
    fun goToPage(number: Int) {
        page = number.coerceIn(1, 604)
        prefs.edit().putInt("mushaf_page", page).apply()
    }
    fun pageFor(surah: Int, verse: Int): Int {
        val index = pages.indexOfLast { it.surah < surah || (it.surah == surah && it.verse <= verse) }
        return (index + 1).coerceIn(1, 604)
    }
    val start = pages[page - 1]
    val end = pages.getOrNull(page)
    val verses = buildList {
        for (surah in data) {
            if (surah.number < start.surah) continue
            if (end != null && surah.number > end.surah) break
            for ((index, verse) in surah.verses.withIndex()) {
                val verseNumber = index + 1
                if (surah.number == start.surah && verseNumber < start.verse) continue
                if (end != null && surah.number == end.surah && verseNumber >= end.verse) break
                add(Triple(surah, verseNumber, verse))
            }
        }
    }
    Text("المصحف الشريف — 604 صفحات دون إنترنت", style = MaterialTheme.typography.titleLarge)
    Text("الصفحة $page من 604")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(18); prefs.edit().putInt("font_size", fontSize).apply() }, enabled = fontSize > 18) { Text("تصغير") }
        OutlinedButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(32); prefs.edit().putInt("font_size", fontSize).apply() }, enabled = fontSize < 32) { Text("تكبير") }
        TextButton(onClick = { picker = true }) { Text("فهرس السور") }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var previousSurah = 0
            verses.forEach { (surah, number, verse) ->
                if (previousSurah != surah.number) {
                    Text("سورة ${surah.name}", style = MaterialTheme.typography.titleMedium)
                    if (number == 1) surah.bismillah?.let {
                        Text(it, fontSize = fontSize.sp, lineHeight = (fontSize * 1.8).sp)
                    }
                    previousSurah = surah.number
                }
                Text("$verse ﴿$number﴾",
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.9).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Justify)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { goToPage(page - 1) }, enabled = page > 1) { Text("الصفحة السابقة") }
        Button(onClick = { goToPage(page + 1) }, enabled = page < 604) { Text("الصفحة التالية") }
    }
    Text("توزيع الآيات حسب بيانات صفحات Tanzil. يتغير ارتفاع العرض حسب حجم الخط؛ لا يحاكي سطور الطبعة الورقية حرفيًا.")
    TextButton(onClick = { uri.openUri("https://tanzil.net") }) { Text("المصدر: Tanzil.net") }
    if (picker) AlertDialog(
        onDismissRequest = { picker = false },
        confirmButton = { TextButton(onClick = { picker = false }) { Text("إغلاق") } },
        title = { Text("فهرس السور") },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("اسم السورة أو رقمها") })
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(data.filter {
                        query.isBlank() || it.name.contains(query.trim()) || it.number.toString() == query.trim()
                    }, key = { it.number }) { surah ->
                        TextButton(onClick = {
                            goToPage(pageFor(surah.number, 1))
                            picker = false
                        }) { Text("${surah.number}. ${surah.name}") }
                    }
                }
            }
        }
    )
}

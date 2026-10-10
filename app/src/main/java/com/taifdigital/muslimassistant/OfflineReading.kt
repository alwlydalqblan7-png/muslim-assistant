package com.taifdigital.muslimassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DailyDhikrCards() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("dhikr_counter", 0) }
    var today by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            val currentDate = java.time.LocalDate.now().toString()
            if (currentDate != today) today = currentDate
        }
    }
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
        val actualDay = java.time.LocalDate.now().toString()
        val newDay = actualDay != today
        if (newDay) today = actualDay
        val nextValue = if (newDay && value > 0) 1 else value
        counts = (if (newDay) List(entries.size) { 0 } else counts).toMutableList().also { it[index] = nextValue }
        val editor = prefs.edit()
        if (prefs.getString("day", "") != today) {
            entries.indices.forEach { editor.remove("count_$it") }
        }
        editor.putString("day", today).putInt("count_$index", nextValue).apply()
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

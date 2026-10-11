package com.taifdigital.muslimassistant

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** User-reported daily worship tracker. No automatic assumptions about prayer completion. */
private val routineTasks = listOf(
    "صلاة الفجر", "صلاة الظهر", "صلاة العصر", "صلاة المغرب", "صلاة العشاء",
    "أذكار الصباح", "أذكار المساء", "ورد القرآن", "أذكار النوم"
)

@Composable
fun DailyWorshipRoutine() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("worship_routine", Context.MODE_PRIVATE) }
    var day by remember { mutableStateOf(LocalDate.now().toString()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            val current = LocalDate.now().toString()
            if (day != current) day = current
        }
    }
    var completed by remember(day) {
        mutableStateOf(routineTasks.indices.map { prefs.getBoolean("$day.task.$it", false) })
    }
    val total = completed.count { it }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("روتيني الإيماني", style = MaterialTheme.typography.titleLarge)
            Text("متابعة شخصية اختيارية — سجّل ما أنجزته بنفسك")
            Text("إنجاز اليوم: $total من ${routineTasks.size}")
            LinearProgressIndicator(
                progress = { total.toFloat() / routineTasks.size },
                modifier = Modifier.fillMaxWidth()
            )
            routineTasks.forEachIndexed { index, task ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(task, modifier = Modifier.weight(1f))
                    Checkbox(checked = completed[index], onCheckedChange = { checked ->
                        completed = completed.toMutableList().also { it[index] = checked }
                        prefs.edit().putBoolean("$day.task.$index", checked).apply()
                    })
                }
            }
            Text("العلامات محفوظة على هذا الجهاز حسب التاريخ، ولا تُعد حكمًا على العبادة.")
        }
    }
}

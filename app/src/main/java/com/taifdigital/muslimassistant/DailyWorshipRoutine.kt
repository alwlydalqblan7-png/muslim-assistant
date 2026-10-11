package com.taifdigital.muslimassistant

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.delay

private val routineTasks = listOf(
    "صلاة الفجر", "صلاة الظهر", "صلاة العصر", "صلاة المغرب", "صلاة العشاء",
    "أذكار الصباح", "أذكار المساء", "ورد القرآن", "أذكار النوم"
)

/** Voluntary, self-reported history. An empty day does not imply missed worship. */
@Composable
fun DailyWorshipRoutine() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("worship_routine", Context.MODE_PRIVATE) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            val now = LocalDate.now()
            if (today != now) {
                if (selectedDay == today) selectedDay = now
                today = now
            }
        }
    }
    val completed = remember(selectedDay, revision) {
        routineTasks.indices.map { prefs.getBoolean("$selectedDay.task.$it", false) }
    }
    fun summary(days: Int): Pair<Int, Int> {
        val dates = (0 until days).map { today.minusDays(it.toLong()) }
        val recordedDays = dates.count { date ->
            routineTasks.indices.any { prefs.contains("$date.task.$it") }
        }
        val completedTasks = dates.sumOf { date ->
            routineTasks.indices.count { prefs.getBoolean("$date.task.$it", false) }
        }
        return completedTasks to recordedDays
    }
    val week = remember(today, revision) { summary(7) }
    val month = remember(today, revision) { summary(30) }
    val total = completed.count { it }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("روتيني الإيماني", style = MaterialTheme.typography.titleLarge)
            Text("متابعة شخصية اختيارية — سجّل ما أنجزته بنفسك")
            Text("إنجاز ${if (selectedDay == today) "اليوم" else selectedDay}: $total من ${routineTasks.size}")
            LinearProgressIndicator(
                progress = { total.toFloat() / routineTasks.size },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { selectedDay = selectedDay.minusDays(1) },
                    enabled = selectedDay.isAfter(today.minusDays(29))
                ) { Text("السابق") }
                OutlinedButton(
                    onClick = { selectedDay = selectedDay.plusDays(1) },
                    enabled = selectedDay.isBefore(today)
                ) { Text("التالي") }
                TextButton(onClick = { selectedDay = today }) { Text("اليوم") }
            }
            routineTasks.forEachIndexed { index, task ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(task, modifier = Modifier.weight(1f))
                    Checkbox(checked = completed[index], onCheckedChange = { checked ->
                        prefs.edit().putBoolean("$selectedDay.task.$index", checked).apply()
                        revision++
                    })
                }
            }
            HorizontalDivider()
            Text("آخر 7 أيام: ${week.first} نشاطًا مسجّلًا في ${week.second} يوم متابعة")
            Text("آخر 30 يومًا: ${month.first} نشاطًا مسجّلًا في ${month.second} يوم متابعة")
            Text("عدم التسجيل في يوم لا يعني عدم أداء العبادات.")
            Text("يمكن تعديل آخر 30 يومًا. البيانات محفوظة محليًا ولا تقيّم صحة العبادة.")
        }
    }
}

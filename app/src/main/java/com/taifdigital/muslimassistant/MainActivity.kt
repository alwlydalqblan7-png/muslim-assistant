package com.taifdigital.muslimassistant

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val emerald = Color(0xFF075C4B)
private val gold = Color(0xFFD4AF37)
private val cream = Color(0xFFF8F4E9)
private val deepGreen = Color(0xFF102D29)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MuslimAssistant() }
    }
}

@Composable
fun MuslimAssistant() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("preferences", 0) }
    var page by remember { mutableStateOf("الرئيسية") }
    var adhan by remember { mutableStateOf(prefs.getBoolean("adhan", true)) }
    var testFeedback by remember { mutableStateOf("") }
    var prayerAlerts by remember { mutableStateOf(prefs.getBoolean("prayer_alerts", false)) }
    var dhikr by remember { mutableStateOf(prefs.getBoolean("dhikr", false)) }
    var pendingPermission by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingPermission == "prayer") {
            prayerAlerts = true
            prefs.edit().putBoolean("prayer_alerts", true).apply()
            PrayerAlerts.schedule(context)
        } else if (granted && pendingPermission == "dhikr") {
            dhikr = true
            prefs.edit().putBoolean("dhikr", true).apply()
            DhikrAlerts.schedule(context)
        }
        pendingPermission = ""
    }
    LaunchedEffect(Unit) {
        PrayerAlerts.schedule(context)
        DhikrAlerts.schedule(context)
    }
    var sound by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var quiet by remember { mutableStateOf(prefs.getBoolean("quiet", true)) }
    var voice by remember { mutableStateOf(prefs.getString("voice", "makkah") ?: "makkah") }

    var cityName by remember { mutableStateOf(prefs.getString("prayer_city", "مكة المكرمة") ?: "مكة المكرمة") }
    var method by remember { mutableStateOf(prefs.getString("prayer_method", "MWL") ?: "MWL") }
    var prayerOffset by remember { mutableIntStateOf(prefs.getInt("prayer_offset_minutes", 0).coerceIn(-30, 30)) }
    val city = PrayerTimes.cities.firstOrNull { it.name == cityName } ?: PrayerTimes.cities.first()
    var clockTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            clockTick++
        }
    }
    val today = remember(cityName, clockTick) { ZonedDateTime.now(ZoneId.of(city.zone)) }
    val angles = if (method == "EGYPT") 19.5 to 17.5 else 18.0 to 17.0
    val prayers = remember(cityName, method, prayerOffset, today.toLocalDate()) {
        PrayerTimes.calculate(today.toLocalDate(), city, angles.first, angles.second).map { it.copy(time = it.time.plusMinutes(prayerOffset.toLong())) }
    }
    val upcoming = prayers.map { prayer ->
        prayer to today.toLocalDate().atTime(prayer.time).atZone(ZoneId.of(city.zone)).toInstant()
    }.filter { it.second.isAfter(today.toInstant()) }.minByOrNull { it.second }
    val tomorrowFajr = remember(cityName, method, prayerOffset, today.toLocalDate()) {
        PrayerTimes.calculate(today.toLocalDate().plusDays(1), city, angles.first, angles.second)
            .firstOrNull()?.let { it.copy(time = it.time.plusMinutes(prayerOffset.toLong())) }
    }
    val nextPrayer = upcoming?.first ?: tomorrowFajr
    val nextInstant = upcoming?.second ?: tomorrowFajr?.let {
        today.toLocalDate().plusDays(1).atTime(it.time).atZone(ZoneId.of(city.zone)).toInstant()
    }
    val remaining = nextInstant?.let { Duration.between(today.toInstant(), it).coerceAtLeast(Duration.ZERO) }
    val countdown = remaining?.let { "%02d:%02d:%02d".format(it.toHours(), it.toMinutes() % 60, it.seconds % 60) } ?: "—"
    val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = emerald, secondary = gold, background = cream, surface = Color.White)) {
            Scaffold(
                topBar = {
                    Surface(color = emerald) {
                        Column(Modifier.fillMaxWidth().padding(22.dp)) {
                            Text("☪ مساعد المسلم", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text("TAIF DIGITAL  •  رفيق المسلم اليومي", color = gold, fontSize = 12.sp)
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = cream) {
                        listOf("الرئيسية", "المؤذن", "الأذكار", "القرآن", "الإعدادات").forEach { label ->
                            NavigationBarItem(
                                selected = page == label,
                                onClick = { page = label },
                                icon = { Text(when (label) { "الرئيسية" -> "⌂"; "المؤذن" -> "◷"; "الأذكار" -> "♡"; "القرآن" -> "▤"; else -> "⚙" }, fontSize = 22.sp, color = if (page == label) emerald else deepGreen) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (page) {
                        "الرئيسية" -> {
                            Text("السلام عليكم ورحمة الله", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = emerald)
                            Text("يومك عامر بذكر الله", color = deepGreen)
                            Card(colors = CardDefaults.cardColors(containerColor = deepGreen)) {
                                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("☪  الصلاة القادمة", color = gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Text(if (nextPrayer != null) "القادمة: ${nextPrayer.name}" else "مواقيت غير متاحة", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text("المتبقي: $countdown", color = gold, fontSize = 20.sp)
                                    Text("المدينة: ${city.name} — توقيت تقريبي", color = cream)
                                    if (prayers.isEmpty()) Text("تعذّر حساب المواقيت لهذه المدينة والتاريخ", color = cream)
                                    prayers.forEach { prayer ->
                                        Text("${prayer.name}: ${prayer.time.format(timeFormat)}", color = Color.White)
                                    }
                                    HorizontalDivider(color = gold)
                                    Text("الفجر   •   الظهر   •   العصر   •   المغرب   •   العشاء", color = Color.White, fontSize = 12.sp)
                                }
                            }
                            listOf("المؤذن" to "◷  المؤذن الذكي", "الأذكار" to "♡  الأذكار اليومية", "القرآن" to "▤  القرآن الكريم", "الإعدادات" to "⚙  الإعدادات").forEach { (destination, label) ->
                                ElevatedCard(onClick = { page = destination }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
                                    Text(label, modifier = Modifier.padding(20.dp), color = emerald, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5EEE7))) {
                                Text("✦  سبحان الله وبحمده، سبحان الله العظيم", modifier = Modifier.fillMaxWidth().padding(18.dp), color = emerald)
                            }
                        }
                        "المؤذن" -> {
                            Text("المؤذن الذكي", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            SettingSwitch("تفضيل الأذان الصوتي (غير مفعّل بعد)", adhan) {
                                adhan = it; prefs.edit().putBoolean("adhan", it).apply()
                            }
                            SettingSwitch("تنبيهات الصلاة (قد تتأخر بسبب توفير البطارية)", prayerAlerts) { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    pendingPermission = "prayer"
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    prayerAlerts = enabled
                                    prefs.edit().putBoolean("prayer_alerts", enabled).apply()
                                    PrayerAlerts.schedule(context)
                                }
                            }
                            Text("التنبيهات إشعارات فقط حاليًا، وليست أذانًا صوتيًا كاملًا.", color = Color.Gray)
                            OutlinedButton(onClick = {
                                testFeedback = if (PrayerAlerts.testNotification(context)) "تم إرسال إشعار تجريبي؛ تحقق من لوحة الإشعارات." else "اسمح بالإشعارات من إعدادات أندرويد أولًا."
                            }) { Text("تجربة إشعار الصلاة الآن") }
                            if (testFeedback.isNotEmpty()) Text(testFeedback, color = emerald)
                            Text("اختيار صوت المؤذن (محفوظ للتحديث الصوتي القادم)")
                            listOf("makkah" to "مؤذن الحرم المكي", "madinah" to "مؤذن المسجد النبوي", "other" to "صوت آخر").forEach { (id, label) ->
                                Row {
                                    RadioButton(selected = voice == id, onClick = {
                                        voice = id; prefs.edit().putString("voice", id).apply()
                                    })
                                    Text(label, modifier = Modifier.padding(top = 12.dp))
                                }
                            }
                            Text("الفجر • الظهر • العصر • المغرب • العشاء")
                            Text("تنبيهات الصلاة إشعارات محلية تجريبية. الأذان الصوتي الكامل غير متاح بعد.", color = Color.Gray)
                        }
                        "الأذكار" -> {
                            Text("الأذكار اليومية", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            DailyDhikrCards()
                            SettingSwitch("التذكير كل 60 دقيقة", dhikr) {
                                if (it && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    pendingPermission = "dhikr"
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    dhikr = it; prefs.edit().putBoolean("dhikr", it).apply()
                                    DhikrAlerts.schedule(context)
                                }
                            }
                            SettingSwitch("صوت الذكر", sound) {
                                sound = it; prefs.edit().putBoolean("sound", it).apply()
                            }
                            SettingSwitch("الهدوء أثناء النوم", quiet) {
                                quiet = it; prefs.edit().putBoolean("quiet", it).apply()
                            }
                            Text("التذكير الدوري يعمل بإشعارات تقريبية وقد يتأخر مع توفير البطارية. الهدوء من 10 مساءً إلى 7 صباحًا.", color = Color.Gray)
                        }
                        "القرآن" -> {
                            Text("القرآن الكريم", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            OfflineQuranReading()
                        }
                        else -> {
                            Text("الإعدادات", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("مدينة مواقيت الصلاة", fontWeight = FontWeight.Bold)
                            PrayerTimes.cities.forEach { item ->
                                Row {
                                    RadioButton(selected = cityName == item.name, onClick = {
                                        cityName = item.name
                                        prefs.edit().putString("prayer_city", item.name).apply()
                                        PrayerAlerts.schedule(context)
                                    })
                                    Text(item.name, modifier = Modifier.padding(top = 12.dp))
                                }
                            }
                            Text("طريقة حساب الفجر والعشاء", fontWeight = FontWeight.Bold)
                            listOf("MWL" to "رابطة العالم الإسلامي (18° / 17°)", "EGYPT" to "الهيئة المصرية (19.5° / 17.5°)").forEach { (id, title) ->
                                Row {
                                    RadioButton(selected = method == id, onClick = {
                                        method = id
                                        prefs.edit().putString("prayer_method", id).apply()
                                        PrayerAlerts.schedule(context)
                                    })
                                    Text(title, modifier = Modifier.padding(top = 12.dp))
                                }
                            }
                            Text("تصحيح مواقيت الصلاة: $prayerOffset دقيقة")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    prayerOffset = (prayerOffset - 1).coerceAtLeast(-30)
                                    prefs.edit().putInt("prayer_offset_minutes", prayerOffset).apply()
                                    PrayerAlerts.schedule(context)
                                }) { Text("−1") }
                                OutlinedButton(onClick = {
                                    prayerOffset = (prayerOffset + 1).coerceAtMost(30)
                                    prefs.edit().putInt("prayer_offset_minutes", prayerOffset).apply()
                                    PrayerAlerts.schedule(context)
                                }) { Text("+1") }
                                TextButton(onClick = {
                                    prayerOffset = 0
                                    prefs.edit().putInt("prayer_offset_minutes", 0).apply()
                                    PrayerAlerts.schedule(context)
                                }) { Text("تصفير") }
                            }
                            Text("المواقيت تقديرية وتحتاج المقارنة بتقويم مسجدك المحلي. تنبيهات الصلاة والأذكار متاحة بصورة تجريبية عند تفعيلها؛ الأذان الصوتي لم يُفعّل بعد.", color = Color.Gray)
                            Text("الإعدادات الحالية محفوظة محليًا على هذا الجهاز.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f).padding(top = 12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

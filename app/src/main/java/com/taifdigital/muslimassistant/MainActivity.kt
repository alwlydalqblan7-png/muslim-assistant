package com.taifdigital.muslimassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var dhikr by remember { mutableStateOf(prefs.getBoolean("dhikr", true)) }
    var sound by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    var quiet by remember { mutableStateOf(prefs.getBoolean("quiet", true)) }
    var voice by remember { mutableStateOf(prefs.getString("voice", "makkah") ?: "makkah") }

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
                                    Text("مواقيت الصلاة", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text("تظهر بعد تحديد المدينة وطريقة الحساب", color = cream)
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
                            SettingSwitch("الأذان الكامل (إعداد مبدئي)", adhan) {
                                adhan = it; prefs.edit().putBoolean("adhan", it).apply()
                            }
                            Text("صوت المؤذن الافتراضي")
                            listOf("makkah" to "مؤذن الحرم المكي", "madinah" to "مؤذن المسجد النبوي", "other" to "صوت آخر").forEach { (id, label) ->
                                Row {
                                    RadioButton(selected = voice == id, onClick = {
                                        voice = id; prefs.edit().putString("voice", id).apply()
                                    })
                                    Text(label, modifier = Modifier.padding(top = 12.dp))
                                }
                            }
                            Text("الفجر • الظهر • العصر • المغرب • العشاء")
                            Text("الأذان والتنبيهات الفعلية ستنفذ في المرحلة الثانية.", color = Color.Gray)
                        }
                        "الأذكار" -> {
                            Text("الأذكار اليومية", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("أستغفر الله العظيم وأتوب إليه", fontSize = 20.sp)
                            SettingSwitch("التذكير كل 60 دقيقة", dhikr) {
                                dhikr = it; prefs.edit().putBoolean("dhikr", it).apply()
                            }
                            SettingSwitch("صوت الذكر", sound) {
                                sound = it; prefs.edit().putBoolean("sound", it).apply()
                            }
                            SettingSwitch("الهدوء أثناء النوم", quiet) {
                                quiet = it; prefs.edit().putBoolean("quiet", it).apply()
                            }
                            Text("الجدولة الفعلية والتنبيهات ستنفذ في المرحلة الثالثة.", color = Color.Gray)
                        }
                        "القرآن" -> {
                            Text("القرآن الكريم", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("قراءة المصحف والاستماع والتنزيل: قيد التخطيط.")
                        }
                        else -> {
                            Text("الإعدادات", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("المدينة وطريقة حساب الصلاة، الأذونات، ساعات النوم، تنزيل الأصوات: ستضاف في المراحل القادمة.")
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

package com.taifdigital.muslimassistant

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun SourceCredits() {
    val uri = LocalUriHandler.current
    Text("المصادر والتراخيص — 0.2.0")
    Text("القرآن: Tanzil Quran Text (Uthmani 1.1), Copyright © 2007–2026 Tanzil Project. CC BY 3.0. نُقل النص كاملًا دون تغيير. إذن النقل لا يسمح بتعديل النص. إشعار الحقوق الكامل مضمّن في ملف النص.")
    TextButton(onClick = { uri.openUri("https://tanzil.net/docs/Text_License") }) { Text("مصدر المصحف وشروطه") }
    Text("الأذان: Azan.ogg، Andrewler، 2022، CC BY-SA 4.0. الملف مضمّن دون تعديل؛ هذا الترخيص يخص التسجيل. المصدر لا يؤيد التطبيق.")
    TextButton(onClick = { uri.openUri("https://commons.wikimedia.org/wiki/File:Azan.ogg") }) { Text("مصدر تسجيل الأذان") }
    TextButton(onClick = { uri.openUri("https://creativecommons.org/licenses/by-sa/4.0/") }) { Text("ترخيص التسجيل CC BY-SA 4.0") }
}

package com.wm.fit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** كل رقم في التطبيق ومصدره. */
@Composable
fun Sources(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("من وين أرقامنا؟", style = MaterialTheme.typography.headlineMedium)
            Text(
                "كل رقم في التطبيق ومصدره",
                style = MaterialTheme.typography.bodySmall,
                color = C.muted,
            )
        }

        Src(
            "التمرين",
            "10 مجموعات أو أكثر لكل عضلة بالأسبوع، والفايدة تقل بعد 18 إلى 20. " +
                "كل عضلة مرتين بالأسبوع. مو لازم كل مجموعة للفشل.",
            "ACSM · الكلية الأمريكية للطب الرياضي · 2026",
        )
        Src(
            "البروتين",
            "1.4 إلى 2.0 جم لكل كيلو عام. ووقت التنشيف 2.3 إلى 3.1 للمتمرنين.",
            "ISSN · الجمعية الدولية للتغذية الرياضية · 2017",
        )
        Src(
            "التضخيم",
            "فائض 10 إلى 20٪، وزيادة 0.25 إلى 0.5٪ من الوزن بالأسبوع.",
            "Iraki وآخرون · مجلة Sports · 2019",
        )
        Src(
            "التنشيف",
            "نزول 0.5 إلى 1٪ من الوزن بالأسبوع، مع بروتين عالي وحديد ثقيل، " +
                "يحمي العضل قدر الإمكان.",
            "مراجعات التغذية الرياضية",
        )
        Src(
            "السعرات",
            "معادلة Mifflin-St Jeor، وهي الأدق للأصحاء بدون قياس نسبة دهون.",
            "Mifflin وآخرون · 1990",
        )
        Src(
            "نسبة الدهون · تقريبية",
            "معادلة الخصر والرقبة فيها خطأ 3 إلى 4٪، وما تتابع التغيّر القصير بدقة. " +
                "عشان كذا الرقم الأساسي عندنا هو مقاس الخصر نفسه.",
            "Hodgdon و Friedl · 1999",
            warn = true,
        )

        Text(
            "العلم يتطور. نراجع الأرقام مع كل تحديث. آخر مراجعة: نوفمبر 2026.",
            style = MaterialTheme.typography.labelSmall,
            color = C.muted,
        )
        Spacer(Modifier.height(16.dp))
        BigButton("رجوع", onClick = onBack)
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun Src(title: String, body: String, source: String, warn: Boolean = false) {
    Card(radius = 22) {
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = if (warn) C.warnText else C.accent,
        )
        Text(body, style = MaterialTheme.typography.bodyMedium)
        Text(source, style = MaterialTheme.typography.labelSmall, color = C.muted)
    }
}

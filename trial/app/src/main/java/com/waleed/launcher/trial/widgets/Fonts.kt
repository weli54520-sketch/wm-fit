@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.waleed.launcher.trial.widgets

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.waleed.launcher.trial.R

private fun variable(res: Int, vararg weights: Int): FontFamily = FontFamily(
    weights.map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** الخطوط الفخمة المتفق عليها */
object WFonts {
    val kufi = variable(R.font.noto_kufi, 300, 400, 500, 600, 700)
    val grotesk = variable(R.font.space_grotesk, 300, 400, 500, 700)
    val playfair = variable(R.font.playfair, 400, 500, 700, 900)
    val bodoni = variable(R.font.bodoni, 400, 500, 700)
    val reem = variable(R.font.reem_kufi, 400, 500, 700)
    val mono = FontFamily(Font(R.font.space_mono, FontWeight.Normal), Font(R.font.space_mono_bold, FontWeight.Bold))
    val amiri = FontFamily(Font(R.font.amiri, FontWeight.Normal), Font(R.font.amiri_bold, FontWeight.Bold))
    val ruqaa = FontFamily(Font(R.font.aref_ruqaa, FontWeight.Normal), Font(R.font.aref_ruqaa_bold, FontWeight.Bold))
}

/** خطوط الأرقام والكتابة اللي يختار منها وليد */
enum class NumFont(val title: String, val family: FontFamily) {
    GROTESK("عصري", WFonts.grotesk),
    PLAYFAIR("كلاسيكي", WFonts.playfair),
    MONO("تقني", WFonts.mono),
    BODONI("فاخر", WFonts.bodoni),
    AMIRI("نسخ", WFonts.amiri),
    REEM("كوفي", WFonts.reem),
    KUFI("بسيط", WFonts.kufi),
    RUQAA("رقعة", WFonts.ruqaa),
}

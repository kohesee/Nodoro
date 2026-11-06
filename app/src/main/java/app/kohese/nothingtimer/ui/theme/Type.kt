@file:OptIn(ExperimentalTextApi::class)

package app.kohese.nothingtimer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

import app.kohese.nothingtimer.R

// Nothing fonts
val NDot55 = FontFamily(Font(R.font.ndot55, FontWeight.Normal))
val NDot57 = FontFamily(Font(R.font.ndot57, FontWeight.Normal))

// Poppins (static)
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

// Inter (variable font, minSdk 34 supports variation settings)
val Inter = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { w ->
        Font(
            R.font.inter_variable,
            weight = w,
            variationSettings = FontVariation.Settings(FontVariation.weight(w.weight))
        )
    }
)

/*
 * Font plan (no system default font is used anywhere):
 *  - display*              -> NDot55 : timer digits
 *  - NODORO wordmark       -> NDot57 : set explicitly where it is drawn
 *  - headline*, titleL/M   -> Inter  : subheadings (Focus, Stats, Settings, section titles)
 *  - everything else       -> Poppins: titleSmall, body*, label*, bottom bar, ads
 */
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = NDot55, fontWeight = FontWeight.Normal),
    displayMedium = base.displayMedium.copy(fontFamily = NDot55, fontWeight = FontWeight.Normal),
    displaySmall = base.displaySmall.copy(fontFamily = NDot55, fontWeight = FontWeight.Normal),
    headlineLarge = base.headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Normal),
    bodySmall = base.bodySmall.copy(fontFamily = Poppins, fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.Medium)
)
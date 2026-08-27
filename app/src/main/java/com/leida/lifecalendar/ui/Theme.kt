package com.leida.lifecalendar.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.leida.lifecalendar.R

/* Design tokens lifted verbatim from 人生周历.dc.html — option 2a (年柱原型 · 深色). */

val Ink = Color(0xFF1F1E1D)          // page background
val Surface = Color(0xFF2A2826)      // cards, sheet
val Field = Color(0xFF34312E)        // inputs
val Bone = Color(0xFFF5F1EB)         // primary text
val Sand = Color(0xFF8C8579)         // secondary text
val Stone = Color(0xFF6B675F)        // tertiary text / ticks
val Clay = Color(0xFFD97757)         // accent

val Hairline = Bone.copy(alpha = 0.06f)
val FieldStroke = Bone.copy(alpha = 0.12f)

/** Track tint per life stage: 童年 / 工作 / 退休. */
val StageTrack = listOf(
    Bone.copy(alpha = 0.07f),
    Bone.copy(alpha = 0.10f),
    Bone.copy(alpha = 0.05f),
)
val FlatTrack = Bone.copy(alpha = 0.08f)
val LivedFill = Bone.copy(alpha = 0.72f)

val InterTight = FontFamily(
    Font(R.font.inter_tight_regular, FontWeight.Normal),
    Font(R.font.inter_tight_medium, FontWeight.Medium),
    Font(R.font.inter_tight_semibold, FontWeight.SemiBold),
)

val SourceSerif = FontFamily(
    Font(R.font.source_serif_light, FontWeight.Light),
    Font(R.font.source_serif_regular, FontWeight.Normal),
    Font(R.font.source_serif_medium, FontWeight.Medium),
)

fun sans(
    size: Double,
    color: Color = Bone,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0,
    lineHeight: Double = 0.0,
): TextStyle = TextStyle(
    fontFamily = InterTight,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = (size * tracking).sp,
    lineHeight = if (lineHeight == 0.0) TextUnit.Unspecified else (size * lineHeight).sp,
)

fun serif(
    size: Double,
    color: Color = Bone,
    weight: FontWeight = FontWeight.Normal,
    tracking: Double = 0.0,
    lineHeight: Double = 0.0,
): TextStyle = TextStyle(
    fontFamily = SourceSerif,
    fontSize = size.sp,
    fontWeight = weight,
    color = color,
    letterSpacing = (size * tracking).sp,
    lineHeight = if (lineHeight == 0.0) TextUnit.Unspecified else (size * lineHeight).sp,
)

/** The 10.5sp / .08em all-caps-ish kicker used above every card. */
fun kicker(color: Color = Stone) = sans(10.5, color, tracking = 0.08)

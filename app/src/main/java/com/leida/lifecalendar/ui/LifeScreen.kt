package com.leida.lifecalendar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leida.lifecalendar.data.LifeCalc
import com.leida.lifecalendar.data.Milestone
import com.leida.lifecalendar.data.byYear
import kotlin.math.min

private const val COLUMNS = 8
private const val APPEAR_MS = 940f
private const val APPEAR_RUN = 500f

/**
 * 人生 tab — one pillar per year of life. The filled height is the weeks already spent in that
 * year; the year you are living in is the only clay-coloured one.
 */
@Composable
fun LifeScreen(
    calc: LifeCalc,
    milestones: List<Milestone>,
    showStages: Boolean,
    showQuote: Boolean,
    selectedYear: Int?,
    scrollState: ScrollState,
    onSelect: (Int) -> Unit,
    onLongSelect: (Int) -> Unit,
) {
    val msByYear = remember(milestones) { milestones.byYear() }

    // Pillars grow to their true height once, and again whenever the arithmetic changes.
    val grow = remember { Animatable(0f) }
    LaunchedEffect(calc.span, calc.livedWeeks) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(700, easing = Settle))
    }

    // Staggered entrance: pillar y starts min(y * 7, 420)ms in, matching the .lr keyframes.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(APPEAR_MS.toInt(), easing = LinearEasing)) }

    Column(
        Modifier
            .verticalScroll(scrollState)
            .padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 120.dp),
    ) {
        Text("剩余", style = kicker())
        Spacer(Modifier.height(8.dp))
        Text(
            "${LifeCalc.group(calc.monthsLeft)} 个月",
            style = serif(44.0, Bone, tracking = -0.025, lineHeight = 1.05),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "已经走过 ${LifeCalc.group(calc.livedWeeks)} 周，占一生的 ${calc.percentLabel}。",
            style = sans(13.0, Sand, lineHeight = 1.6),
        )
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            (0 until calc.span).chunked(COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { year ->
                        Pillar(
                            year = year,
                            fill = calc.livedIn(year) / 52f,
                            isNow = year == calc.currentYear,
                            hasMilestone = msByYear.containsKey(year),
                            selected = selectedYear == year,
                            track = if (showStages) StageTrack[calc.stageOf(year)] else FlatTrack,
                            grow = { grow.value },
                            appear = { appear.value },
                            onClick = { onSelect(year) },
                            onLongClick = { onLongSelect(year) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Legend()

        Spacer(Modifier.height(26.dp))
        Card(Modifier.fillMaxWidth()) {
            Text("今日一句", style = kicker())
            Spacer(Modifier.height(10.dp))
            Text(
                if (showQuote) calc.quote else "已关闭每日一句。",
                style = serif(17.0, Bone, lineHeight = 1.65),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Pillar(
    year: Int,
    fill: Float,
    isNow: Boolean,
    hasMilestone: Boolean,
    selected: Boolean,
    track: Color,
    grow: () -> Float,
    appear: () -> Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val delay = min(year * 7, 420) / APPEAR_MS
    val fillColor = if (isNow) Clay else LivedFill
    val decade = year % 10 == 0

    Column(
        modifier
            .graphicsLayer {
                val t = ((appear() - delay) / (APPEAR_RUN / APPEAR_MS)).coerceIn(0f, 1f)
                alpha = t
                translationY = (1f - t) * 6.dp.toPx()
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier
                .size(4.dp)
                .background(if (hasMilestone) Clay else Color.Transparent, CircleShape),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .drawBehind {
                    val r = CornerRadius(3.dp.toPx())
                    drawRoundRect(track, cornerRadius = r)
                    val h = size.height * fill * grow()
                    if (h > 0.5f) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(0f, size.height - h),
                            size = Size(size.width, h),
                            cornerRadius = r,
                        )
                    }
                    if (selected) {
                        val w = 1.5.dp.toPx()
                        drawRoundRect(
                            color = Bone.copy(alpha = 0.55f),
                            topLeft = Offset(-w / 2, -w / 2),
                            size = Size(size.width + w, size.height + w),
                            cornerRadius = CornerRadius(3.dp.toPx() + w / 2),
                            style = Stroke(w),
                        )
                    }
                },
        )
        Text(
            if (decade) year.toString() else "·",
            style = sans(
                8.0,
                if (decade) Bone.copy(alpha = 0.34f) else Color.Transparent,
                tracking = 0.02,
                lineHeight = 1.0,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        LegendItem("童年 0–18", StageTrack[0], border = true)
        LegendItem("工作 18–60", Bone.copy(alpha = 0.18f))
        LegendItem("此刻", Clay)
    }
}

@Composable
private fun LegendItem(name: String, swatch: Color, border: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val shape = RoundedCornerShape(2.dp)
        Box(
            Modifier
                .size(12.dp, 6.dp)
                .background(swatch, shape)
                .then(
                    if (border) Modifier.border(1.dp, Bone.copy(alpha = 0.14f), shape)
                    else Modifier,
                ),
        )
        Text(name, style = sans(11.5, Sand))
    }
}

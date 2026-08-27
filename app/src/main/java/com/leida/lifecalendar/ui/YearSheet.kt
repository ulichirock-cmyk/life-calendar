package com.leida.lifecalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leida.lifecalendar.data.LifeCalc
import com.leida.lifecalendar.data.Milestone

/**
 * The year a pillar stands for, opened out: its 52 weeks as a strip, plus the option to
 * leave a mark on it.
 */
@Composable
fun YearSheet(
    calc: LifeCalc,
    year: Int,
    milestones: List<Milestone>,
    marking: Boolean,
    draft: String,
    onDraft: (String) -> Unit,
    onStartMark: () -> Unit,
    onConfirmMark: () -> Unit,
) {
    val yearMilestones = remember(milestones, year) {
        milestones.filter { it.week / 52 == year }
    }
    val livedThis = calc.livedIn(year)
    val startYear = calc.settings.birth.year + year
    val kickerText = when {
        year < calc.currentYear -> "已经过完"
        year == calc.currentYear -> "正在过"
        else -> "还没到来"
    }

    Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 34.dp)) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(36.dp, 4.dp)
                .background(Bone.copy(alpha = 0.14f), RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.height(20.dp))

        Text(kickerText, style = kicker())
        Spacer(Modifier.height(8.dp))
        Text("$year 岁", style = serif(28.0, Bone, lineHeight = 1.2))
        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                append("$startYear – ${startYear + 1} 年 · 这一年过了 $livedThis / 52 周")
                if (yearMilestones.isNotEmpty()) {
                    append(" · ")
                    append(yearMilestones.joinToString("、") { it.label })
                }
            },
            style = sans(13.0, Sand, lineHeight = 1.7),
        )
        Spacer(Modifier.height(18.dp))

        WeekStrip(calc, year, yearMilestones)
        Spacer(Modifier.height(20.dp))

        if (marking) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Field, RoundedCornerShape(10.dp))
                    .border(1.dp, FieldStroke, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                if (draft.isEmpty()) {
                    Text("给这一年起个名字", style = sans(16.0, Stone))
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraft,
                    singleLine = true,
                    textStyle = sans(16.0, Bone),
                    cursorBrush = SolidColor(Clay),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirmMark() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Spacer(Modifier.height(12.dp))
            SheetButton("留下这个刻痕", Clay, androidx.compose.ui.graphics.Color.White, onConfirmMark)
        } else {
            SheetButton("标记为里程碑", Bone.copy(alpha = 0.07f), Bone, onStartMark)
        }
    }
}

@Composable
private fun WeekStrip(calc: LifeCalc, year: Int, yearMilestones: List<Milestone>) {
    val marked = remember(yearMilestones) { yearMilestones.associateBy { it.week % 52 } }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (k in 0 until 52) {
            val globalWeek = year * 52 + k
            val past = globalWeek < calc.livedWeeks
            val isNow = globalWeek == calc.livedWeeks
            val mark = marked[k]
            val color = when {
                isNow || mark != null -> Clay
                past -> Bone.copy(alpha = 0.70f)
                else -> Bone.copy(alpha = 0.12f)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(22.dp)
                    .background(
                        if (mark != null && !past) color.copy(alpha = color.alpha * 0.5f) else color,
                        RoundedCornerShape(1.5.dp),
                    ),
            )
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    background: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
    ) {
        Text(
            label,
            style = sans(15.0, content),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

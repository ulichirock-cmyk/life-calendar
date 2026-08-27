package com.leida.lifecalendar.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leida.lifecalendar.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

private const val SPAN_MIN = 60f
private const val SPAN_MAX = 100f

/** 设置 tab — birth date, expected span, and the two display switches. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    scrollState: ScrollState,
    versionName: String,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
    onBirth: (LocalDate) -> Unit,
    onSpan: (Int) -> Unit,
    onToggleStages: () -> Unit,
    onToggleQuote: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }

    Column(
        Modifier
            .verticalScroll(scrollState)
            .padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 120.dp),
    ) {
        Text("设置", style = serif(30.0, Bone, lineHeight = 1.2))
        Spacer(Modifier.height(26.dp))

        Card(Modifier.fillMaxWidth()) {
            Text("出生日期", style = kicker())
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Field, RoundedCornerShape(10.dp))
                    .border(1.dp, FieldStroke, RoundedCornerShape(10.dp))
                    .clickable { picking = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                Text(settings.birth.toString(), style = sans(16.0, Bone))
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("预期寿命", style = kicker(), modifier = Modifier.alignByBaseline())
                Text(
                    "${settings.span} 岁",
                    style = serif(22.0, Bone),
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(Modifier.height(14.dp))
            Slider(
                value = settings.span.toFloat(),
                onValueChange = { onSpan(it.roundToInt()) },
                valueRange = SPAN_MIN..SPAN_MAX,
                steps = (SPAN_MAX - SPAN_MIN).toInt() - 1,
                thumb = {
                    Box(Modifier.size(20.dp).background(Clay, CircleShape))
                },
                track = {
                    val fraction = (settings.span - SPAN_MIN) / (SPAN_MAX - SPAN_MIN)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Bone.copy(alpha = 0.14f), CircleShape),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(4.dp)
                                .background(Clay, CircleShape),
                        )
                    }
                },
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("60", style = sans(10.5, Stone))
                Text("100", style = sans(10.5, Stone))
            }
        }
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
            ToggleRow("按阶段区分年柱底色", settings.showStages, onToggleStages)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            ToggleRow("每日一句提醒", settings.showQuote, onToggleQuote)
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCheckUpdate)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("检查更新", style = sans(15.0, Bone))
                Text(
                    if (checkingUpdate) "检查中…" else "v" + versionName,
                    style = sans(13.0, Stone),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "寿命只是一个刻度，不是一个预言。把它调到你愿意面对的那个数字。",
            style = serif(13.0, Stone, lineHeight = 1.7),
        )
    }

    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = settings.birth
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            colors = DatePickerDefaults.colors(containerColor = Surface),
            confirmButton = {
                Text(
                    "确定",
                    style = sans(15.0, Clay, weight = androidx.compose.ui.text.font.FontWeight.Medium),
                    modifier = Modifier
                        .clickable {
                            state.selectedDateMillis?.let {
                                onBirth(
                                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(),
                                )
                            }
                            picking = false
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    style = sans(15.0, Sand),
                    modifier = Modifier
                        .clickable { picking = false }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                )
            },
        ) {
            DatePicker(
                state = state,
                title = { Text("出生日期", style = kicker(), modifier = Modifier.padding(24.dp, 20.dp, 24.dp, 0.dp)) },
                headline = {
                    Text(
                        state.selectedDateMillis
                            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
                            ?: settings.birth.toString(),
                        style = serif(30.0, Bone),
                        modifier = Modifier.padding(24.dp, 8.dp, 24.dp, 8.dp),
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Surface,
                    titleContentColor = Stone,
                    headlineContentColor = Bone,
                    weekdayContentColor = Stone,
                    subheadContentColor = Sand,
                    navigationContentColor = Bone,
                    yearContentColor = Sand,
                    currentYearContentColor = Clay,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = Clay,
                    dayContentColor = Bone,
                    disabledDayContentColor = Stone,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = Clay,
                    todayContentColor = Clay,
                    todayDateBorderColor = Clay,
                    dividerColor = Hairline,
                ),
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = sans(15.0, Bone))
        PillSwitch(checked)
    }
}

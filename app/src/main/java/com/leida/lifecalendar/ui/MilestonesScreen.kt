package com.leida.lifecalendar.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leida.lifecalendar.data.LifeCalc
import com.leida.lifecalendar.data.Milestone

/** 里程碑 tab — every mark, oldest first. */
@Composable
fun MilestonesScreen(
    calc: LifeCalc,
    milestones: List<Milestone>,
    scrollState: ScrollState,
    onRemove: (Long) -> Unit,
) {
    val sorted = remember(milestones) { milestones.sortedBy { it.week } }

    Column(
        Modifier
            .verticalScroll(scrollState)
            .padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 120.dp),
    ) {
        Text("${milestones.size} 个刻痕", style = serif(30.0, Bone, lineHeight = 1.2))
        Spacer(Modifier.height(8.dp))
        Text("长按任意一根年柱，就能在那一年留下刻痕。", style = sans(13.0, Sand))
        Spacer(Modifier.height(26.dp))

        sorted.forEach { m ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(8.dp).background(Clay, CircleShape))
                Column(Modifier.weight(1f)) {
                    Text(m.label, style = sans(15.0, Bone))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${m.week / 52} 岁 · ${calc.dateOfWeek(m.week).year} 年 · " +
                            "第 ${LifeCalc.group(m.week)} 周",
                        style = sans(12.0, Stone),
                    )
                }
                Text(
                    "移除",
                    style = sans(12.0, Stone),
                    modifier = Modifier
                        .clickable { onRemove(m.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        }
    }
}

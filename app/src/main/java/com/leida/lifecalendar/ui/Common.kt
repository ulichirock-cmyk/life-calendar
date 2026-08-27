package com.leida.lifecalendar.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The CSS `cubic-bezier(0.2,0,0,1)` used for every state change in the prototype. */
val Snap = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** The slower `cubic-bezier(0,0,0,1)` used for the pillars filling up. */
val Settle = CubicBezierEasing(0f, 0f, 0f, 1f)

/** Surface card: #2A2826, hairline border, r14, 20dp padding. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            .padding(padding),
        content = content,
    )
}

/** 42×25 pill switch with a 21dp knob — hand-built to match the prototype rather than Material's. */
@Composable
fun PillSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val track by animateColorAsState(
        if (checked) Clay else Bone.copy(alpha = 0.14f),
        tween(200, easing = Snap),
        label = "track",
    )
    val knob by animateDpAsState(
        if (checked) 17.dp else 0.dp,
        tween(200, easing = Snap),
        label = "knob",
    )
    Box(
        modifier
            .size(42.dp, 25.dp)
            .background(track, CircleShape)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = knob)
                .size(21.dp)
                .shadow(1.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

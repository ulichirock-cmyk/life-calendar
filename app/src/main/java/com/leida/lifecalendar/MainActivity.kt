package com.leida.lifecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leida.lifecalendar.data.LifeCalc
import com.leida.lifecalendar.ui.Bone
import com.leida.lifecalendar.ui.Clay
import com.leida.lifecalendar.ui.Hairline
import com.leida.lifecalendar.ui.Ink
import com.leida.lifecalendar.ui.LifeScreen
import com.leida.lifecalendar.ui.MilestonesScreen
import com.leida.lifecalendar.ui.Sand
import com.leida.lifecalendar.ui.SettingsScreen
import com.leida.lifecalendar.ui.Stone
import com.leida.lifecalendar.ui.Surface
import com.leida.lifecalendar.ui.UpdateDialog
import com.leida.lifecalendar.ui.YearSheet
import com.leida.lifecalendar.ui.sans
import com.leida.lifecalendar.ui.serif
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private enum class Tab(val label: String) { Life("人生"), Milestones("里程碑"), Settings("设置") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(
    vm: LifeViewModel = viewModel(),
    updateVm: UpdateViewModel = viewModel(),
) {
    val settings = vm.settings
    val milestones = vm.milestones
    val calc = remember(settings) { LifeCalc(settings) }

    var tab by remember { mutableStateOf(Tab.Life) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var marking by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    // Bumped on every message so an identical one restarts the timer instead of inheriting it.
    var toastSeq by remember { mutableIntStateOf(0) }

    fun showToast(message: String) {
        toast = message
        toastSeq++
    }

    val lifeScroll = rememberScrollState()
    val msScroll = rememberScrollState()
    val setScroll = rememberScrollState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Header replaces the title with the age you are scrolling past, as in the prototype.
    val density = LocalDensity.current
    val idlePx = remember(density) { with(density) { 50.dp.toPx() } }
    val leadPx = remember(density) { with(density) { 120.dp.toPx() } }
    val scrollAge by remember(calc.span) {
        derivedStateOf {
            val max = lifeScroll.maxValue.toFloat()
            val at = lifeScroll.value.toFloat()
            when {
                max < 40f || at < idlePx -> null
                else -> (((at - leadPx) / (max - leadPx)).coerceIn(0f, 1f) * calc.span).roundToInt()
            }
        }
    }

    // Throttled to once a day inside the view model, and silent unless it finds something.
    LaunchedEffect(Unit) { updateVm.checkOnLaunch() }

    LaunchedEffect(toastSeq) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            selectedYear = null
            marking = false
            draft = ""
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 26.dp, top = 14.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    if (tab == Tab.Life && scrollAge != null) "$scrollAge 岁" else "人生年柱",
                    style = serif(19.0, Bone, tracking = -0.01),
                )
                Text(
                    if (tab == Tab.Life) {
                        "${calc.currentYear} 岁 · 第 ${LifeCalc.group(calc.livedWeeks + 1)} 周"
                    } else {
                        ""
                    },
                    style = sans(11.0, Sand, tracking = 0.04),
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.Life -> LifeScreen(
                        calc = calc,
                        milestones = milestones,
                        showStages = settings.showStages,
                        showQuote = settings.showQuote,
                        selectedYear = selectedYear,
                        scrollState = lifeScroll,
                        onSelect = { selectedYear = it; marking = false; draft = "" },
                        onLongSelect = { selectedYear = it; marking = true; draft = "" },
                    )

                    Tab.Milestones -> MilestonesScreen(
                        calc = calc,
                        milestones = milestones,
                        scrollState = msScroll,
                        onRemove = { vm.removeMilestone(it); showToast("已移除") },
                    )

                    Tab.Settings -> SettingsScreen(
                        settings = settings,
                        scrollState = setScroll,
                        versionName = updateVm.currentVersionName,
                        checkingUpdate = updateVm.checkingManually,
                        onCheckUpdate = updateVm::checkManually,
                        onBirth = { vm.setBirth(it); selectedYear = null },
                        onSpan = { vm.setSpan(it); selectedYear = null },
                        onToggleStages = vm::toggleStages,
                        onToggleQuote = vm::toggleQuote,
                    )
                }
            }

            TabBar(tab) {
                tab = it
                selectedYear = null
            }
        }

        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 26.dp, end = 26.dp, bottom = 76.dp),
        ) {
            Text(
                toast.orEmpty(),
                style = sans(13.0, Ink),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Bone.copy(alpha = 0.94f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    val year = selectedYear
    if (year != null) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            containerColor = Surface,
            contentColor = Bone,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0) },
        ) {
            Box(Modifier.navigationBarsPadding().imePadding()) {
                YearSheet(
                    calc = calc,
                    year = year,
                    milestones = milestones,
                    marking = marking,
                    draft = draft,
                    onDraft = { draft = it },
                    onStartMark = { marking = true },
                    onConfirmMark = {
                        showToast(vm.markYear(year, draft))
                        closeSheet()
                    },
                )
            }
        }
    }

    UpdateDialog(updateVm)
}

@Composable
private fun TabBar(current: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Ink)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp)) {
            Tab.entries.forEach { t ->
                val active = t == current
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (active) Clay else Color.Transparent),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(t.label, style = sans(12.0, if (active) Bone else Stone, tracking = 0.02))
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}


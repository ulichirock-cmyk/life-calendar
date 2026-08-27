package com.leida.lifecalendar.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.leida.lifecalendar.UpdateViewModel
import com.leida.lifecalendar.data.update.InstallBlocker
import com.leida.lifecalendar.data.update.UpdateError
import com.leida.lifecalendar.data.update.UpdateState

/**
 * The updater's only screen. Every state the user is meant to see renders here; the states that are
 * meant to be silent ([UpdateState.Idle]) never get this far because the view model gates
 * `dialogVisible`.
 */
@Composable
fun UpdateDialog(vm: UpdateViewModel) {
    if (!vm.dialogVisible) return
    val context = LocalContext.current
    val state = vm.state

    Dialog(onDismissRequest = vm::dismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(18.dp))
                .border(1.dp, Hairline, RoundedCornerShape(18.dp))
                .padding(24.dp),
        ) {
            when (state) {
                is UpdateState.Checking -> {
                    Text("检查更新", style = kicker())
                    Spacer(Modifier.height(10.dp))
                    Text("正在看看有没有新版本…", style = sans(15.0, Bone))
                }

                is UpdateState.UpToDate -> {
                    Text("检查更新", style = kicker())
                    Spacer(Modifier.height(10.dp))
                    Text("已经是最新版本", style = serif(24.0, Bone, lineHeight = 1.2))
                    Spacer(Modifier.height(8.dp))
                    Text("当前 v${vm.currentVersionName}", style = sans(13.0, Sand))
                    Spacer(Modifier.height(20.dp))
                    Actions(primaryLabel = null, secondaryLabel = "知道了", onSecondary = vm::dismiss)
                }

                is UpdateState.Available -> {
                    Text("有新版本", style = kicker())
                    Spacer(Modifier.height(10.dp))
                    Text("v${state.update.versionName}", style = serif(28.0, Bone, lineHeight = 1.2))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "当前 v${vm.currentVersionName} · ${formatSize(state.update.assetSizeBytes)}",
                        style = sans(12.0, Stone),
                    )
                    val notes = highlights(state.update.notes)
                    if (notes != null) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            notes,
                            style = sans(13.0, Sand, lineHeight = 1.7),
                            modifier = Modifier
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Actions("下载", vm::download, "以后再说", vm::dismiss)
                }

                is UpdateState.Downloading -> {
                    Text("正在下载", style = kicker())
                    Spacer(Modifier.height(10.dp))
                    Text("v${state.update.versionName}", style = serif(24.0, Bone, lineHeight = 1.2))
                    Spacer(Modifier.height(16.dp))
                    Progress(state.progress)
                    Spacer(Modifier.height(8.dp))
                    Text("${(state.progress * 100).toInt()}%", style = sans(12.0, Stone))
                    Spacer(Modifier.height(20.dp))
                    Actions(primaryLabel = null, secondaryLabel = "取消", onSecondary = vm::dismiss)
                }

                is UpdateState.ReadyToInstall -> {
                    val apk = vm.apkToInstall()
                    val blocker = apk?.let { vm.installBlocker(it) }
                    Text("下载完成", style = kicker())
                    Spacer(Modifier.height(10.dp))
                    Text("v${state.update.versionName}", style = serif(24.0, Bone, lineHeight = 1.2))
                    Spacer(Modifier.height(10.dp))
                    when {
                        apk == null -> {
                            Text(
                                "安装包不见了——可能被系统清理了缓存。重新下载一次就好。",
                                style = sans(13.0, Sand, lineHeight = 1.7),
                            )
                            Spacer(Modifier.height(20.dp))
                            Actions("重新下载", vm::download, "以后再说", vm::dismiss)
                        }

                        blocker == InstallBlocker.SignatureMismatch -> {
                            Text(
                                "当前装的是调试版，签名和正式版对不上，Android 不允许直接覆盖。" +
                                    "先卸载现在这个，再装一次就行——之后的更新都不会再遇到这一步。",
                                style = sans(13.0, Sand, lineHeight = 1.7),
                            )
                            Spacer(Modifier.height(20.dp))
                            Actions(primaryLabel = null, secondaryLabel = "知道了", onSecondary = vm::dismiss)
                        }

                        blocker == InstallBlocker.Downgrade -> {
                            Text(
                                "这个版本比已安装的还旧，Android 不支持降级安装。",
                                style = sans(13.0, Sand, lineHeight = 1.7),
                            )
                            Spacer(Modifier.height(20.dp))
                            Actions(primaryLabel = null, secondaryLabel = "知道了", onSecondary = vm::dismiss)
                        }

                        !vm.canInstall() -> {
                            Text(
                                "还差一步：需要允许人生周历安装应用。去设置里打开这个开关，回来再点安装。",
                                style = sans(13.0, Sand, lineHeight = 1.7),
                            )
                            Spacer(Modifier.height(20.dp))
                            Actions(
                                "去授权",
                                { context.startActivity(vm.unknownSourcesSettingsIntent()) },
                                "以后再说",
                                vm::dismiss,
                            )
                        }

                        else -> {
                            Text(
                                "接下来是系统的安装确认页，装完自动回到应用。",
                                style = sans(13.0, Sand, lineHeight = 1.7),
                            )
                            Spacer(Modifier.height(20.dp))
                            Actions(
                                "安装",
                                { context.startActivity(vm.installIntent(apk)) },
                                "以后再说",
                                vm::dismiss,
                            )
                        }
                    }
                }

                is UpdateState.Failed -> {
                    Text("检查更新", style = kicker())
                    Spacer(Modifier.height(10.dp))
                    Text("没成功", style = serif(24.0, Bone, lineHeight = 1.2))
                    Spacer(Modifier.height(8.dp))
                    Text(messageFor(state.reason), style = sans(13.0, Sand, lineHeight = 1.7))
                    state.detail?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = sans(11.0, Stone))
                    }
                    Spacer(Modifier.height(20.dp))
                    Actions("重试", vm::checkManually, "知道了", vm::dismiss)
                }

                UpdateState.Idle -> vm.dismiss()
            }
        }
    }
}

@Composable
private fun Progress(progress: Float) {
    val animated by animateFloatAsState(progress, tween(200, easing = Snap), label = "progress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Bone.copy(alpha = 0.14f), CircleShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .height(4.dp)
                .background(Clay, CircleShape),
        )
    }
}

@Composable
private fun Actions(
    primaryLabel: String?,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DialogButton(secondaryLabel, Bone.copy(alpha = 0.07f), Bone, Modifier.weight(1f), onSecondary)
        if (primaryLabel != null && onPrimary != null) {
            DialogButton(primaryLabel, Clay, Color.White, Modifier.weight(1f), onPrimary)
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .background(background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            label,
            style = sans(15.0, content),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun messageFor(error: UpdateError): String = when (error) {
    UpdateError.Network -> "连不上 GitHub，检查一下网络再试。"
    UpdateError.Unauthorized -> "GitHub 暂时拒绝了请求，多半是访问太频繁，过会儿再试。"
    UpdateError.NoRelease -> "仓库还没有发过带 APK 的 release。"
    UpdateError.Unknown -> "GitHub 返回了没预料到的结果。"
}

/**
 * The release body as the dialog should show it: only the hand-written part above whatever GitHub
 * auto-generated below it (`## What's Changed`, `**Full Changelog**`) — a wall of commit links no
 * one reads on a phone. Null when there is nothing worth showing.
 */
private fun highlights(body: String?): String? = body
    ?.lineSequence()
    ?.takeWhile { line ->
        val t = line.trimStart()
        !t.startsWith("##") && !t.startsWith("**Full Changelog**")
    }
    ?.joinToString("\n")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

private fun formatSize(bytes: Long): String =
    if (bytes <= 0) "" else "%.1f MB".format(bytes / 1024.0 / 1024.0)

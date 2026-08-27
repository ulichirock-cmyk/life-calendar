package com.leida.lifecalendar.data.update

import java.io.IOException

/** A release that [UpdateService] has already decided is newer than the running build. */
data class AvailableUpdate(
    val versionName: String,
    val tagName: String,
    val notes: String?,
    val assetUrl: String,
    val assetSizeBytes: Long,
)

/**
 * The updater's whole surface, as one state machine driven by
 * [com.leida.lifecalendar.UpdateViewModel]. Only [Available], [Downloading], [ReadyToInstall] and
 * [Failed] have UI; the rest are silent unless the check was manual.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val progress: Float) : UpdateState
    data class ReadyToInstall(val update: AvailableUpdate) : UpdateState

    /** @param reason mapped to user-facing text by the dialog — the data layer stays string-free. */
    data class Failed(val reason: UpdateError, val detail: String? = null) : UpdateState
}

enum class UpdateError {
    /** Network unreachable / timed out / TLS failure. */
    Network,

    /** 401/403 — GitHub refused the request (rate limited, or the repo went private). */
    Unauthorized,

    /** Repo has no release yet, or the release carries no APK asset. */
    NoRelease,

    /** Any other non-2xx status or malformed payload. */
    Unknown,
}

/** Thrown by [UpdateService] so callers get the same [UpdateError] taxonomy the UI renders. */
class UpdateException(val error: UpdateError, val detail: String? = null) :
    IOException(detail ?: error.name)

package com.leida.lifecalendar

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.leida.lifecalendar.data.Store
import com.leida.lifecalendar.data.update.ApkInstaller
import com.leida.lifecalendar.data.update.UpdateError
import com.leida.lifecalendar.data.update.UpdateException
import com.leida.lifecalendar.data.update.UpdateService
import com.leida.lifecalendar.data.update.UpdateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * Drives the whole updater: the throttled check on cold start, the manual check from 设置, the
 * download, and the handoff to the system installer.
 *
 * The dialog is only auto-raised by [checkOnLaunch] when an update actually exists; "up to date"
 * and every failure stay silent there and only surface for a [checkManually], which is the one case
 * where the user is waiting for an answer.
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val service = UpdateService(app)
    private val installer = ApkInstaller(app)
    private val store = Store(app)

    var state by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    /** The dialog reads this so "以后再说" closes it without losing the underlying state. */
    var dialogVisible by mutableStateOf(false)
        private set

    /** True while a manual check is running, so 设置 can show a spinner on its own row. */
    var checkingManually by mutableStateOf(false)
        private set

    val currentVersionName: String = BuildConfig.VERSION_NAME

    private var downloadedApk: File? = null
    private var downloadJob: Job? = null

    /**
     * Cold-start check, at most once per [CHECK_INTERVAL_MILLIS]. Silent unless it finds something:
     * a launch is not a moment to interrupt the user with "you're up to date" or a network error.
     */
    fun checkOnLaunch() {
        // Idle is the only state a launch check may overwrite — never stomp a download in flight
        // or a result the user hasn't dismissed yet.
        if (state != UpdateState.Idle) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (now - store.lastUpdateCheck() < CHECK_INTERVAL_MILLIS) return@launch
            // Claim the slot before suspending so a re-entrant call can't start a second request.
            state = UpdateState.Checking
            val result = service.checkForUpdate()
            if (result is UpdateState.Failed) {
                // Don't spend the daily slot on a failure — a launch with no connectivity would
                // otherwise suppress the check for 24h. Back out to Idle so the next launch retries.
                state = UpdateState.Idle
                return@launch
            }
            store.saveLastUpdateCheck(now)
            state = result
            if (result is UpdateState.Available) dialogVisible = true
        }
    }

    /** 设置 page check: always hits the network, always reports the outcome in the dialog. */
    fun checkManually() {
        if (checkingManually) return
        viewModelScope.launch {
            checkingManually = true
            state = UpdateState.Checking
            val result = service.checkForUpdate()
            store.saveLastUpdateCheck(System.currentTimeMillis())
            state = result
            checkingManually = false
            dialogVisible = true
        }
    }

    /** Downloads the APK for the currently [UpdateState.Available] update. */
    fun download() {
        val available = state as? UpdateState.Available ?: return
        val previous = downloadJob
        downloadJob = viewModelScope.launch {
            // Wait for the old attempt to actually let go, not just to be told to stop. Cancelling
            // a coroutine does not interrupt a blocking socket read, so a job cancelled by "以后再说"
            // can stay parked until the read timeout and then land one last 64KB write. Both
            // attempts append to the same `.part`, so overlapping them splices duplicate bytes into
            // the APK — which the size check then rejects forever.
            previous?.cancelAndJoin()
            state = UpdateState.Downloading(available.update, 0f)
            try {
                val file = service.downloadApk(available.update) { progress ->
                    state = UpdateState.Downloading(available.update, progress)
                }
                downloadedApk = file
                state = UpdateState.ReadyToInstall(available.update)
            } catch (e: UpdateException) {
                Log.w(TAG, "download failed: ${e.error} ${e.detail}")
                state = UpdateState.Failed(e.error, e.detail)
            } catch (e: IOException) {
                Log.w(TAG, "download failed", e)
                state = UpdateState.Failed(UpdateError.Network, e.message)
            }
        }
    }

    /**
     * @return the file to install, or null when the download is gone (cache evicted between the
     *   download finishing and the user tapping install).
     */
    fun apkToInstall(): File? = downloadedApk?.takeIf { it.exists() }

    fun canInstall(): Boolean = installer.canInstall()

    /** Non-null when Android would refuse this APK; the dialog explains it instead of guessing. */
    fun installBlocker(apk: File) = installer.installBlocker(apk)

    fun installIntent(apk: File) = installer.installIntent(apk)

    fun unknownSourcesSettingsIntent() = installer.unknownSourcesSettingsIntent()

    /** "以后再说" / tapping outside: hide the dialog, cancel any download, drop back to Idle. */
    fun dismiss() {
        downloadJob?.cancel()
        downloadJob = null
        dialogVisible = false
        state = UpdateState.Idle
    }

    private companion object {
        const val TAG = "UpdateViewModel"

        /** Once a day — enough to land an update promptly without a request on every cold start. */
        const val CHECK_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
    }
}

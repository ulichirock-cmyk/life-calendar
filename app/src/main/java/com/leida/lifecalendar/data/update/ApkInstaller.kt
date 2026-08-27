package com.leida.lifecalendar.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File

/**
 * A reason Android will refuse the downloaded APK, detected before the handoff so the user gets an
 * explanation instead of the system's bare "app not installed".
 */
enum class InstallBlocker {
    /**
     * The installed app and the downloaded release were signed with different keys — in practice,
     * a debug build sitting in the release build's install slot. Android never replaces one with
     * the other, so the only fix is to uninstall first. (The debug build type carries
     * `applicationIdSuffix = ".debug"` precisely so this stops happening.)
     */
    SignatureMismatch,

    /** The release's versionCode is lower than the installed one; Android does not downgrade. */
    Downgrade,
}

/**
 * Hands a downloaded APK to the system package installer.
 *
 * There is no such thing as a silent self-update for a normal app: Android always shows its own
 * install confirmation, and since API 26 the app must additionally hold `REQUEST_INSTALL_PACKAGES`
 * **and** be granted the per-app "install unknown apps" toggle. So the flow is
 * [installBlocker] -> [canInstall] -> [unknownSourcesSettingsIntent] when that's off ->
 * [installIntent].
 */
class ApkInstaller(private val context: Context) {

    /** False when the user hasn't granted "install unknown apps" for this app yet. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Per-app "install unknown apps" screen, deep-linked to this package. */
    fun unknownSourcesSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Inspects [apk] against what is installed and reports why the install would fail, or null when
     * nothing stands in the way.
     *
     * Worth doing because the system installer's failure text ("应用未安装") names no cause at all,
     * and the two causes it hides have completely different fixes. Anything this method can't
     * determine returns null — it exists to explain a certain failure, never to block an install
     * the system might well have accepted.
     */
    fun installBlocker(apk: File): InstallBlocker? {
        val pm = context.packageManager
        val archive = runCatching { pm.getPackageArchiveInfo(apk.absolutePath, SIGNING_FLAGS) }
            .getOrNull() ?: return null
        // Not our package at all: nothing we can meaningfully say about it.
        if (archive.packageName != context.packageName) return null
        val installed = runCatching { pm.getPackageInfo(context.packageName, SIGNING_FLAGS) }
            .getOrNull() ?: return null

        val archiveSigners = signersOf(archive)
        val installedSigners = signersOf(installed)
        if (archiveSigners.isNotEmpty() && installedSigners.isNotEmpty() &&
            archiveSigners != installedSigners
        ) {
            return InstallBlocker.SignatureMismatch
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <
            PackageInfoCompat.getLongVersionCode(installed)
        ) {
            return InstallBlocker.Downgrade
        }
        return null
    }

    /**
     * The certificates that actually signed the APK, as a comparable set. `apkContentsSigners`
     * answers "who signed this file", which is exactly what the installer compares.
     */
    private fun signersOf(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().filterNotNull().map { it.toCharsString() }.toSet()
    }

    /**
     * The APK lives in the app's private cache dir, so it must be shared as a `content://` URI via
     * the FileProvider (`res/xml/file_paths.xml` -> `updates`) with read permission granted; a
     * `file://` URI would throw FileUriExposedException.
     */
    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private companion object {
        /** GET_SIGNING_CERTIFICATES is API 28+; minSdk is 26, so 26/27 use the old flag. */
        @Suppress("DEPRECATION")
        val SIGNING_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
    }
}

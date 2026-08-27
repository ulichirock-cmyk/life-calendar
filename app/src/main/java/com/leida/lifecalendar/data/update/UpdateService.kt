package com.leida.lifecalendar.data.update

import android.content.Context
import android.util.Log
import com.leida.lifecalendar.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * In-app updater against this repo's GitHub Releases (see `.github/workflows/android-release.yml`).
 *
 * The repo is public, so both the "latest release" lookup and the asset download are anonymous —
 * no credential is baked into the APK. Built on [HttpURLConnection] and `org.json` rather than
 * OkHttp/kotlinx: the rest of this app has no third-party dependencies and the updater needs none.
 */
class UpdateService(context: Context) {

    private val downloadDir = File(context.cacheDir, "updates")

    /**
     * @return [UpdateState.Available] when a strictly newer, non-draft release with an APK asset
     *   exists, [UpdateState.UpToDate] when it doesn't, or [UpdateState.Failed] — never throws.
     */
    suspend fun checkForUpdate(): UpdateState = withContext(Dispatchers.IO) {
        try {
            val release = fetchLatestRelease()
            val tag = release.optString("tag_name")
            // A draft is invisible to an anonymous client anyway, but the guard keeps the intent
            // explicit; pre-releases are skipped so a beta tag never auto-prompts.
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
                return@withContext UpdateState.UpToDate
            }
            if (!UpdateVersion.isNewer(tag, BuildConfig.VERSION_NAME)) {
                return@withContext UpdateState.UpToDate
            }
            val assets = release.optJSONArray("assets")
            val apks = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
            val asset = apks.firstOrNull { it.optString("name") == BuildConfig.UPDATE_ASSET_NAME }
                ?: apks.firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: return@withContext UpdateState.Failed(UpdateError.NoRelease)
            UpdateState.Available(
                AvailableUpdate(
                    versionName = UpdateVersion.normalize(tag),
                    tagName = tag,
                    notes = release.optString("body").takeIf { it.isNotBlank() },
                    // The asset API endpoint (not browser_download_url) — with an
                    // `Accept: application/octet-stream` header it 302s straight to the bytes,
                    // and it is the one form that keeps working if the repo ever goes private.
                    assetUrl = asset.optString("url"),
                    assetSizeBytes = asset.optLong("size"),
                ),
            )
        } catch (e: UpdateException) {
            Log.w(TAG, "update check failed: ${e.error} ${e.detail}")
            UpdateState.Failed(e.error, e.detail)
        } catch (e: IOException) {
            Log.w(TAG, "update check failed", e)
            UpdateState.Failed(UpdateError.Network, e.message)
        }
    }

    /**
     * Downloads [update]'s APK into the cache dir, reporting 0f..1f as it goes. Resumes a partial
     * download across attempts and verifies the finished size against the release metadata.
     *
     * @throws UpdateException on any failure; a non-network failure (4xx) is not retried.
     */
    suspend fun downloadApk(update: AvailableUpdate, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                throw UpdateException(UpdateError.Unknown, "mkdirs failed")
            }
            val target = File(downloadDir, "life-calendar-${update.versionName}.apk")
            val part = File(downloadDir, "${target.name}.part")
            val total = update.assetSizeBytes

            // Anything from a different version is dead weight — only this version's finished file
            // and its in-progress `.part` are worth keeping.
            downloadDir.listFiles()?.forEach {
                if (it.name != target.name && it.name != part.name) it.delete()
            }
            // Already complete: a previous attempt finished, or the user tapped download twice.
            if (total > 0 && target.length() == total) {
                onProgress(1f)
                return@withContext target
            }
            // A leftover of the wrong size is junk, not a resume point (that role belongs to `part`).
            if (target.exists()) target.delete()
            // A `.part` at or past the full size can't be resumed from: `Range: bytes=<total>-`
            // earns a 416, which is not a Network error and so is never retried — leaving the
            // updater stuck until someone clears the app's cache. Starting over costs one download.
            if (total > 0 && part.length() >= total) part.delete()

            var lastError: UpdateException? = null
            repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
                try {
                    val got = downloadOnce(update, part, total, onProgress)
                    if (total > 0 && got != total) {
                        // A server that closes the body early raises nothing: read() returning -1
                        // looks like a clean end, so length is the only thing that catches a stall.
                        throw UpdateException(UpdateError.Network, "truncated: $got/$total")
                    }
                    if (!part.renameTo(target)) {
                        throw UpdateException(UpdateError.Unknown, "rename failed")
                    }
                    onProgress(1f)
                    return@withContext target
                } catch (e: UpdateException) {
                    // Only a network fault is worth another go; a 401/404 fails identically forever.
                    if (e.error != UpdateError.Network) throw e
                    lastError = e
                    Log.w(TAG, "download attempt ${attempt + 1} failed (${e.detail}), will resume")
                } catch (e: IOException) {
                    // A connection dropped mid-body surfaces here, straight out of the read loop.
                    // CancellationException is not an IOException, so "cancel" still cancels.
                    lastError = UpdateException(UpdateError.Network, e.message)
                    Log.w(TAG, "download attempt ${attempt + 1} dropped mid-stream, will resume", e)
                }
                if (attempt < MAX_DOWNLOAD_ATTEMPTS - 1) {
                    delay(RETRY_BACKOFF_MILLIS * (attempt + 1))
                }
            }
            throw lastError ?: UpdateException(UpdateError.Network, "download failed")
        }

    /**
     * One download attempt, appending to [part] when the server honours our range request.
     *
     * @return the total number of bytes in [part] afterwards, resumed bytes included.
     */
    private suspend fun downloadOnce(
        update: AvailableUpdate,
        part: File,
        total: Long,
        onProgress: (Float) -> Unit,
    ): Long {
        val alreadyHave = part.length()
        // Always re-request the asset API URL rather than reusing the CDN URL a previous attempt
        // was redirected to: that one is pre-signed and its signature expires.
        val conn = open(
            url = update.assetUrl,
            // This header is what turns the asset API endpoint from JSON metadata into bytes.
            accept = "application/octet-stream",
            rangeFrom = alreadyHave,
        )
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw errorFor(code)
            // 206 means the range was honoured, so we append. Anything else (a plain 200) means the
            // server ignored it and is sending the whole file again — restart rather than append a
            // second full body onto a partial file.
            val resuming = code == HttpURLConnection.HTTP_PARTIAL && alreadyHave > 0
            // On a 206 the content length is the REMAINING byte count, not the file size, so the
            // size from the release metadata is the only figure that means "total" in both cases.
            val expected = total.takeIf { it > 0 }
                ?: conn.contentLengthLong.takeIf { it > 0 }?.plus(if (resuming) alreadyHave else 0L)
                ?: 0L
            var written = if (resuming) alreadyHave else 0L
            var lastReported = -1
            // Report the resumed position immediately, so the bar picks up where it left off rather
            // than appearing to restart from zero.
            if (expected > 0 && written > 0) onProgress(written.toFloat() / expected)

            conn.inputStream.use { input ->
                FileOutputStream(part, resuming).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    while (true) {
                        // Cooperative cancellation: backing out of the dialog kills the coroutine.
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        if (expected > 0) {
                            // Only emit on whole-percent changes — a 10MB APK would otherwise push
                            // thousands of recompositions.
                            val percent = (written * 100 / expected).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
            return written
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchLatestRelease(): JSONObject {
        val conn = open(
            url = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest",
            accept = "application/vnd.github+json",
        )
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw errorFor(code)
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            return runCatching { JSONObject(raw) }
                .getOrElse { throw UpdateException(UpdateError.Unknown, "malformed release JSON") }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Opens a GET on the GitHub API. Redirects are followed automatically: the asset endpoint 302s
     * to a pre-signed CDN URL on another host, and since no credential is attached there is nothing
     * that could leak across that hop.
     */
    private fun open(url: String, accept: String, rangeFrom: Long = 0L): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            requestMethod = "GET"
            setRequestProperty("Accept", accept)
            setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
            setRequestProperty("User-Agent", USER_AGENT)
            if (rangeFrom > 0) setRequestProperty("Range", "bytes=$rangeFrom-")
        }

    private fun errorFor(code: Int): UpdateException = when (code) {
        HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
            // 403 from an anonymous client is almost always the 60-req/hr rate limit.
            UpdateException(UpdateError.Unauthorized, "HTTP $code")
        HttpURLConnection.HTTP_NOT_FOUND -> UpdateException(UpdateError.NoRelease, "HTTP $code")
        in 500..599 -> UpdateException(UpdateError.Network, "HTTP $code")
        else -> UpdateException(UpdateError.Unknown, "HTTP $code")
    }

    private companion object {
        const val TAG = "UpdateService"
        const val GITHUB_API_VERSION = "2022-11-28"
        const val USER_AGENT = "life-calendar-updater"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val MAX_DOWNLOAD_ATTEMPTS = 3
        const val RETRY_BACKOFF_MILLIS = 1_000L
        const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
    }
}

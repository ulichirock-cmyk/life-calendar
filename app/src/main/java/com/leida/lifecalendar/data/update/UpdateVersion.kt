package com.leida.lifecalendar.data.update

/**
 * Version comparison for the in-app updater. Pure Kotlin, no Android — "is the GitHub release
 * newer than what's installed?" is the one piece of the updater that can go silently wrong (a bad
 * compare either nags forever or never offers the update), so it is kept isolated and small.
 *
 * The input is a git tag from the release workflow (`v1.1.0`, per
 * `.github/workflows/android-release.yml` triggering on `v*`) compared against
 * `BuildConfig.VERSION_NAME` (`1.0.0`). Both sides are normalized, so the leading `v` never matters.
 *
 * Semantics kept deliberately small — this project tags plain `vMAJOR.MINOR.PATCH`:
 *  - segments are compared numerically, left to right, missing segments read as 0
 *    (so `1.1` == `1.1.0`, and `1.10.0` > `1.9.0` — a plain string compare would get that wrong)
 *  - a pre-release suffix ranks BELOW the same numeric version (`1.1.0-beta1` < `1.1.0`), matching
 *    semver, so a beta tag never offers itself as an update to the final release
 *  - anything unparseable compares as "not newer", i.e. a malformed tag is ignored rather than
 *    prompting the user to install something we can't reason about
 */
object UpdateVersion {

    /** Strips the tag's leading `v`/`V` and surrounding whitespace. `"v1.1.0"` -> `"1.1.0"`. */
    fun normalize(raw: String): String = raw.trim().removePrefix("v").removePrefix("V").trim()

    /**
     * @return true when [remoteTag] denotes a strictly newer version than [currentVersionName].
     *   Equal versions and unparseable input both return false (never prompt).
     */
    fun isNewer(remoteTag: String, currentVersionName: String): Boolean {
        val remote = parse(normalize(remoteTag)) ?: return false
        val current = parse(normalize(currentVersionName)) ?: return false
        return compare(remote, current) > 0
    }

    private data class Parsed(val numbers: List<Int>, val preRelease: String?)

    /** null when the version has no leading numeric segment at all (e.g. `"latest"`). */
    private fun parse(version: String): Parsed? {
        if (version.isEmpty()) return null
        // Split the numeric core from any `-beta1` / `+build` suffix before touching the dots, so a
        // suffix containing a dot (`1.1.0-rc.1`) can't be mistaken for another version segment.
        val core = version.takeWhile { it.isDigit() || it == '.' }.trimEnd('.')
        if (core.isEmpty()) return null
        val suffix = version.substring(core.length).takeIf { it.isNotEmpty() }
        val numbers = core.split('.').map { it.toIntOrNull() ?: return null }
        return Parsed(numbers, suffix)
    }

    private fun compare(a: Parsed, b: Parsed): Int {
        for (i in 0 until maxOf(a.numbers.size, b.numbers.size)) {
            val cmp = (a.numbers.getOrElse(i) { 0 }).compareTo(b.numbers.getOrElse(i) { 0 })
            if (cmp != 0) return cmp
        }
        // Same numbers: absent suffix (the final release) outranks any pre-release suffix.
        return when {
            a.preRelease == null && b.preRelease == null -> 0
            a.preRelease == null -> 1
            b.preRelease == null -> -1
            else -> a.preRelease.compareTo(b.preRelease)
        }
    }
}

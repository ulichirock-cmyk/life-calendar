package com.leida.lifecalendar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * SharedPreferences-backed persistence. Small enough that a JSON blob beats a database,
 * and it keeps the app dependency-free.
 */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("life_calendar", Context.MODE_PRIVATE)

    fun loadSettings(): Settings {
        if (!prefs.contains(KEY_BIRTH)) return Settings()
        return Settings(
            birth = runCatching { LocalDate.parse(prefs.getString(KEY_BIRTH, "")!!) }
                .getOrElse { Settings().birth },
            span = prefs.getInt(KEY_SPAN, 80),
            showStages = prefs.getBoolean(KEY_STAGES, true),
            showQuote = prefs.getBoolean(KEY_QUOTE, true),
        )
    }

    fun saveSettings(s: Settings) {
        prefs.edit()
            .putString(KEY_BIRTH, s.birth.toString())
            .putInt(KEY_SPAN, s.span)
            .putBoolean(KEY_STAGES, s.showStages)
            .putBoolean(KEY_QUOTE, s.showQuote)
            .apply()
    }

    fun loadMilestones(): List<Milestone> {
        val raw = prefs.getString(KEY_MILESTONES, null) ?: return DEFAULT_MILESTONES
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Milestone(o.getLong("id"), o.getInt("w"), o.getString("label"))
            }
        }.getOrElse { DEFAULT_MILESTONES }
    }

    fun saveMilestones(list: List<Milestone>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("id", m.id).put("w", m.week).put("label", m.label))
        }
        prefs.edit().putString(KEY_MILESTONES, arr.toString()).apply()
    }

    /** Epoch millis of the last update check, so a cold start checks at most once a day. */
    fun lastUpdateCheck(): Long = prefs.getLong(KEY_LAST_UPDATE_CHECK, 0L)

    fun saveLastUpdateCheck(millis: Long) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK, millis).apply()
    }

    private companion object {
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_BIRTH = "birth"
        const val KEY_SPAN = "span"
        const val KEY_STAGES = "show_stages"
        const val KEY_QUOTE = "show_quote"
        const val KEY_MILESTONES = "milestones"
    }
}

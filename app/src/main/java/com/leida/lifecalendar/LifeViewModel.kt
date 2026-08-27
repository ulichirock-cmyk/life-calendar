package com.leida.lifecalendar

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.leida.lifecalendar.data.LifeCalc
import com.leida.lifecalendar.data.Milestone
import com.leida.lifecalendar.data.Settings
import com.leida.lifecalendar.data.Store
import java.time.LocalDate

class LifeViewModel(app: Application) : AndroidViewModel(app) {

    private val store = Store(app)

    var settings by mutableStateOf(store.loadSettings())
        private set

    var milestones by mutableStateOf(store.loadMilestones())
        private set

    /** Recomputed whenever settings change; the date is read once per process start. */
    val calc: LifeCalc get() = LifeCalc(settings)

    fun setBirth(date: LocalDate) = update(settings.copy(birth = date))

    fun setSpan(span: Int) {
        if (span != settings.span) update(settings.copy(span = span))
    }

    fun toggleStages() = update(settings.copy(showStages = !settings.showStages))

    fun toggleQuote() = update(settings.copy(showQuote = !settings.showQuote))

    /** Marks the middle week of a year of life, as the year-pillar prototype does. */
    fun markYear(year: Int, label: String): String {
        val text = label.trim().ifEmpty { "一个刻痕" }
        val next = milestones + Milestone(System.currentTimeMillis(), year * 52 + 26, text)
        milestones = next
        store.saveMilestones(next)
        return "已在 $year 岁留下「$text」"
    }

    fun removeMilestone(id: Long) {
        val next = milestones.filterNot { it.id == id }
        milestones = next
        store.saveMilestones(next)
    }

    private fun update(next: Settings) {
        settings = next
        store.saveSettings(next)
    }
}

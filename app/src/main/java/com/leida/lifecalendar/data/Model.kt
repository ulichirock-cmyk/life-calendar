package com.leida.lifecalendar.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** A mark left on one week of the calendar. `week` is the week index counted from birth. */
data class Milestone(val id: Long, val week: Int, val label: String)

data class Settings(
    val birth: LocalDate = LocalDate.of(1994, 6, 15),
    val span: Int = 80,
    val showStages: Boolean = true,
    val showQuote: Boolean = true,
)

val DEFAULT_MILESTONES = listOf(
    Milestone(1L, 52 * 18 + 32, "离开家乡"),
    Milestone(2L, 52 * 22 + 20, "第一份工作"),
    Milestone(3L, 52 * 29 + 6, "搬到海边"),
)

val QUOTES = listOf(
    "你不能延长它的长度，但可以决定每一格的颜色。",
    "所有未填的格子都还空着——这既是提醒，也是余地。",
    "一周很短，短到常常被忽略；四千周很短，短到不该被忽略。",
    "把今天当作一格，而不是一段无限的走廊。",
    "时间不会加速，只是我们越走越少回头。",
)

/**
 * The prototype's `calc()`, ported one-for-one so the numbers on screen match it exactly:
 * a week is a flat 7 days from the birth date, never a calendar week.
 */
class LifeCalc(val settings: Settings, val today: LocalDate = LocalDate.now()) {

    val span: Int = settings.span
    val totalWeeks: Int = span * 52

    /** Whole 7-day periods elapsed since birth, clamped to the span. */
    val livedWeeks: Int = run {
        val days = ChronoUnit.DAYS.between(settings.birth, today)
        max(0L, min(totalWeeks.toLong(), floor(days / 7.0).toLong())).toInt()
    }

    val currentYear: Int = livedWeeks / 52
    val percent: Double = livedWeeks.toDouble() / totalWeeks * 100.0

    /** `pct.toFixed(1) + '%'` from the prototype. */
    val percentLabel: String = String.format(Locale.US, "%.1f%%", percent)

    /** `span * 12 - floor(livedW / 4.345)` — the prototype's month arithmetic, kept as-is. */
    val monthsLeft: Int = max(0, span * 12 - floor(livedWeeks / 4.345).toInt())

    /** Weeks already spent inside a given year of life, 0..52. */
    fun livedIn(year: Int): Int = max(0, min(52, livedWeeks - year * 52))

    /** 0 = 童年 (<18), 1 = 工作 (18–60), 2 = 退休 (60+). */
    fun stageOf(year: Int): Int = when {
        year < 18 -> 0
        year < 60 -> 1
        else -> 2
    }

    /** The calendar date a week index lands on. */
    fun dateOfWeek(week: Int): LocalDate = settings.birth.plusDays(week * 7L)

    val quote: String = QUOTES[today.dayOfMonth % QUOTES.size]

    companion object {
        fun group(n: Int): String = String.format(Locale.US, "%,d", n)

        fun todayIn(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)
    }
}

/** Milestones bucketed by year of life, matching `msByYear` in the prototype. */
fun List<Milestone>.byYear(): Map<Int, List<Milestone>> = groupBy { it.week / 52 }

package com.somkov.greek_b1.progress

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The weeks of the calendar of the days from [first] to [last], each from Monday to Sunday, as
 * the calendar in Cyprus has them. The days of the first and the last week outside the range are
 * null.
 */
fun calendarWeeks(first: LocalDate, last: LocalDate): List<List<LocalDate?>> {
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { it <= end }
        .map { if (it < first || it > last) null else it }
        .chunked(7)
        .toList()
}

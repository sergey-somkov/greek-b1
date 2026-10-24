package com.somkov.greek_b1.progress

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarTest {
    @Test
    fun `the weeks run from Monday to Sunday, with the days outside the range empty`() {
        // Wed 28 Oct 2026 to Mon 11 Jan 2027.
        val weeks = calendarWeeks(LocalDate.of(2026, 10, 28), LocalDate.of(2027, 1, 11))

        assertEquals(12, weeks.size)
        assertEquals(listOf(7), weeks.map { it.size }.distinct())
        assertEquals(listOf(null, null), weeks.first().take(2))
        assertEquals(LocalDate.of(2026, 10, 28), weeks.first()[2])
        assertEquals(LocalDate.of(2027, 1, 11), weeks.last()[0])
        assertEquals(List(6) { null }, weeks.last().drop(1))
        assertEquals(76, weeks.flatten().count { it != null })
        for (week in weeks) {
            week.forEachIndexed { index, day -> if (day != null) assertEquals(DayOfWeek.of(index + 1), day.dayOfWeek) }
        }
    }

    @Test
    fun `a single day is one week`() {
        val sunday = LocalDate.of(2026, 11, 1)
        val weeks = calendarWeeks(sunday, sunday)

        assertEquals(1, weeks.size)
        assertEquals(sunday, weeks.single()[6])
        weeks.single().take(6).forEach { assertNull(it) }
    }
}

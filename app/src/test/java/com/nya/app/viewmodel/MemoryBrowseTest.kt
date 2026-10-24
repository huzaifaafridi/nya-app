package com.nya.app.viewmodel

import com.nya.app.data.Memory
import com.nya.app.data.Scene
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryBrowseTest {
    private val zone = ZoneId.of("Asia/Karachi")
    private val now = at(LocalDateTime.of(2026, 10, 24, 15, 0))
    private val today = LocalDate.of(2026, 10, 24)

    private fun at(t: LocalDateTime) = t.atZone(zone).toInstant().toEpochMilli()

    private fun memory(id: Long, title: String = "t", place: String = "p", note: String = "n",
                       time: Long = now, clip: Int? = null) =
        Memory(id, title, place, note, Scene.Sea, time, clip)

    private fun ids(query: String = "", filter: MemoryFilter = MemoryFilter.All, all: List<Memory>) =
        filterMemories(all, query, filter, now, zone).map { it.id }

    private val sunset = memory(1, "Sunset at the pier", "Karachi", "Golden hour by the sea")
    private val chai = memory(2, "Chai", "Home", "Warm cup")

    @Test
    fun emptyQueryKeepsEverything() = assertEquals(listOf(1L, 2L), ids("  ", all = listOf(sunset, chai)))

    @Test
    fun matchingIsCaseInsensitiveAndTrimmed() = assertEquals(listOf(1L), ids("  SUNSET ", all = listOf(sunset, chai)))

    @Test
    fun matchesTitlePlaceAndNote() {
        val all = listOf(sunset, chai)
        assertEquals(listOf(1L), ids("pier", all = all))
        assertEquals(listOf(2L), ids("home", all = all))
        assertEquals(listOf(2L), ids("cup", all = all))
    }

    @Test
    fun everyWordMustMatchSomewhere() {
        val all = listOf(sunset, chai)
        assertEquals(listOf(1L), ids("sunset sea", all = all))
        assertEquals(listOf(1L), ids("karachi   golden", all = all))
        assertEquals(emptyList<Long>(), ids("sunset chai", all = all))
    }

    @Test
    fun photosAndClipsSplitOnClipSeconds() {
        val all = listOf(memory(1), memory(2, clip = 8))
        assertEquals(listOf(1L), ids(filter = MemoryFilter.Photos, all = all))
        assertEquals(listOf(2L), ids(filter = MemoryFilter.Clips, all = all))
    }

    @Test
    fun todayUsesTheDeviceCalendarDay() {
        val all = listOf(
            memory(1, time = at(LocalDateTime.of(2026, 10, 24, 0, 0))),
            memory(2, time = at(LocalDateTime.of(2026, 10, 23, 23, 59))),
            memory(3, time = at(LocalDateTime.of(2026, 10, 24, 23, 59)))
        )
        assertEquals(listOf(1L, 3L), ids(filter = MemoryFilter.Today, all = all))
    }

    @Test
    fun thisWeekIsTheLastSevenDaysIncludingToday() {
        val all = listOf(
            memory(1, time = at(LocalDateTime.of(2026, 10, 18, 0, 0))),
            memory(2, time = at(LocalDateTime.of(2026, 10, 17, 23, 59))),
            memory(3)
        )
        assertEquals(listOf(1L, 3L), ids(filter = MemoryFilter.Week, all = all))
    }

    @Test
    fun searchAndFilterCombine() {
        val all = listOf(memory(1, "Sunset", clip = 5), memory(2, "Sunset"), memory(3, "Chai", clip = 5))
        assertEquals(listOf(1L), ids("sunset", MemoryFilter.Clips, all))
    }

    @Test
    fun groupsByDayWithFriendlyLabels() {
        val items = listOf(
            memory(1),
            memory(2, time = at(LocalDateTime.of(2026, 10, 24, 1, 0))),
            memory(3, time = at(LocalDateTime.of(2026, 10, 23, 22, 0))),
            memory(4, time = at(LocalDateTime.of(2026, 10, 12, 9, 0)))
        )
        val sections = groupByDay(items, today, zone, Locale.ENGLISH)
        assertEquals(listOf("TODAY", "YESTERDAY", "MON, 12 OCT"), sections.map { it.label })
        assertEquals(listOf(listOf(1L, 2L), listOf(3L), listOf(4L)), sections.map { s -> s.items.map { it.id } })
    }
}

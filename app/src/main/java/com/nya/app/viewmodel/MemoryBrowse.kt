package com.nya.app.viewmodel

import com.nya.app.data.Memory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MemoryFilter(val label: String) {
    All("All"), Photos("Photos"), Clips("Clips"), Today("Today"), Week("This week")
}

/** A run of memories created on the same day, under a [label] such as "TODAY". */
data class DaySection(val label: String, val items: List<Memory>)

private val ShortDay = DateTimeFormatter.ofPattern("EEE, d MMM")

private fun Long.day(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Memories matching [filter] (relative to [now] in [zone]) whose title, place and note contain every word of [query]. */
fun filterMemories(all: List<Memory>, query: String, filter: MemoryFilter, now: Long, zone: ZoneId): List<Memory> {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val today = now.day(zone)
    return all.filter { m ->
        val text = "${m.title} ${m.place} ${m.note}".lowercase()
        val date = m.createdAt.day(zone)
        words.all { it in text } && when (filter) {
            MemoryFilter.All -> true
            MemoryFilter.Photos -> m.clipSeconds == null
            MemoryFilter.Clips -> m.clipSeconds != null
            MemoryFilter.Today -> date == today
            MemoryFilter.Week -> date >= today.minusDays(6)
        }
    }
}

/** Splits [items] into per-day sections, keeping their order: "TODAY", "YESTERDAY", then e.g. "MON, 12 OCT". */
fun groupByDay(items: List<Memory>, today: LocalDate, zone: ZoneId, locale: Locale = Locale.getDefault()): List<DaySection> =
    items.groupBy { it.createdAt.day(zone) }.map { (date, group) ->
        val label = when {
            date >= today -> "TODAY"
            date == today.minusDays(1) -> "YESTERDAY"
            else -> date.format(ShortDay.withLocale(locale)).uppercase(locale)
        }
        DaySection(label, group)
    }

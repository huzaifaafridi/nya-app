package com.nya.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val Clock = DateTimeFormatter.ofPattern("h:mm a")
private val Weekday = DateTimeFormatter.ofPattern("EEE")
private val DayMonth = DateTimeFormatter.ofPattern("d MMM")

/** "Today, 6:42 PM", "Yesterday, 11:10 AM", "Mon, 7:05 AM" within a week, else "12 Mar, 7:05 AM". */
fun memoryTime(createdAt: Long): String {
    val zone = ZoneId.systemDefault()
    val t = Instant.ofEpochMilli(createdAt).atZone(zone)
    val day = when (ChronoUnit.DAYS.between(t.toLocalDate(), LocalDate.now(zone))) {
        in Long.MIN_VALUE..0 -> "Today"
        1L -> "Yesterday"
        in 2..6 -> t.format(Weekday)
        else -> t.format(DayMonth)
    }
    return "$day, ${t.format(Clock)}"
}

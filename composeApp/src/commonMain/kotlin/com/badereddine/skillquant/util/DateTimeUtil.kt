package com.badereddine.skillquant.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Long.toRelativeTimeString(): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - this
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 30 -> "${days / 30}mo ago"
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

fun Long.toFormattedDate(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return "${months[localDateTime.monthNumber - 1]} ${localDateTime.dayOfMonth}"
}

fun Long.toFormattedDateTime(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    return "${months[localDateTime.monthNumber - 1]} ${localDateTime.dayOfMonth}, " +
            "${localDateTime.hour.toString().padStart(2, '0')}:" +
            localDateTime.minute.toString().padStart(2, '0')
}

fun Double.toSalaryString(): String {
    return when {
        this >= 1_000_000 -> "$${(this / 1_000_000).format(1)}M"
        this >= 1_000 -> "$${(this / 1_000).format(0)}K"
        else -> "$${this.format(0)}"
    }
}

fun Double.toSalaryString(location: String): String {
    val symbol = currencySymbol(location)
    return when {
        this >= 1_000_000 -> "$symbol${(this / 1_000_000).format(1)}M"
        this >= 1_000 -> "$symbol${(this / 1_000).format(0)}K"
        else -> "$symbol${this.format(0)}"
    }
}

fun Long.toSalaryString(): String = this.toDouble().toSalaryString()
fun Long.toSalaryString(location: String): String = this.toDouble().toSalaryString(location)

fun currencySymbol(location: String): String = when (location.lowercase()) {
    "morocco" -> "MAD "
    "france" -> "€"
    else -> "$"
}

fun currencyCode(location: String): String = when (location.lowercase()) {
    "morocco" -> "MAD"
    "france" -> "EUR"
    else -> "USD"
}

fun Double.toPercentString(): String {
    val sign = if (this >= 0) "+" else ""
    return "$sign${this.format(1)}%"
}

fun Double.format(decimals: Int): String {
    // Simple multiplatform-safe rounding
    var factor = 1.0
    repeat(decimals) { factor *= 10.0 }
    val rounded = kotlin.math.round(this * factor) / factor
    return if (decimals == 0) rounded.toLong().toString()
    else {
        val str = rounded.toString()
        val dotIdx = str.indexOf('.')
        if (dotIdx == -1) "$str.${"0".repeat(decimals)}"
        else {
            val currentDecimals = str.length - dotIdx - 1
            if (currentDecimals >= decimals) str.substring(0, dotIdx + decimals + 1)
            else str + "0".repeat(decimals - currentDecimals)
        }
    }
}


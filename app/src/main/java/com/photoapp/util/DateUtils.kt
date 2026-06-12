package com.photoapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val fullDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    fun getDateGroup(timestamp: Long): String {
        val now = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, date) -> "Today"
            isYesterday(now, date) -> "Yesterday"
            isSameWeek(now, date) -> "This Week"
            isSameMonth(now, date) -> "This Month"
            isLastMonth(now, date) -> "Last Month"
            isSameYear(now, date) -> monthYearFormat.format(Date(timestamp))
            else -> monthYearFormat.format(Date(timestamp))
        }
    }

    fun formatDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun getDaysRemaining(dateDeleted: Long, retentionDays: Int = 30): Int {
        val now = System.currentTimeMillis()
        val elapsedMillis = now - dateDeleted
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
        return (retentionDays - elapsedDays).coerceAtLeast(0)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, date)
    }

    private fun isSameWeek(now: Calendar, date: Calendar): Boolean {
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == date.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(now: Calendar, date: Calendar): Boolean {
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == date.get(Calendar.MONTH)
    }

    private fun isLastMonth(now: Calendar, date: Calendar): Boolean {
        val lastMonth = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.MONTH, -1)
        }
        return lastMonth.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                lastMonth.get(Calendar.MONTH) == date.get(Calendar.MONTH)
    }

    private fun isSameYear(now: Calendar, date: Calendar): Boolean {
        return now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
    }
}

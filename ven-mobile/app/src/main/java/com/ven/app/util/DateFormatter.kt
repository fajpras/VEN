package com.ven.app.util

import com.ven.app.R
import com.ven.app.core.localization.AppStrings
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitas pemformat tanggal untuk aplikasi VEN.
 * Sesuai spesifikasi arsitektur DESIGN.md A3.2 & A8.6.1.
 */
object DateFormatter {

    private val utcDatePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    private val localDatePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    )

    /**
     * Memformat tanggal menjadi format "Sat, 26 September 2026".
     */
    fun formatRequestMeetingDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", AppStrings.locale()))
    }

    /**
     * Memformat YearMonth menjadi format "September 2026".
     */
    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", AppStrings.locale()))
    }

    /**
     * Mengubah format tanggal ISO / SQL datetime menjadi format waktu relatif ala Instagram
     * (mis. "Just now", "2 hours ago", "1 day ago", "3 days ago").
     */
    fun formatRelativeTime(dateString: String?, nowMillis: Long = System.currentTimeMillis()): String {
        if (dateString.isNullOrBlank()) return localized(R.string.time_just_now)

        var parsedMillis: Long? = null

        // Coba parsing pola UTC bila ada akhiran 'Z'
        if (dateString.endsWith("Z", ignoreCase = true)) {
            for (pattern in utcDatePatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = sdf.parse(dateString)
                    if (date != null) {
                        parsedMillis = date.time
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }

        // Coba parsing pola lokal / tanpa timezone
        if (parsedMillis == null) {
            val allPatterns = if (dateString.endsWith("Z", ignoreCase = true)) localDatePatterns else (localDatePatterns + utcDatePatterns)
            for (pattern in allPatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                    val date = sdf.parse(dateString)
                    if (date != null) {
                        parsedMillis = date.time
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (parsedMillis == null) {
            return dateString
        }

        val diffMillis = nowMillis - parsedMillis
        if (diffMillis <= 0) return localized(R.string.time_just_now)

        val seconds = diffMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 60 -> localized(R.string.time_just_now)
            minutes < 60 -> if (minutes == 1L) {
                localized(R.string.time_minute_ago)
            } else {
                localized(R.string.time_minutes_ago, minutes)
            }
            hours < 24 -> if (hours == 1L) {
                localized(R.string.time_hour_ago)
            } else {
                localized(R.string.time_hours_ago, hours)
            }
            days < 7 -> if (days == 1L) {
                localized(R.string.time_day_ago)
            } else {
                localized(R.string.time_days_ago, days)
            }
            weeks < 4 -> if (weeks == 1L) {
                localized(R.string.time_week_ago)
            } else {
                localized(R.string.time_weeks_ago, weeks)
            }
            months < 12 -> if (months == 1L) {
                localized(R.string.time_month_ago)
            } else {
                localized(R.string.time_months_ago, months)
            }
            else -> if (years == 1L) {
                localized(R.string.time_year_ago)
            } else {
                localized(R.string.time_years_ago, years)
            }
        }
    }

    private fun localized(resId: Int, vararg args: Any): String {
        val value = AppStrings.get(resId, *args)
        if (value.isNotBlank()) return value

        val fallback = when (resId) {
            R.string.time_just_now -> "Just now"
            R.string.time_minute_ago -> "1 minute ago"
            R.string.time_minutes_ago -> "%d minutes ago"
            R.string.time_hour_ago -> "1 hour ago"
            R.string.time_hours_ago -> "%d hours ago"
            R.string.time_day_ago -> "1 day ago"
            R.string.time_days_ago -> "%d days ago"
            R.string.time_week_ago -> "1 week ago"
            R.string.time_weeks_ago -> "%d weeks ago"
            R.string.time_month_ago -> "1 month ago"
            R.string.time_months_ago -> "%d months ago"
            R.string.time_year_ago -> "1 year ago"
            R.string.time_years_ago -> "%d years ago"
            else -> ""
        }

        return if (args.isEmpty()) fallback else String.format(Locale.ENGLISH, fallback, *args)
    }

    /**
     * Mem-parsing string tanggal ISO / SQL datetime ke epoch millis.
     */
    fun parseIsoToMillis(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        if (dateString.endsWith("Z", ignoreCase = true)) {
            for (pattern in utcDatePatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val date = sdf.parse(dateString)
                    if (date != null) return date.time
                } catch (_: Exception) {
                }
            }
        }
        for (pattern in localDatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Mengecek apakah string tanggal/waktu yang diberikan sudah lewat dari waktu saat ini.
     */
    fun isPastDateTime(dateString: String?): Boolean {
        val millis = parseIsoToMillis(dateString) ?: return false
        return millis < System.currentTimeMillis()
    }

    /**
     * Memformat string waktu ke format tanggal ringkas (mis. "24 Sep 2026").
     */
    fun formatScheduleDate(dateString: String?): String {
        val millis = parseIsoToMillis(dateString) ?: return dateString.orEmpty()
        val sdf = SimpleDateFormat("d MMM yyyy", AppStrings.locale())
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Memformat string waktu ke format jam ringkas (mis. "02:00 PM").
     */
    fun formatScheduleTime(dateString: String?): String {
        val millis = parseIsoToMillis(dateString) ?: return dateString.orEmpty()
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        return sdf.format(java.util.Date(millis))
    }

    /**
     * Memformat string waktu ke format lengkap (mis. "Thursday, 24 September 2026 at 02:00 PM").
     */
    fun formatScheduleFull(dateString: String?): String {
        val millis = parseIsoToMillis(dateString) ?: return dateString.orEmpty()
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy 'at' hh:mm a", AppStrings.locale())
        return sdf.format(java.util.Date(millis))
    }
}

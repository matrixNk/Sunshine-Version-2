/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app

import android.content.Context
import android.preference.PreferenceManager
import android.text.format.Time
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

object Utility {

    const val DATE_FORMAT = "yyyyMMdd"

    fun getPreferredLocation(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(
            context.getString(R.string.pref_location_key),
            context.getString(R.string.pref_location_default)
        )
    }

    fun isMetric(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(
            context.getString(R.string.pref_units_key),
            context.getString(R.string.pref_units_metric)
        ) == context.getString(R.string.pref_units_metric)
    }

    fun formatTemperature(context: Context, temperature: Double): String {
        var temp = temperature
        if (!isMetric(context)) {
            temp = temp * 1.8 + 32
        }
        return String.format(context.getString(R.string.format_temperature), temp)
    }

    fun formatDate(dateInMilliseconds: Long): String {
        val date = Date(dateInMilliseconds)
        return DateFormat.getDateInstance().format(date)
    }

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    fun getFriendlyDayString(context: Context, dateInMillis: Long): String {
        val time = Time()
        time.setToNow()
        val currentTime = System.currentTimeMillis()
        val julianDay = Time.getJulianDay(dateInMillis, time.gmtoff)
        val currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff)

        return if (julianDay == currentJulianDay) {
            val today = context.getString(R.string.today)
            val formatId = R.string.format_full_friendly_date
            String.format(
                context.getString(formatId),
                today,
                getFormattedMonthDay(context, dateInMillis)
            )
        } else if (julianDay < currentJulianDay + 7) {
            getDayName(context, dateInMillis)
        } else {
            val shortenedDateFormat = SimpleDateFormat("EEE MMM dd")
            shortenedDateFormat.format(dateInMillis)
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return
     */
    fun getDayName(context: Context, dateInMillis: Long): String {
        val t = Time()
        t.setToNow()
        val julianDay = Time.getJulianDay(dateInMillis, t.gmtoff)
        val currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff)
        return when {
            julianDay == currentJulianDay -> context.getString(R.string.today)
            julianDay == currentJulianDay + 1 -> context.getString(R.string.tomorrow)
            else -> {
                val dayFormat = SimpleDateFormat("EEEE")
                dayFormat.format(dateInMillis)
            }
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMillis The db formatted date string
     * @return The day in the form of a string formatted "December 6"
     */
    fun getFormattedMonthDay(context: Context, dateInMillis: Long): String {
        val monthDayFormat = SimpleDateFormat("MMMM dd")
        return monthDayFormat.format(dateInMillis)
    }

    fun getFormattedWind(context: Context, windSpeed: Float, degrees: Float): String {
        val windFormat = if (isMetric(context)) R.string.format_wind_kmh else {
            R.string.format_wind_mph
        }
        val speed = if (isMetric(context)) windSpeed else windSpeed * 0.621371192237334f

        val direction = when {
            degrees >= 337.5 || degrees < 22.5 -> "N"
            degrees >= 22.5 && degrees < 67.5 -> "NE"
            degrees >= 67.5 && degrees < 112.5 -> "E"
            degrees >= 112.5 && degrees < 157.5 -> "SE"
            degrees >= 157.5 && degrees < 202.5 -> "S"
            degrees >= 202.5 && degrees < 247.5 -> "SW"
            degrees >= 247.5 && degrees < 292.5 -> "W"
            degrees >= 292.5 && degrees < 337.5 -> "NW"
            else -> "Unknown"
        }
        return String.format(context.getString(windFormat), speed, direction)
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    fun getIconResourceForWeatherCondition(weatherId: Int): Int {
        return when {
            weatherId in 200..232 -> R.drawable.ic_storm
            weatherId in 300..321 -> R.drawable.ic_light_rain
            weatherId in 500..504 -> R.drawable.ic_rain
            weatherId == 511 -> R.drawable.ic_snow
            weatherId in 520..531 -> R.drawable.ic_rain
            weatherId in 600..622 -> R.drawable.ic_snow
            weatherId in 701..761 -> R.drawable.ic_fog
            weatherId == 761 || weatherId == 781 -> R.drawable.ic_storm
            weatherId == 800 -> R.drawable.ic_clear
            weatherId == 801 -> R.drawable.ic_light_clouds
            weatherId in 802..804 -> R.drawable.ic_cloudy
            else -> -1
        }
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    fun getArtResourceForWeatherCondition(weatherId: Int): Int {
        return when {
            weatherId in 200..232 -> R.drawable.art_storm
            weatherId in 300..321 -> R.drawable.art_light_rain
            weatherId in 500..504 -> R.drawable.art_rain
            weatherId == 511 -> R.drawable.art_snow
            weatherId in 520..531 -> R.drawable.art_rain
            weatherId in 600..622 -> R.drawable.art_snow
            weatherId in 701..761 -> R.drawable.art_fog
            weatherId == 761 || weatherId == 781 -> R.drawable.art_storm
            weatherId == 800 -> R.drawable.art_clear
            weatherId == 801 -> R.drawable.art_light_clouds
            weatherId in 802..804 -> R.drawable.art_clouds
            else -> -1
        }
    }
}

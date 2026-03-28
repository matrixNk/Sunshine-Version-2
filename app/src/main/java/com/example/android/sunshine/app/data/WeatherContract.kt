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
package com.example.android.sunshine.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns
import android.text.format.Time

/**
 * Defines table and column names for the weather database.
 */
object WeatherContract {

    val CONTENT_AUTHORITY = "com.example.android.sunshine.app"

    val BASE_CONTENT_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

    val PATH_WEATHER = "weather"
    val PATH_LOCATION = "location"

    fun normalizeDate(startDate: Long): Long {
        val time = Time()
        time.set(startDate)
        val julianDay = Time.getJulianDay(startDate, time.gmtoff)
        return time.setJulianDay(julianDay)
    }

    /* Inner class that defines the table contents of the location table */
    object LocationEntry : BaseColumns {

        const val _ID = "_id"

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION).build()

        val CONTENT_TYPE =
            "${ContentResolver.CURSOR_DIR_BASE_TYPE}/$CONTENT_AUTHORITY/$PATH_LOCATION"
        val CONTENT_ITEM_TYPE =
            "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/$CONTENT_AUTHORITY/$PATH_LOCATION"

        const val TABLE_NAME = "location"

        const val COLUMN_LOCATION_SETTING = "location_setting"

        const val COLUMN_CITY_NAME = "city_name"

        const val COLUMN_COORD_LAT = "coord_lat"
        const val COLUMN_COORD_LONG = "coord_long"

        fun buildLocationUri(id: Long): Uri = ContentUris.withAppendedId(CONTENT_URI, id)
    }

    /* Inner class that defines the table contents of the weather table */
    object WeatherEntry : BaseColumns {

        const val _ID = "_id"

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(PATH_WEATHER).build()

        val CONTENT_TYPE =
            "${ContentResolver.CURSOR_DIR_BASE_TYPE}/$CONTENT_AUTHORITY/$PATH_WEATHER"
        val CONTENT_ITEM_TYPE =
            "${ContentResolver.CURSOR_ITEM_BASE_TYPE}/$CONTENT_AUTHORITY/$PATH_WEATHER"

        const val TABLE_NAME = "weather"

        const val COLUMN_LOC_KEY = "location_id"
        const val COLUMN_DATE = "date"
        const val COLUMN_WEATHER_ID = "weather_id"

        const val COLUMN_SHORT_DESC = "short_desc"

        const val COLUMN_MIN_TEMP = "min"
        const val COLUMN_MAX_TEMP = "max"

        const val COLUMN_HUMIDITY = "humidity"

        const val COLUMN_PRESSURE = "pressure"

        const val COLUMN_WIND_SPEED = "wind"

        const val COLUMN_DEGREES = "degrees"

        fun buildWeatherUri(id: Long): Uri = ContentUris.withAppendedId(CONTENT_URI, id)

        fun buildWeatherLocation(locationSetting: String): Uri =
            CONTENT_URI.buildUpon().appendPath(locationSetting).build()

        fun buildWeatherLocationWithStartDate(locationSetting: String, startDate: Long): Uri {
            val normalizedDate = normalizeDate(startDate)
            return CONTENT_URI.buildUpon()
                .appendPath(locationSetting)
                .appendQueryParameter(COLUMN_DATE, normalizedDate.toString())
                .build()
        }

        fun buildWeatherLocationWithDate(locationSetting: String, date: Long): Uri =
            CONTENT_URI.buildUpon()
                .appendPath(locationSetting)
                .appendPath(normalizeDate(date).toString())
                .build()

        fun getLocationSettingFromUri(uri: Uri): String = uri.pathSegments[1]

        fun getDateFromUri(uri: Uri): Long = uri.pathSegments[2].toLong()

        fun getStartDateFromUri(uri: Uri): Long {
            val dateString = uri.getQueryParameter(COLUMN_DATE)
            return if (!dateString.isNullOrEmpty()) dateString.toLong() else 0L
        }
    }
}

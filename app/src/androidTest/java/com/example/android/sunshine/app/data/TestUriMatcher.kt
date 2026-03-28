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

import android.content.UriMatcher
import android.net.Uri
import android.test.AndroidTestCase

class TestUriMatcher : AndroidTestCase() {

    companion object {
        private const val LOCATION_QUERY = "London, UK"
        private const val TEST_DATE = 1419033600L  // December 20th, 2014
        private const val TEST_LOCATION_ID = 10L

        private val TEST_WEATHER_DIR: Uri = WeatherContract.WeatherEntry.CONTENT_URI
        private val TEST_WEATHER_WITH_LOCATION_DIR: Uri =
            WeatherContract.WeatherEntry.buildWeatherLocation(LOCATION_QUERY)
        private val TEST_WEATHER_WITH_LOCATION_AND_DATE_DIR: Uri =
            WeatherContract.WeatherEntry.buildWeatherLocationWithDate(LOCATION_QUERY, TEST_DATE)
        private val TEST_LOCATION_DIR: Uri = WeatherContract.LocationEntry.CONTENT_URI
    }

    fun testUriMatcher() {
        val testMatcher = WeatherProvider.buildUriMatcher()

        assertEquals(
            "Error: The WEATHER URI was matched incorrectly.",
            testMatcher.match(TEST_WEATHER_DIR),
            WeatherProvider.WEATHER
        )
        assertEquals(
            "Error: The WEATHER WITH LOCATION URI was matched incorrectly.",
            testMatcher.match(TEST_WEATHER_WITH_LOCATION_DIR),
            WeatherProvider.WEATHER_WITH_LOCATION
        )
        assertEquals(
            "Error: The WEATHER WITH LOCATION AND DATE URI was matched incorrectly.",
            testMatcher.match(TEST_WEATHER_WITH_LOCATION_AND_DATE_DIR),
            WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE
        )
        assertEquals(
            "Error: The LOCATION URI was matched incorrectly.",
            testMatcher.match(TEST_LOCATION_DIR),
            WeatherProvider.LOCATION
        )
    }
}

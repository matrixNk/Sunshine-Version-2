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

import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.test.AndroidTestCase
import android.util.Log
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry

class TestProvider : AndroidTestCase() {

    companion object {
        val LOG_TAG: String = TestProvider::class.java.simpleName
        private const val BULK_INSERT_RECORDS_TO_INSERT = 10

        fun createBulkInsertWeatherValues(locationRowId: Long): Array<ContentValues> {
            var currentTestDate = TestUtilities.TEST_DATE
            val millisecondsInADay = 1000L * 60 * 60 * 24
            return Array(BULK_INSERT_RECORDS_TO_INSERT) { i ->
                ContentValues().apply {
                    put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId)
                    put(WeatherContract.WeatherEntry.COLUMN_DATE, currentTestDate)
                    put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1)
                    put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2 + 0.01f * i)
                    put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3 - 0.01f * i)
                    put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75 + i)
                    put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65 - i)
                    put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "Asteroids")
                    put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5 + 0.2f * i)
                    put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321)
                    currentTestDate += millisecondsInADay
                }
            }
        }
    }

    fun deleteAllRecordsFromProvider() {
        mContext.contentResolver.delete(WeatherEntry.CONTENT_URI, null, null)
        mContext.contentResolver.delete(LocationEntry.CONTENT_URI, null, null)

        var cursor = mContext.contentResolver.query(
            WeatherEntry.CONTENT_URI, null, null, null, null
        )
        assertEquals("Error: Records not deleted from Weather table during delete", 0, cursor.count)
        cursor.close()

        cursor = mContext.contentResolver.query(
            LocationEntry.CONTENT_URI, null, null, null, null
        )
        assertEquals("Error: Records not deleted from Location table during delete", 0, cursor.count)
        cursor.close()
    }

    fun deleteAllRecords() {
        deleteAllRecordsFromProvider()
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        deleteAllRecords()
    }

    fun testProviderRegistry() {
        val pm = mContext.packageManager
        val componentName = ComponentName(mContext.packageName, WeatherProvider::class.java.name)
        try {
            val providerInfo = pm.getProviderInfo(componentName, 0)
            assertEquals(
                "Error: WeatherProvider registered with authority: ${providerInfo.authority} " +
                        "instead of authority: ${WeatherContract.CONTENT_AUTHORITY}",
                providerInfo.authority,
                WeatherContract.CONTENT_AUTHORITY
            )
        } catch (e: PackageManager.NameNotFoundException) {
            assertTrue("Error: WeatherProvider not registered at ${mContext.packageName}", false)
        }
    }

    fun testGetType() {
        var type = mContext.contentResolver.getType(WeatherEntry.CONTENT_URI)
        assertEquals(
            "Error: the WeatherEntry CONTENT_URI should return WeatherEntry.CONTENT_TYPE",
            WeatherEntry.CONTENT_TYPE,
            type
        )

        val testLocation = "94074"
        type = mContext.contentResolver.getType(WeatherEntry.buildWeatherLocation(testLocation))
        assertEquals(
            "Error: the WeatherEntry CONTENT_URI with location should return WeatherEntry.CONTENT_TYPE",
            WeatherEntry.CONTENT_TYPE,
            type
        )

        val testDate = 1419120000L
        type = mContext.contentResolver.getType(
            WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate)
        )
        assertEquals(
            "Error: the WeatherEntry CONTENT_URI with location and date should return WeatherEntry.CONTENT_ITEM_TYPE",
            WeatherEntry.CONTENT_ITEM_TYPE,
            type
        )

        type = mContext.contentResolver.getType(LocationEntry.CONTENT_URI)
        assertEquals(
            "Error: the LocationEntry CONTENT_URI should return LocationEntry.CONTENT_TYPE",
            LocationEntry.CONTENT_TYPE,
            type
        )
    }

    fun testBasicWeatherQuery() {
        val dbHelper = WeatherDbHelper(mContext)
        val db = dbHelper.writableDatabase

        val locationRowId = TestUtilities.insertNorthPoleLocationValues(mContext)
        val weatherValues = TestUtilities.createWeatherValues(locationRowId)

        val weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues)
        assertTrue("Unable to Insert WeatherEntry into the Database", weatherRowId != -1L)
        db.close()

        val weatherCursor = mContext.contentResolver.query(
            WeatherEntry.CONTENT_URI, null, null, null, null
        )
        TestUtilities.validateCursor("testBasicWeatherQuery", weatherCursor, weatherValues)
    }

    fun testBasicLocationQueries() {
        val dbHelper = WeatherDbHelper(mContext)
        val db = dbHelper.writableDatabase

        val testValues = TestUtilities.createNorthPoleLocationValues()
        val locationRowId = TestUtilities.insertNorthPoleLocationValues(mContext)

        val locationCursor = mContext.contentResolver.query(
            LocationEntry.CONTENT_URI, null, null, null, null
        )
        TestUtilities.validateCursor(
            "testBasicLocationQueries, location query",
            locationCursor,
            testValues
        )

        if (Build.VERSION.SDK_INT >= 19) {
            assertEquals(
                "Error: Location Query did not properly set NotificationUri",
                locationCursor.notificationUri,
                LocationEntry.CONTENT_URI
            )
        }
    }

    fun testUpdateLocation() {
        val values = TestUtilities.createNorthPoleLocationValues()
        val locationUri = mContext.contentResolver.insert(LocationEntry.CONTENT_URI, values)
        val locationRowId = ContentUris.parseId(locationUri)

        assertTrue(locationRowId != -1L)
        Log.d(LOG_TAG, "New row id: $locationRowId")

        val updatedValues = ContentValues(values)
        updatedValues.put(LocationEntry._ID, locationRowId)
        updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village")

        val locationCursor = mContext.contentResolver.query(
            LocationEntry.CONTENT_URI, null, null, null, null
        )

        val tco = TestUtilities.getTestContentObserver()
        locationCursor.registerContentObserver(tco)

        val count = mContext.contentResolver.update(
            LocationEntry.CONTENT_URI,
            updatedValues,
            "${LocationEntry._ID}= ?",
            arrayOf(locationRowId.toString())
        )
        assertEquals(count, 1)

        tco.waitForNotificationOrFail()

        locationCursor.unregisterContentObserver(tco)
        locationCursor.close()

        val cursor = mContext.contentResolver.query(
            LocationEntry.CONTENT_URI,
            null,
            "${LocationEntry._ID} = $locationRowId",
            null,
            null
        )
        TestUtilities.validateCursor(
            "testUpdateLocation.  Error validating location entry update.",
            cursor,
            updatedValues
        )
        cursor.close()
    }

    fun testInsertReadProvider() {
        val testValues = TestUtilities.createNorthPoleLocationValues()

        var tco = TestUtilities.getTestContentObserver()
        mContext.contentResolver.registerContentObserver(LocationEntry.CONTENT_URI, true, tco)
        val locationUri = mContext.contentResolver.insert(LocationEntry.CONTENT_URI, testValues)

        tco.waitForNotificationOrFail()
        mContext.contentResolver.unregisterContentObserver(tco)

        val locationRowId = ContentUris.parseId(locationUri)
        assertTrue(locationRowId != -1L)

        var cursor = mContext.contentResolver.query(
            LocationEntry.CONTENT_URI, null, null, null, null
        )
        TestUtilities.validateCursor(
            "testInsertReadProvider. Error validating LocationEntry.",
            cursor,
            testValues
        )

        val weatherValues = TestUtilities.createWeatherValues(locationRowId)
        tco = TestUtilities.getTestContentObserver()

        mContext.contentResolver.registerContentObserver(WeatherEntry.CONTENT_URI, true, tco)

        val weatherInsertUri = mContext.contentResolver.insert(WeatherEntry.CONTENT_URI, weatherValues)
        assertTrue(weatherInsertUri != null)

        tco.waitForNotificationOrFail()
        mContext.contentResolver.unregisterContentObserver(tco)

        var weatherCursor = mContext.contentResolver.query(
            WeatherEntry.CONTENT_URI, null, null, null, null
        )
        TestUtilities.validateCursor(
            "testInsertReadProvider. Error validating WeatherEntry insert.",
            weatherCursor,
            weatherValues
        )

        weatherValues.putAll(testValues)

        weatherCursor = mContext.contentResolver.query(
            WeatherEntry.buildWeatherLocation(TestUtilities.TEST_LOCATION),
            null, null, null, null
        )
        TestUtilities.validateCursor(
            "testInsertReadProvider.  Error validating joined Weather and Location Data.",
            weatherCursor,
            weatherValues
        )

        weatherCursor = mContext.contentResolver.query(
            WeatherEntry.buildWeatherLocationWithStartDate(
                TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE
            ),
            null, null, null, null
        )
        TestUtilities.validateCursor(
            "testInsertReadProvider.  Error validating joined Weather and Location Data with start date.",
            weatherCursor,
            weatherValues
        )

        weatherCursor = mContext.contentResolver.query(
            WeatherEntry.buildWeatherLocationWithDate(
                TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE
            ),
            null, null, null, null
        )
        TestUtilities.validateCursor(
            "testInsertReadProvider.  Error validating joined Weather and Location data for a specific date.",
            weatherCursor,
            weatherValues
        )
    }

    fun testDeleteRecords() {
        testInsertReadProvider()

        val locationObserver = TestUtilities.getTestContentObserver()
        mContext.contentResolver.registerContentObserver(LocationEntry.CONTENT_URI, true, locationObserver)

        val weatherObserver = TestUtilities.getTestContentObserver()
        mContext.contentResolver.registerContentObserver(WeatherEntry.CONTENT_URI, true, weatherObserver)

        deleteAllRecordsFromProvider()

        locationObserver.waitForNotificationOrFail()
        weatherObserver.waitForNotificationOrFail()

        mContext.contentResolver.unregisterContentObserver(locationObserver)
        mContext.contentResolver.unregisterContentObserver(weatherObserver)
    }

    fun testBulkInsert() {
        val testValues = TestUtilities.createNorthPoleLocationValues()
        val locationUri = mContext.contentResolver.insert(LocationEntry.CONTENT_URI, testValues)
        val locationRowId = ContentUris.parseId(locationUri)
        assertTrue(locationRowId != -1L)

        val cursor = mContext.contentResolver.query(
            LocationEntry.CONTENT_URI, null, null, null, null
        )
        TestUtilities.validateCursor("testBulkInsert. Error validating LocationEntry.", cursor, testValues)

        val bulkInsertContentValues = createBulkInsertWeatherValues(locationRowId)

        val weatherObserver = TestUtilities.getTestContentObserver()
        mContext.contentResolver.registerContentObserver(WeatherEntry.CONTENT_URI, true, weatherObserver)

        val insertCount = mContext.contentResolver.bulkInsert(
            WeatherEntry.CONTENT_URI, bulkInsertContentValues
        )

        weatherObserver.waitForNotificationOrFail()
        mContext.contentResolver.unregisterContentObserver(weatherObserver)

        assertEquals(insertCount, BULK_INSERT_RECORDS_TO_INSERT)

        val weatherCursor = mContext.contentResolver.query(
            WeatherEntry.CONTENT_URI,
            null, null, null,
            "${WeatherEntry.COLUMN_DATE} ASC"
        )

        assertEquals(weatherCursor.count, BULK_INSERT_RECORDS_TO_INSERT)

        weatherCursor.moveToFirst()
        for (i in 0 until BULK_INSERT_RECORDS_TO_INSERT) {
            TestUtilities.validateCurrentRecord(
                "testBulkInsert.  Error validating WeatherEntry $i",
                weatherCursor,
                bulkInsertContentValues[i]
            )
            weatherCursor.moveToNext()
        }
        weatherCursor.close()
    }
}

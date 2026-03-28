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

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.test.AndroidTestCase
import java.util.HashSet

class TestDb : AndroidTestCase() {

    companion object {
        val LOG_TAG: String = TestDb::class.java.simpleName
    }

    private fun deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME)
    }

    public override fun setUp() {
        deleteTheDatabase()
    }

    @Throws(Throwable::class)
    fun testCreateDb() {
        val tableNameHashSet = HashSet<String>()
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME)
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME)

        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME)
        val db = WeatherDbHelper(this.mContext).writableDatabase
        assertEquals(true, db.isOpen)

        var c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        assertTrue(
            "Error: This means that the database has not been created correctly",
            c.moveToFirst()
        )

        do {
            tableNameHashSet.remove(c.getString(0))
        } while (c.moveToNext())

        assertTrue(
            "Error: Your database was created without both the location entry and weather entry tables",
            tableNameHashSet.isEmpty()
        )

        c = db.rawQuery(
            "PRAGMA table_info(${WeatherContract.LocationEntry.TABLE_NAME})", null
        )
        assertTrue(
            "Error: This means that we were unable to query the database for table information.",
            c.moveToFirst()
        )

        val locationColumnHashSet = HashSet<String>()
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID)
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME)
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT)
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG)
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING)

        val columnNameIndex = c.getColumnIndex("name")
        do {
            locationColumnHashSet.remove(c.getString(columnNameIndex))
        } while (c.moveToNext())

        assertTrue(
            "Error: The database doesn't contain all of the required location entry columns",
            locationColumnHashSet.isEmpty()
        )
        db.close()
    }

    fun testLocationTable() {
        insertLocation()
    }

    fun testWeatherTable() {
        val locationRowId = insertLocation()
        assertFalse("Error: Location Not Inserted Correctly", locationRowId == -1L)

        val dbHelper = WeatherDbHelper(mContext)
        val db = dbHelper.writableDatabase

        val weatherValues = TestUtilities.createWeatherValues(locationRowId)
        val weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, weatherValues)
        assertTrue(weatherRowId != -1L)

        val weatherCursor = db.query(
            WeatherContract.WeatherEntry.TABLE_NAME,
            null, null, null, null, null, null
        )

        assertTrue("Error: No Records returned from location query", weatherCursor.moveToFirst())

        TestUtilities.validateCurrentRecord(
            "testInsertReadDb weatherEntry failed to validate",
            weatherCursor,
            weatherValues
        )

        assertFalse("Error: More than one record returned from weather query", weatherCursor.moveToNext())

        weatherCursor.close()
        dbHelper.close()
    }

    fun insertLocation(): Long {
        val dbHelper = WeatherDbHelper(mContext)
        val db = dbHelper.writableDatabase

        val testValues = TestUtilities.createNorthPoleLocationValues()
        val locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, testValues)
        assertTrue(locationRowId != -1L)

        val cursor = db.query(
            WeatherContract.LocationEntry.TABLE_NAME,
            null, null, null, null, null, null
        )

        assertTrue("Error: No Records returned from location query", cursor.moveToFirst())

        TestUtilities.validateCurrentRecord(
            "Error: Location Query Validation Failed",
            cursor,
            testValues
        )

        assertFalse("Error: More than one record returned from location query", cursor.moveToNext())

        cursor.close()
        db.close()
        return locationRowId
    }
}

package com.example.android.sunshine.app.data

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.test.AndroidTestCase
import com.example.android.sunshine.app.utils.PollingCheck
import java.util.Map

class TestUtilities : AndroidTestCase() {

    companion object {
        const val TEST_LOCATION = "99705"
        const val TEST_DATE = 1419033600L  // December 20th, 2014

        fun validateCursor(error: String, valueCursor: Cursor, expectedValues: ContentValues) {
            assertTrue("Empty cursor returned. $error", valueCursor.moveToFirst())
            validateCurrentRecord(error, valueCursor, expectedValues)
            valueCursor.close()
        }

        fun validateCurrentRecord(error: String, valueCursor: Cursor, expectedValues: ContentValues) {
            val valueSet = expectedValues.valueSet()
            for (entry in valueSet) {
                val columnName = entry.key
                val idx = valueCursor.getColumnIndex(columnName)
                assertFalse("Column '$columnName' not found. $error", idx == -1)
                val expectedValue = entry.value.toString()
                assertEquals(
                    "Value '${entry.value}' did not match the expected value '$expectedValue'. $error",
                    expectedValue,
                    valueCursor.getString(idx)
                )
            }
        }

        fun createWeatherValues(locationRowId: Long): ContentValues {
            val weatherValues = ContentValues()
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationRowId)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, TEST_DATE)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "Asteroids")
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5)
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321)
            return weatherValues
        }

        fun createNorthPoleLocationValues(): ContentValues {
            val testValues = ContentValues()
            testValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION)
            testValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "North Pole")
            testValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, 64.7488)
            testValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, -147.353)
            return testValues
        }

        fun insertNorthPoleLocationValues(context: Context): Long {
            val dbHelper = WeatherDbHelper(context)
            val db = dbHelper.writableDatabase
            val testValues = createNorthPoleLocationValues()

            val locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, testValues)
            assertTrue("Error: Failure to insert North Pole Location Values", locationRowId != -1L)
            return locationRowId
        }

        fun getTestContentObserver(): TestContentObserver =
            TestContentObserver.getTestContentObserver()
    }

    class TestContentObserver private constructor(ht: HandlerThread) :
        ContentObserver(Handler(ht.looper)) {

        val mHT: HandlerThread = ht
        var mContentChanged = false

        companion object {
            fun getTestContentObserver(): TestContentObserver {
                val ht = HandlerThread("ContentObserverThread")
                ht.start()
                return TestContentObserver(ht)
            }
        }

        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            mContentChanged = true
        }

        fun waitForNotificationOrFail() {
            object : PollingCheck(5000) {
                override fun check(): Boolean = mContentChanged
            }.run()
            mHT.quit()
        }
    }
}

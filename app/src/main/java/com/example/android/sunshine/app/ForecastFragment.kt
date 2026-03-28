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

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import com.example.android.sunshine.app.data.WeatherContract
import com.example.android.sunshine.app.sync.SunshineSyncAdapter

/**
 * Encapsulates fetching the forecast and displaying it as a [ListView] layout.
 */
class ForecastFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        val LOG_TAG: String = ForecastFragment::class.java.simpleName
        private const val SELECTED_KEY = "selected_position"
        private const val FORECAST_LOADER = 0

        val FORECAST_COLUMNS = arrayOf(
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
        )

        const val COL_WEATHER_ID = 0
        const val COL_WEATHER_DATE = 1
        const val COL_WEATHER_DESC = 2
        const val COL_WEATHER_MAX_TEMP = 3
        const val COL_WEATHER_MIN_TEMP = 4
        const val COL_LOCATION_SETTING = 5
        const val COL_WEATHER_CONDITION_ID = 6
        const val COL_COORD_LAT = 7
        const val COL_COORD_LONG = 8
    }

    /**
     * A callback interface that all activities containing this fragment must implement.
     */
    interface Callback {
        fun onItemSelected(dateUri: Uri)
    }

    private lateinit var mForecastAdapter: ForecastAdapter
    private lateinit var mListView: ListView
    private var mPosition = ListView.INVALID_POSITION
    private var mUseTodayLayout = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.forecastfragment, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_map) {
            openPreferredLocationInMap()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mForecastAdapter = ForecastAdapter(activity, null, 0)

        val rootView = inflater.inflate(R.layout.fragment_main, container, false)

        mListView = rootView.findViewById(R.id.listview_forecast) as ListView
        mListView.adapter = mForecastAdapter
        mListView.setOnItemClickListener { adapterView, _, position, _ ->
            val cursor = adapterView.getItemAtPosition(position) as? Cursor
            if (cursor != null) {
                val locationSetting = Utility.getPreferredLocation(activity)
                (activity as Callback).onItemSelected(
                    WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                        locationSetting, cursor.getLong(COL_WEATHER_DATE)
                    )
                )
            }
            mPosition = position
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY)
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        loaderManager.initLoader(FORECAST_LOADER, null, this)
        super.onActivityCreated(savedInstanceState)
    }

    fun onLocationChanged() {
        updateWeather()
        loaderManager.restartLoader(FORECAST_LOADER, null, this)
    }

    private fun updateWeather() {
        SunshineSyncAdapter.syncImmediately(activity)
    }

    private fun openPreferredLocationInMap() {
        val c = mForecastAdapter.cursor
        if (c != null) {
            c.moveToPosition(0)
            val posLat = c.getString(COL_COORD_LAT)
            val posLong = c.getString(COL_COORD_LONG)
            val geoLocation = Uri.parse("geo:$posLat,$posLong")

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = geoLocation

            if (intent.resolveActivity(activity.packageManager) != null) {
                startActivity(intent)
            } else {
                Log.d(LOG_TAG, "Couldn't call $geoLocation, no receiving apps installed!")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        val sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC"
        val locationSetting = Utility.getPreferredLocation(activity)
        val weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
            locationSetting, System.currentTimeMillis()
        )
        return CursorLoader(activity, weatherForLocationUri, FORECAST_COLUMNS, null, null, sortOrder)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        mForecastAdapter.swapCursor(data)
        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mForecastAdapter.swapCursor(null)
    }

    fun setUseTodayLayout(useTodayLayout: Boolean) {
        mUseTodayLayout = useTodayLayout
        if (::mForecastAdapter.isInitialized) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout)
        }
    }
}

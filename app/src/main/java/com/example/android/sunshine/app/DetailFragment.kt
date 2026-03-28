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
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.ShareActionProvider
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.android.sunshine.app.data.WeatherContract
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry

/**
 * A placeholder fragment containing a simple view.
 */
class DetailFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    companion object {
        private val LOG_TAG: String = DetailFragment::class.java.simpleName
        const val DETAIL_URI = "URI"
        private const val FORECAST_SHARE_HASHTAG = " #SunshineApp"
        private const val DETAIL_LOADER = 0

        private val DETAIL_COLUMNS = arrayOf(
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
        )

        const val COL_WEATHER_ID = 0
        const val COL_WEATHER_DATE = 1
        const val COL_WEATHER_DESC = 2
        const val COL_WEATHER_MAX_TEMP = 3
        const val COL_WEATHER_MIN_TEMP = 4
        const val COL_WEATHER_HUMIDITY = 5
        const val COL_WEATHER_PRESSURE = 6
        const val COL_WEATHER_WIND_SPEED = 7
        const val COL_WEATHER_DEGREES = 8
        const val COL_WEATHER_CONDITION_ID = 9
    }

    private var mShareActionProvider: ShareActionProvider? = null
    private var mForecast: String? = null
    private var mUri: Uri? = null

    private lateinit var mIconView: ImageView
    private lateinit var mFriendlyDateView: TextView
    private lateinit var mDateView: TextView
    private lateinit var mDescriptionView: TextView
    private lateinit var mHighTempView: TextView
    private lateinit var mLowTempView: TextView
    private lateinit var mHumidityView: TextView
    private lateinit var mWindView: TextView
    private lateinit var mPressureView: TextView

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let { mUri = it.getParcelable(DETAIL_URI) }

        val rootView = inflater.inflate(R.layout.fragment_detail, container, false)
        mIconView = rootView.findViewById(R.id.detail_icon) as ImageView
        mDateView = rootView.findViewById(R.id.detail_date_textview) as TextView
        mFriendlyDateView = rootView.findViewById(R.id.detail_day_textview) as TextView
        mDescriptionView = rootView.findViewById(R.id.detail_forecast_textview) as TextView
        mHighTempView = rootView.findViewById(R.id.detail_high_textview) as TextView
        mLowTempView = rootView.findViewById(R.id.detail_low_textview) as TextView
        mHumidityView = rootView.findViewById(R.id.detail_humidity_textview) as TextView
        mWindView = rootView.findViewById(R.id.detail_wind_textview) as TextView
        mPressureView = rootView.findViewById(R.id.detail_pressure_textview) as TextView
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.detailfragment, menu)

        val menuItem = menu.findItem(R.id.action_share)
        mShareActionProvider = MenuItemCompat.getActionProvider(menuItem) as ShareActionProvider

        mForecast?.let { mShareActionProvider?.setShareIntent(createShareForecastIntent()) }
    }

    private fun createShareForecastIntent(): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG)
        return shareIntent
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        loaderManager.initLoader(DETAIL_LOADER, null, this)
        super.onActivityCreated(savedInstanceState)
    }

    fun onLocationChanged(newLocation: String) {
        val uri = mUri ?: return
        val date = WeatherContract.WeatherEntry.getDateFromUri(uri)
        mUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date)
        loaderManager.restartLoader(DETAIL_LOADER, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor>? {
        val uri = mUri ?: return null
        return CursorLoader(activity, uri, DETAIL_COLUMNS, null, null, null)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        if (data != null && data.moveToFirst()) {
            val weatherId = data.getInt(COL_WEATHER_CONDITION_ID)
            mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId))

            val date = data.getLong(COL_WEATHER_DATE)
            mFriendlyDateView.text = Utility.getDayName(activity, date)
            mDateView.text = Utility.getFormattedMonthDay(activity, date)

            val description = data.getString(COL_WEATHER_DESC)
            mDescriptionView.text = description
            mIconView.contentDescription = description

            val high = data.getDouble(COL_WEATHER_MAX_TEMP)
            mHighTempView.text = Utility.formatTemperature(activity, high)

            val low = data.getDouble(COL_WEATHER_MIN_TEMP)
            mLowTempView.text = Utility.formatTemperature(activity, low)

            val humidity = data.getFloat(COL_WEATHER_HUMIDITY)
            mHumidityView.text = activity.getString(R.string.format_humidity, humidity)

            val windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED)
            val windDir = data.getFloat(COL_WEATHER_DEGREES)
            mWindView.text = Utility.getFormattedWind(activity, windSpeed, windDir)

            val pressure = data.getFloat(COL_WEATHER_PRESSURE)
            mPressureView.text = activity.getString(R.string.format_pressure, pressure)

            val dateText = Utility.getFormattedMonthDay(activity, date)
            mForecast = "$dateText - $description - $high/$low"

            mShareActionProvider?.setShareIntent(createShareForecastIntent())
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {}
}

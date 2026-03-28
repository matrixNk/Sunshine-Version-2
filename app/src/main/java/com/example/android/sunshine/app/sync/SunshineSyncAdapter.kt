package com.example.android.sunshine.app.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SyncRequest
import android.content.SyncResult
import android.content.res.Resources
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.text.format.Time
import android.util.Log
import com.example.android.sunshine.app.BuildConfig
import com.example.android.sunshine.app.MainActivity
import com.example.android.sunshine.app.R
import com.example.android.sunshine.app.Utility
import com.example.android.sunshine.app.data.WeatherContract
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Vector

class SunshineSyncAdapter(context: Context, autoInitialize: Boolean) :
    AbstractThreadedSyncAdapter(context, autoInitialize) {

    val LOG_TAG: String = SunshineSyncAdapter::class.java.simpleName

    companion object {
        const val SYNC_INTERVAL = 60 * 180
        const val SYNC_FLEXTIME = SYNC_INTERVAL / 3
        private const val DAY_IN_MILLIS = 1000L * 60 * 60 * 24
        private const val WEATHER_NOTIFICATION_ID = 3004

        private val NOTIFY_WEATHER_PROJECTION = arrayOf(
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
        )

        private const val INDEX_WEATHER_ID = 0
        private const val INDEX_MAX_TEMP = 1
        private const val INDEX_MIN_TEMP = 2
        private const val INDEX_SHORT_DESC = 3

        fun configurePeriodicSync(context: Context, syncInterval: Int, flexTime: Int) {
            val account = getSyncAccount(context)
            val authority = context.getString(R.string.content_authority)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val request = SyncRequest.Builder()
                    .syncPeriodic(syncInterval.toLong(), flexTime.toLong())
                    .setSyncAdapter(account, authority)
                    .setExtras(Bundle())
                    .build()
                ContentResolver.requestSync(request)
            } else {
                ContentResolver.addPeriodicSync(account, authority, Bundle(), syncInterval.toLong())
            }
        }

        fun syncImmediately(context: Context) {
            val bundle = Bundle()
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            ContentResolver.requestSync(
                getSyncAccount(context),
                context.getString(R.string.content_authority),
                bundle
            )
        }

        fun getSyncAccount(context: Context): Account {
            val accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
            val newAccount = Account(
                context.getString(R.string.app_name),
                context.getString(R.string.sync_account_type)
            )

            if (null == accountManager.getPassword(newAccount)) {
                if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                    return newAccount
                }
                onAccountCreated(newAccount, context)
            }
            return newAccount
        }

        private fun onAccountCreated(newAccount: Account, context: Context) {
            configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME)
            ContentResolver.setSyncAutomatically(
                newAccount,
                context.getString(R.string.content_authority),
                true
            )
            syncImmediately(context)
        }

        fun initializeSyncAdapter(context: Context) {
            getSyncAccount(context)
        }
    }

    override fun onPerformSync(
        account: Account,
        extras: Bundle,
        authority: String,
        provider: ContentProviderClient,
        syncResult: SyncResult
    ) {
        Log.d(LOG_TAG, "Starting sync")
        val locationQuery = Utility.getPreferredLocation(context)

        var urlConnection: HttpURLConnection? = null
        var reader: BufferedReader? = null

        val format = "json"
        val units = "metric"
        val numDays = 14

        try {
            val forecastBaseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?"
            val builtUri = Uri.parse(forecastBaseUrl).buildUpon()
                .appendQueryParameter("q", locationQuery)
                .appendQueryParameter("mode", format)
                .appendQueryParameter("units", units)
                .appendQueryParameter("cnt", numDays.toString())
                .appendQueryParameter("APPID", BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                .build()

            val url = URL(builtUri.toString())
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connect()

            val inputStream = urlConnection.inputStream ?: return
            val buffer = StringBuffer()
            reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line).append("\n")
            }

            if (buffer.isEmpty()) return

            getWeatherDataFromJson(buffer.toString(), locationQuery)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Error ", e)
        } catch (e: JSONException) {
            Log.e(LOG_TAG, e.message, e)
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
            try {
                reader?.close()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error closing stream", e)
            }
        }
    }

    @Throws(JSONException::class)
    private fun getWeatherDataFromJson(forecastJsonStr: String, locationSetting: String) {
        val owmCity = "city"
        val owmCityName = "name"
        val owmCoord = "coord"
        val owmLatitude = "lat"
        val owmLongitude = "lon"
        val owmList = "list"
        val owmPressure = "pressure"
        val owmHumidity = "humidity"
        val owmWindspeed = "speed"
        val owmWindDirection = "deg"
        val owmTemperature = "temp"
        val owmMax = "max"
        val owmMin = "min"
        val owmWeather = "weather"
        val owmDescription = "main"
        val owmWeatherId = "id"

        try {
            val forecastJson = JSONObject(forecastJsonStr)
            val weatherArray = forecastJson.getJSONArray(owmList)

            val cityJson = forecastJson.getJSONObject(owmCity)
            val cityName = cityJson.getString(owmCityName)
            val cityCoord = cityJson.getJSONObject(owmCoord)
            val cityLatitude = cityCoord.getDouble(owmLatitude)
            val cityLongitude = cityCoord.getDouble(owmLongitude)

            val locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude)

            val cVVector = Vector<ContentValues>(weatherArray.length())

            val dayTime = Time()
            dayTime.setToNow()
            val julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff)
            val utcDayTime = Time()

            for (i in 0 until weatherArray.length()) {
                val dayForecast = weatherArray.getJSONObject(i)
                val dateTime = utcDayTime.setJulianDay(julianStartDay + i)

                val pressure = dayForecast.getDouble(owmPressure)
                val humidity = dayForecast.getInt(owmHumidity)
                val windSpeed = dayForecast.getDouble(owmWindspeed)
                val windDirection = dayForecast.getDouble(owmWindDirection)

                val weatherObject = dayForecast.getJSONArray(owmWeather).getJSONObject(0)
                val description = weatherObject.getString(owmDescription)
                val weatherId = weatherObject.getInt(owmWeatherId)

                val temperatureObject = dayForecast.getJSONObject(owmTemperature)
                val high = temperatureObject.getDouble(owmMax)
                val low = temperatureObject.getDouble(owmMin)

                val weatherValues = ContentValues().apply {
                    put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationId)
                    put(WeatherContract.WeatherEntry.COLUMN_DATE, dateTime)
                    put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity)
                    put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure)
                    put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed)
                    put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection)
                    put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high)
                    put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low)
                    put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description)
                    put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weatherId)
                }
                cVVector.add(weatherValues)
            }

            if (cVVector.size > 0) {
                val cvArray = cVVector.toTypedArray()
                context.contentResolver.bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray)

                context.contentResolver.delete(
                    WeatherContract.WeatherEntry.CONTENT_URI,
                    "${WeatherContract.WeatherEntry.COLUMN_DATE} <= ?",
                    arrayOf(utcDayTime.setJulianDay(julianStartDay - 1).toString())
                )

                notifyWeather()
            }

            Log.d(LOG_TAG, "Sync Complete. ${cVVector.size} Inserted")
        } catch (e: JSONException) {
            Log.e(LOG_TAG, e.message, e)
            e.printStackTrace()
        }
    }

    private fun notifyWeather() {
        val context = context
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key)
        val displayNotifications = prefs.getBoolean(
            displayNotificationsKey,
            context.getString(R.string.pref_enable_notifications_default).toBoolean()
        )

        if (displayNotifications) {
            val lastNotificationKey = context.getString(R.string.pref_last_notification)
            val lastSync = prefs.getLong(lastNotificationKey, 0)

            if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
                val locationQuery = Utility.getPreferredLocation(context)
                val weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    locationQuery, System.currentTimeMillis()
                )

                val cursor = context.contentResolver.query(
                    weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null
                )

                if (cursor?.moveToFirst() == true) {
                    val weatherId = cursor.getInt(INDEX_WEATHER_ID)
                    val high = cursor.getDouble(INDEX_MAX_TEMP)
                    val low = cursor.getDouble(INDEX_MIN_TEMP)
                    val desc = cursor.getString(INDEX_SHORT_DESC)

                    val iconId = Utility.getIconResourceForWeatherCondition(weatherId)
                    val resources = context.resources
                    val largeIcon = BitmapFactory.decodeResource(
                        resources, Utility.getArtResourceForWeatherCondition(weatherId)
                    )
                    val title = context.getString(R.string.app_name)
                    val contentText = String.format(
                        context.getString(R.string.format_notification),
                        desc,
                        Utility.formatTemperature(context, high),
                        Utility.formatTemperature(context, low)
                    )

                    val mBuilder = NotificationCompat.Builder(context)
                        .setColor(resources.getColor(R.color.sunshine_light_blue))
                        .setSmallIcon(iconId)
                        .setLargeIcon(largeIcon)
                        .setContentTitle(title)
                        .setContentText(contentText)

                    val resultIntent = Intent(context, MainActivity::class.java)
                    val stackBuilder = TaskStackBuilder.create(context)
                    stackBuilder.addNextIntent(resultIntent)
                    val resultPendingIntent = stackBuilder.getPendingIntent(
                        0, PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    mBuilder.setContentIntent(resultPendingIntent)

                    val mNotificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build())

                    prefs.edit().putLong(lastNotificationKey, System.currentTimeMillis()).commit()
                }
                cursor?.close()
            }
        }
    }

    fun addLocation(locationSetting: String, cityName: String, lat: Double, lon: Double): Long {
        val locationCursor = context.contentResolver.query(
            WeatherContract.LocationEntry.CONTENT_URI,
            arrayOf(WeatherContract.LocationEntry._ID),
            "${WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING} = ?",
            arrayOf(locationSetting),
            null
        )

        val locationId: Long
        if (locationCursor != null && locationCursor.moveToFirst()) {
            val locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID)
            locationId = locationCursor.getLong(locationIdIndex)
        } else {
            val locationValues = ContentValues().apply {
                put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName)
                put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting)
                put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat)
                put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon)
            }
            val insertedUri = context.contentResolver.insert(
                WeatherContract.LocationEntry.CONTENT_URI, locationValues
            )
            locationId = ContentUris.parseId(insertedUri)
        }

        locationCursor?.close()
        return locationId
    }
}

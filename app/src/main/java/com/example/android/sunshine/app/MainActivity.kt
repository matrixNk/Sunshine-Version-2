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
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.ActionBarActivity
import android.view.Menu
import android.view.MenuItem
import com.example.android.sunshine.app.sync.SunshineSyncAdapter

class MainActivity : ActionBarActivity(), ForecastFragment.Callback {

    private val LOG_TAG = MainActivity::class.java.simpleName
    private val DETAILFRAGMENT_TAG = "DFTAG"

    private var mTwoPane = false
    private var mLocation: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocation = Utility.getPreferredLocation(this)

        setContentView(R.layout.activity_main)
        if (findViewById(R.id.weather_detail_container) != null) {
            mTwoPane = true
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.weather_detail_container, DetailFragment(), DETAILFRAGMENT_TAG)
                    .commit()
            }
        } else {
            mTwoPane = false
            supportActionBar.elevation = 0f
        }

        val forecastFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_forecast) as ForecastFragment
        forecastFragment.setUseTodayLayout(!mTwoPane)

        SunshineSyncAdapter.initializeSyncAdapter(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        val location = Utility.getPreferredLocation(this)
        if (location != mLocation) {
            val ff = supportFragmentManager.findFragmentById(R.id.fragment_forecast) as? ForecastFragment
            ff?.onLocationChanged()

            val df = supportFragmentManager.findFragmentByTag(DETAILFRAGMENT_TAG) as? DetailFragment
            df?.onLocationChanged(location)

            mLocation = location
        }
    }

    override fun onItemSelected(contentUri: Uri) {
        if (mTwoPane) {
            val args = Bundle()
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri)

            val fragment = DetailFragment()
            fragment.arguments = args

            supportFragmentManager.beginTransaction()
                .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                .commit()
        } else {
            val intent = Intent(this, DetailActivity::class.java).setData(contentUri)
            startActivity(intent)
        }
    }
}

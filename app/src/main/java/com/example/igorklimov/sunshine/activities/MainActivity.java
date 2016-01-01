/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.igorklimov.sunshine.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.igorklimov.sunshine.fragments.DetailFragment;
import com.example.igorklimov.sunshine.fragments.ForecastFragment;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.gcm.RegistrationIntentService;
import com.example.igorklimov.sunshine.helpers.ForecastAdapter;
import com.example.igorklimov.sunshine.helpers.Utility;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {

    private String mLocation;
    private Uri mUri;
    public boolean isTablet;
    public static boolean sIconStyleChanged;

    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        mLocation = Utility.getPreferredLocation(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        isTablet = findViewById(R.id.weather_detail_container) != null;
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(getString(R.string.pref_is_tablet), isTablet)
                .apply();
        SunshineSyncAdapter.initializeSyncAdapter(this);

        if (checkPlayServices()) {
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            boolean sentToken = sharedPreferences.getBoolean(SENT_TOKEN_TO_SERVER, false);
            if (!sentToken) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
        Intent i = getIntent();
        if (i != null && i.getData() != null) {
            mUri = i.getData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utility.setToday();
        String location = Utility.getPreferredLocation(this);
        // update the mLocation in our second pane using the fragment manager
        ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        if (location != null && !location.equals(this.mLocation)) {
            if (null != ff) ff.onLocationOrUnitSystemChanged();
            DetailFragment df = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != df) df.onLocationChanged(location);
            this.mLocation = location;
        } else if (sIconStyleChanged) {
            if (null != ff) ff.onLocationOrUnitSystemChanged();
            sIconStyleChanged = false;
        }
    }

    /**
     * This method opens detail view whether it's a handset - {@link DetailActivity} or on a tablet -
     * {@link DetailFragment} in main layout.
     * <p>mUri ternary check in case user pressed on item from a widget</p>
     */
    @Override
    public void onItemSelected(@NotNull Uri dateUri, ForecastAdapter.ViewHolder holder) {
        if (!isTablet) {
            Intent intent = new Intent(this, DetailActivity.class).setData(dateUri);
            ActivityOptionsCompat activityOptions =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                            new Pair<View, String>(holder.image, getString(R.string.detail_icon_transition_name)));
            ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
        } else {
            DetailFragment newFragment = new DetailFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("one", mUri == null ? dateUri : mUri);
            newFragment.setArguments(bundle);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.weather_detail_container, newFragment, DETAILFRAGMENT_TAG);
            transaction.addToBackStack(null);
            transaction.commit();
            mUri = null;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
//            Uncomment below code for real device
//            if (apiAvailability.isUserResolvableError(resultCode)) {
//                apiAvailability.getErrorDialog(this, resultCode,
//                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
//            } else {
//                Log.d("TAG", "This device is not supported.");
//                finish();
//            }
            return false;
        }
        return true;
    }
}

package com.example.igorklimov.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private String location;
    private static final String FORECASTFRAGMENT_TAG = "forecast_tag";

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

        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ForecastFragment(), FORECASTFRAGMENT_TAG)
                    .commit();
        }

        location = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.location_key), getString(R.string.location_default_value));

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!location.equals(Utility.getPreferredLocation(this))) {
            ForecastFragment fragment = (ForecastFragment) getSupportFragmentManager()
                    .findFragmentByTag(FORECASTFRAGMENT_TAG);
            fragment.onLocationChanged();
            fragment.getLoaderManager().restartLoader(0, null, fragment);
            location = Utility.getPreferredLocation(this);
        }

        Log.d("TAG", "onResume()");
        Utility.setToday();
    }

}

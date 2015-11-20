package com.example.igorklimov.sunshine.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.fragments.DetailFragment;
import com.example.igorklimov.sunshine.fragments.ForecastFragment;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.helpers.Utility;

import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.getDateFromUri;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback {

    private String location;
    public boolean mTwoPane;
    static boolean unitSystemChanged = false;
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private int position = 0;


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
        location = Utility.getPreferredLocation(this);
        ActionBar bar = getSupportActionBar();
        if(bar!= null) bar.setElevation(0f);

        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
//            if (savedInstanceState == null) {
//                DetailFragment fragment = new DetailFragment();
//                getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
//                        .commit();
//            }
        } else {
            mTwoPane = false;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Utility.setToday();
        String location = Utility.getPreferredLocation(this);
        // update the location in our second pane using the fragment manager
        if (location != null && !location.equals(this.location)) {
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (null != ff) {
                ff.onLocationOrUnitSystemChanged();
            }
            DetailFragment df = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != df) {
                df.onLocationChanged(location);
            }
            this.location = location;
        }
    }

    @Override
    public void onItemSelected(Uri dateUri) {

        if (!mTwoPane) {
            Intent intent = new Intent(this, DetailActivity.class).setData(dateUri);
            startActivity(intent);
        } else {
            DetailFragment newFragment = new DetailFragment();

            Bundle bundle = new Bundle();
            bundle.putParcelable("one",dateUri);
            newFragment.setArguments(bundle);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.weather_detail_container, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }

    }

}

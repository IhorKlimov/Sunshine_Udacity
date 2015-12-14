package com.example.igorklimov.sunshine.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.MainActivity;
import com.example.igorklimov.sunshine.data.WeatherContract.LocationEntry;
import com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry;
import com.example.igorklimov.sunshine.helpers.ForecastAdapter;
import com.example.igorklimov.sunshine.helpers.Utility;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int FORECAST_LOADER = 0;
    private ForecastAdapter forecastAdapter;
    ListView listView;
    MainActivity mainActivity;
    private int pos = 0;
    private Context context;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG,
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;
    private TextView notAvailable;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_location:
                Uri geoLocation = Uri.parse(getGeoLocation());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The CursorAdapter will take data from our cursor and populate the ListView.
        forecastAdapter = new ForecastAdapter(getActivity(), null, 0);
        if (savedInstanceState != null && savedInstanceState.containsKey("position"))
            pos = savedInstanceState.getInt("position");
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mainActivity = (MainActivity) getActivity();
        // Get a reference to the ListView, and attach this adapter to it.
        listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);
        notAvailable = (TextView) rootView.findViewById(R.id.not_available);
        listView.setEmptyView(notAvailable);
        updateWeather();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                pos = position;
                String locationSetting = Utility.getPreferredLocation(context);
                mainActivity.onItemSelected(WeatherEntry
                        .buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)));

            }
        });

        return rootView;
    }

    public void onLocationOrUnitSystemChanged() {
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
//        updateWeather();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("position", pos);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    private String getGeoLocation() {
        String msg = "";
        Cursor cursor = forecastAdapter.getCursor();
        if (cursor.moveToFirst()) {
            String latitude = cursor.getString(COL_COORD_LAT);
            String longitude = cursor.getString(COL_COORD_LONG);
            msg = String.format("geo:%s,%s", latitude, longitude);
        }
        return msg;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!cursor.moveToFirst()) setErrorMessage();

        forecastAdapter.swapCursor(cursor);
        if (mainActivity.isTablet) {
            listView.setItemChecked(pos, true);
            listView.smoothScrollToPosition(pos);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    listView.performItemClick(
                            listView.getAdapter().getView(pos, null, null),
                            pos,
                            listView.getAdapter().getItemId(pos));
                }
            }, 300);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        forecastAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setErrorMessage();
    }

    private void setErrorMessage() {
        ConnectivityManager systemService =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (systemService.getActiveNetworkInfo() == null) {
            notAvailable.setText(R.string.no_weather_available);
            return;
        }

        switch (Utility.getLocationStatusPreference(context)) {
            case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                notAvailable.setText(R.string.empty_forecast_list_server_down);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                notAvailable.setText(R.string.empty_forecast_list_server_error);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                notAvailable.setText(R.string.empty_forecast_list_invalid_location);
                break;
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri dateUri);

    }

}
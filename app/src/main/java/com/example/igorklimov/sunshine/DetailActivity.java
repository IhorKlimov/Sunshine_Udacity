package com.example.igorklimov.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry;

import org.w3c.dom.Text;

import java.text.ParseException;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();

        private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

        private ShareActionProvider mShareActionProvider;
        private String mForecast;

        private static final int DETAIL_LOADER = 0;

        private static final String[] FORECAST_COLUMNS = {
                WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
                WeatherEntry.COLUMN_DATE,
                WeatherEntry.COLUMN_SHORT_DESC,
                WeatherEntry.COLUMN_MAX_TEMP,
                WeatherEntry.COLUMN_MIN_TEMP,
                WeatherEntry.COLUMN_HUMIDITY,
                WeatherEntry.COLUMN_WIND_SPEED,
                WeatherEntry.COLUMN_PRESSURE,
                WeatherEntry.COLUMN_DEGREES
        };

        // these constants correspond to the projection defined above, and must change if the
        // projection changes
        private static final int COL_WEATHER_ID = 0;
        private static final int COL_WEATHER_DATE = 1;
        private static final int COL_WEATHER_DESC = 2;
        private static final int COL_WEATHER_MAX_TEMP = 3;
        private static final int COL_WEATHER_MIN_TEMP = 4;
        private static final int COL_WEATHER_HUMIDITY = 5;
        private static final int COL_WEATHER_WIND_SPEED = 6;
        private static final int COL_WEATHER_PRESSURE = 7;
        private static final int COL_WEATHER_DEGREES = 8;

        public DetailFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_activity_detail, container, false);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detail, menu);

            // Retrieve the share menu item
            MenuItem menuItem = menu.findItem(R.id.menu_share);

            // Get the provider and hold onto it to set/change the share intent.
//            mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            // If onLoadFinished happens before this, we can go ahead and set the share intent now.
//            if (mForecast != null) {
//                mShareActionProvider.setShareIntent(createShareForecastIntent());
//            }
        }

        private Intent createShareForecastIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
            return shareIntent;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.v(LOG_TAG, "In onCreateLoader");
            Intent intent = getActivity().getIntent();
            if (intent == null) return null;

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(getActivity(), intent.getData(), FORECAST_COLUMNS, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            Log.v(LOG_TAG, "In onLoadFinished");
            if (!data.moveToFirst()) return;

            boolean isMetric = Utility.isMetric(getActivity());

            String dateString = Utility.formatDate(data.getLong(COL_WEATHER_DATE));
            String weatherDescription = data.getString(COL_WEATHER_DESC);
            String high = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
            String low = Utility.formatTemperature(getContext(), data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
            String weekDay = Utility.getWeekDay(dateString);
            String humidity = Utility.formatHumidity(getContext(), data.getInt(COL_WEATHER_HUMIDITY));
            String wind = Utility.formatWindSpeed(getContext(), data.getDouble(COL_WEATHER_WIND_SPEED),
                    data.getDouble(COL_WEATHER_DEGREES));
            String pressure = Utility.formatPressure(getContext(), data.getDouble(COL_WEATHER_PRESSURE));

            View view = getView();
            if (view != null) {
                TextView weekD = (TextView) view.findViewById(R.id.details_week_day);
                TextView date = (TextView) view.findViewById(R.id.details_date);
                TextView h = (TextView) view.findViewById(R.id.details_high);
                TextView l = (TextView) view.findViewById(R.id.details_low);
                TextView d = (TextView) view.findViewById(R.id.details_description);
                TextView hum = (TextView) view.findViewById(R.id.details_humidity);
                TextView w = (TextView) view.findViewById(R.id.details_wind);
                TextView p = (TextView) view.findViewById(R.id.details_pressure);

                weekD.setText(weekDay);
                date.setText(dateString.substring(0, 6));
                h.setText(high);
                l.setText(low);
                d.setText(weatherDescription);
                hum.setText(humidity);
                w.setText(wind);
                p.setText(pressure);
            }


            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }
}
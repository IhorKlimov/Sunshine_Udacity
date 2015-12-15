package com.example.igorklimov.sunshine.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry;
import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.helpers.Utility;

import java.util.Calendar;
import java.util.Locale;

import static com.example.igorklimov.sunshine.helpers.Utility.*;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

    private ShareActionProvider mShareActionProvider;

    private TextView weekD;
    private TextView date;
    private TextView h;
    private TextView l;
    private TextView d;
    private TextView hum;
    private TextView w;
    private TextView p;
    private ImageView i;

    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_WEATHER_ID
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
    private static final int COL_WEATHER_CONDITION_ID = 9;
    public Uri mUri;
    private double high;
    private double low;
    private String weatherDescription;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.main, container, false);

        weekD = (TextView) inflate.findViewById(R.id.details_week_day);
        date = (TextView) inflate.findViewById(R.id.details_date);
        h = (TextView) inflate.findViewById(R.id.details_high);
        l = (TextView) inflate.findViewById(R.id.details_low);
        d = (TextView) inflate.findViewById(R.id.details_description);
        hum = (TextView) inflate.findViewById(R.id.details_humidity);
        w = (TextView) inflate.findViewById(R.id.details_wind);
        p = (TextView) inflate.findViewById(R.id.details_pressure);
        i = (ImageView) inflate.findViewById(R.id.details_image);
        return inflate;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detail, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.menu_share);


        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = new ShareActionProvider(getContext());
        MenuItemCompat.setActionProvider(menuItem, mShareActionProvider);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        mShareActionProvider.setShareIntent(createShareForecastIntent());
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getForecast(getContext(), high, low, weatherDescription)
                        + " " + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getActivity().getIntent();
        if (intent == null || intent.getData() == null) {
            Uri one = getArguments().getParcelable("one");
            return new CursorLoader(
                    getActivity(),
                    one,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null);
        } else {

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    intent.getData(),
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (!data.moveToFirst()) return;

        boolean isMetric = isMetric(getActivity());
        Calendar calendarDate = getCalendarDate(data.getLong(COL_WEATHER_DATE));

        weatherDescription = Utility.getDescription(data, getContext());
        this.high = data.getDouble(COL_WEATHER_MAX_TEMP);
        Context context = getContext();
        String high = formatTemperature(context, this.high, isMetric);
        this.low = data.getDouble(COL_WEATHER_MIN_TEMP);
        String low = formatTemperature(context, this.low, isMetric);
        String weekDay = getWeekDay(calendarDate, context);
        String humidity = formatHumidity(context, data.getInt(COL_WEATHER_HUMIDITY));
        String wind = formatWind(context, data.getDouble(COL_WEATHER_WIND_SPEED),
                data.getDouble(COL_WEATHER_DEGREES));
        String pressure = formatPressure(context, data.getDouble(COL_WEATHER_PRESSURE));
        String image = Utility.getArtUrlForWeatherCondition(data.getInt(COL_WEATHER_CONDITION_ID),context);

        weekD.setText(weekDay);
        String text = calendarDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) +
                " " + calendarDate.get(Calendar.DAY_OF_MONTH);
        date.setText(text);
        h.setText(high);
        l.setText(low);
        d.setText(weatherDescription);
        hum.setText(humidity);
        w.setText(wind);
        p.setText(pressure);

        int newSize = Utility.getSize(context, true);

        Glide.with(context).load(image).override(newSize, newSize).into(i);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void onLocationChanged(String newLocation) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            mUri = WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

}
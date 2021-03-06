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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
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
    public static final String DETAIL_TRANSITION_ANIMATION = "DTA";

    private boolean mTransitionAnimation;
    private Uri mUri;
    private double mHigh;
    private double mLow;
    private String mWeatherDescription;

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


    public DetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_detail_start, container, false);
        Bundle arguments = getArguments();
        if (arguments != null)
            mTransitionAnimation = arguments.getBoolean(DetailFragment.DETAIL_TRANSITION_ANIMATION);
        date = (TextView) inflate.findViewById(R.id.detail_date_textview);
        h = (TextView) inflate.findViewById(R.id.detail_high_textview);
        l = (TextView) inflate.findViewById(R.id.detail_low_textview);
        d = (TextView) inflate.findViewById(R.id.detail_forecast_textview);
        hum = (TextView) inflate.findViewById(R.id.detail_humidity_textview);
        w = (TextView) inflate.findViewById(R.id.detail_wind_textview);
        p = (TextView) inflate.findViewById(R.id.detail_pressure_textview);
        i = (ImageView) inflate.findViewById(R.id.detail_icon);
        return inflate;
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getForecast(getContext(), mHigh, mLow, mWeatherDescription)
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
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v(LOG_TAG, "In onLoadFinished");
        ViewParent parent = getView().getParent();
        if (parent instanceof CardView) ((View) parent).setVisibility(View.VISIBLE);

        if (!cursor.moveToFirst()) return;

        Calendar calendarDate = getCalendarDate(cursor.getLong(COL_WEATHER_DATE));

        mWeatherDescription = Utility.getDescription(cursor, getContext());
        this.mHigh = cursor.getDouble(COL_WEATHER_MAX_TEMP);
        Context context = getContext();
        String high = formatTemperature(context, this.mHigh);
        this.mLow = cursor.getDouble(COL_WEATHER_MIN_TEMP);
        String low = formatTemperature(context, this.mLow);
        String d = getWeekDay(calendarDate, context) + ", " +
                calendarDate.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) +
                " " + calendarDate.get(Calendar.DAY_OF_MONTH);
        String humidity = formatHumidity(context, cursor.getInt(COL_WEATHER_HUMIDITY));
        String wind = formatWind(context, cursor.getDouble(COL_WEATHER_WIND_SPEED),
                cursor.getDouble(COL_WEATHER_DEGREES));
        String pressure = formatPressure(context, cursor.getDouble(COL_WEATHER_PRESSURE));

        date.setText(d);
        h.setText(high);
        l.setText(low);
        this.d.setText(mWeatherDescription);
        hum.setText(humidity);
        w.setText(wind);
        p.setText(pressure);

        if (!Utility.getIconStyle(context).equals("1")) {
            int newSize = Utility.getSize(context, true);
            String image = Utility.getArtUrlForWeatherCondition(cursor.getInt(COL_WEATHER_CONDITION_ID), context);
            Glide.with(context).load(image).override(newSize, newSize).into(i);
        } else {
            i.setImageResource(Utility.getArtResourceForWeatherCondition(cursor,context));
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

        // We need to start the enter transition after the cursor has loaded
        if (mTransitionAnimation) {
            activity.supportStartPostponedEnterTransition();

            if (null != toolbarView) {
                activity.setSupportActionBar(toolbarView);

                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if (null != toolbarView) {
                Menu menu = toolbarView.getMenu();
                if (null != menu) menu.clear();
                toolbarView.inflateMenu(R.menu.detail);
                finishCreatingMenu(toolbarView.getMenu());
            }
        }
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.menu_share);

        // Get the provider and hold onto it to set/change the share intent.
        ShareActionProvider mShareActionProvider = new ShareActionProvider(getContext());
        MenuItemCompat.setActionProvider(menuItem, mShareActionProvider);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        mShareActionProvider.setShareIntent(createShareForecastIntent());
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
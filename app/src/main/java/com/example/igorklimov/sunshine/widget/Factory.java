/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.igorklimov.sunshine.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.DetailActivity;
import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.helpers.Utility;

import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_DATE;
import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_MAX_TEMP;
import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_MIN_TEMP;
import static com.example.igorklimov.sunshine.fragments.ForecastFragment.COL_WEATHER_CONDITION_ID;
import static com.example.igorklimov.sunshine.fragments.ForecastFragment.COL_WEATHER_DATE;
import static com.example.igorklimov.sunshine.fragments.ForecastFragment.COL_WEATHER_MAX_TEMP;
import static com.example.igorklimov.sunshine.fragments.ForecastFragment.COL_WEATHER_MIN_TEMP;
import static com.example.igorklimov.sunshine.helpers.Utility.getCalendarDate;
import static com.example.igorklimov.sunshine.helpers.Utility.getWeekDay;
import static com.example.igorklimov.sunshine.helpers.Utility.isMetric;

/**
 * Created by Igor Klimov on 12/30/2015.
 */
public class Factory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "Factory";
    private Context mContext;
    private int mWidgetId;

    public Factory(Context context, Intent intent) {
        mContext = context;
        mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return 14;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews v = new RemoteViews(mContext.getPackageName(), R.layout.widget_detail_list_item);
        String locationSetting = Utility.getPreferredLocation(mContext);
        String sort = COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());
        Cursor c = mContext.getContentResolver()
                .query(weatherForLocationUri, null, null, null, sort);

        int icon = 0;
        String date = "";
        String descr = "";
        String high = "";
        String low = "";
        Uri data = null;
        if (c != null && c.moveToPosition(position)) {
            icon = Utility.getArtResourceForWeatherCondition(c, mContext);
            date = Utility.getCalendarDate(c, mContext);
            descr = Utility.getDescription(c, mContext);
            high = Utility.formatTemperature(mContext, c.getDouble(c.getColumnIndex(COLUMN_MAX_TEMP)));
            low = Utility.formatTemperature(mContext, c.getDouble(c.getColumnIndex(COLUMN_MIN_TEMP)));
            data = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting,
                    c.getLong(c.getColumnIndex(COLUMN_DATE)));
            c.close();
        }

        v.setImageViewResource(R.id.widget_icon, icon);
        v.setTextViewText(R.id.widget_date, date);
        v.setTextViewText(R.id.widget_description, descr);
        v.setTextViewText(R.id.widget_temp_max, high);
        v.setTextViewText(R.id.widget_temp_min, low);

        v.setOnClickFillInIntent(R.id.widget_list_item, new Intent().setData(data));

        return v;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        Log.v(TAG, "getItemId");
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}

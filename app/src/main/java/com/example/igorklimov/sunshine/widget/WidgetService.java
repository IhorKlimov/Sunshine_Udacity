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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.MainActivity;
import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.helpers.Utility;

import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_MAX_TEMP;
import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_MIN_TEMP;
import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_SHORT_DESC;
import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_WEATHER_ID;

/**
 * Created by Igor Klimov on 12/28/2015.
 */
public class WidgetService extends android.app.IntentService {
    /**
     * Creates an WidgetService.  Invoked by your subclass's constructor.
     */
    public static final String TAG = "WidgetService";

    public WidgetService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(context, WidgetProvider.class));

        int weatherArtResourceId = R.drawable.art_clear;
        String description = "";
        double maxTemp = 0;
        double minTemp = 0;
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        String locationSetting = Utility.getPreferredLocation(context);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());
        Cursor cursor = context.getContentResolver()
                .query(weatherForLocationUri, null, null, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            description = cursor.getString(cursor.getColumnIndex(COLUMN_SHORT_DESC));
            weatherArtResourceId = Utility.getArtResourceForWeatherCondition(cursor, context);
            maxTemp = cursor.getDouble(cursor.getColumnIndex(COLUMN_MAX_TEMP));
            minTemp = cursor.getDouble(cursor.getColumnIndex(COLUMN_MIN_TEMP));
            cursor.close();
        }
        String formattedMaxTemperature = Utility.formatTemperature(context, maxTemp);
        String formattedMinTemperature = Utility.formatTemperature(context, minTemp);

        // Perform this loop procedure for each Today widget
        for (int i = 0; i < appWidgetIds.length; i++) {
            int layoutId;
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("layout")) layoutId = extras.getInt("layout");
            else layoutId = R.layout.appwidget_small;
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

            // Add the data to the RemoteViews
            views.setImageViewResource(R.id.widget_icon, weatherArtResourceId);
            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                setRemoteContentDescription(views, description);
            }
            views.setTextViewText(R.id.widget_temp_max, formattedMaxTemperature);
            if (layoutId == R.layout.appwidget_medium || layoutId == R.layout.appwidget_large) {
                views.setTextViewText(R.id.widget_temp_min, formattedMinTemperature);
            }
            if (layoutId == R.layout.appwidget_large) {
                views.setTextViewText(R.id.widget_description, description);
            }
            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void setRemoteContentDescription(RemoteViews views, String description) {
        views.setContentDescription(R.id.widget_icon, description);
    }

}

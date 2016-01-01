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

package com.example.igorklimov.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.igorklimov.sunshine.BuildConfig;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.MainActivity;
import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry;
import com.example.igorklimov.sunshine.helpers.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import static com.example.igorklimov.sunshine.helpers.Utility.getForecast;
import static com.example.igorklimov.sunshine.helpers.Utility.isNotificationOn;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String ACTION_DATE_UPDATED =
            "com.example.igorklimov.sunshine.ACTION_DATA_UPDATED";

    private Context context;

    private static final int SYNC_INTERVAL = 60 * 180;
    private static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCATION_STATUS_OK, LOCATION_STATUS_SERVER_DOWN, LOCATION_STATUS_SERVER_INVALID,
            LOCATION_STATUS_UNKNOWN, LOCATION_STATUS_INVALID})
    public @interface LocationStatus {
    }

    public static final int LOCATION_STATUS_OK = 0;
    public static final int LOCATION_STATUS_SERVER_DOWN = 1;
    public static final int LOCATION_STATUS_SERVER_INVALID = 2;
    public static final int LOCATION_STATUS_UNKNOWN = 3;
    public static final int LOCATION_STATUS_INVALID = 4;

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[]{
            WeatherEntry.COLUMN_WEATHER_ID,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_SHORT_DESC
    };

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Log.d("TAG", "syncImmediately: ");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Context context = getContext();
        String locationQuery = Utility.getPreferredLocation(context);
        String locationLatitude = String.valueOf(Utility.getLocationLatitude(context));
        String locationLongitude = String.valueOf(Utility.getLocationLongitude(context));

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr;

        String units = "metric";
        int numDays = 14;

        try {
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String UNITS_PARAM = "units";
            final String LAT_PARAM = "lat";
            final String LON_PARAM = "lon";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            Uri.Builder uriBuilder = Uri.parse(FORECAST_BASE_URL).buildUpon();

            // Instead of always building the query based off of the location string, we want to
            // potentially build a query using a lat/lon value. This will be the case when we are
            // syncing based off of a new location from the Place Picker API. So we need to check
            // if we have a lat/lon to work with, and use those when we do. Otherwise, the weather
            // service may not understand the location address provided by the Place Picker API
            // and the user could end up with no weather! The horror!
            if (Utility.isLocationLatLonAvailable(context)) {
                uriBuilder.appendQueryParameter(LAT_PARAM, locationLatitude)
                        .appendQueryParameter(LON_PARAM, locationLongitude);
            } else {
                uriBuilder.appendQueryParameter(QUERY_PARAM, locationQuery);
            }

            Uri builtUri = uriBuilder
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

            Log.d("TAG", builtUri.toString());

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) return;
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            if (buffer.length() == 0) {
                Utility.setLocationStatusPreference(LOCATION_STATUS_SERVER_DOWN, context);
                return;
            }

            forecastJsonStr = buffer.toString();
            getWeatherDataFromJson(forecastJsonStr, locationQuery);
        } catch (IOException e) {
            Utility.setLocationStatusPreference(LOCATION_STATUS_SERVER_DOWN, context);
            Log.e("TAG", "Error ", e);
        } catch (JSONException e) {
            Utility.setLocationStatusPreference(LOCATION_STATUS_SERVER_INVALID, context);
            Log.e("TAG", "onPerformSync: ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("TAG", "Error closing stream", e);
                }
            }
        }

    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if (null == accountManager.getPassword(newAccount)) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) return null;
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getWeatherDataFromJson(String forecastJsonStr, String locationSetting) throws JSONException {

        final String OWM_MESSAGE_CODE = "cod";

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";

        // Location coordinate
        final String OWM_LATITUDE = "lat";
        final String OWM_LONGITUDE = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        try {
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            int code = 0;
            if (forecastJson.has(OWM_MESSAGE_CODE)) code = forecastJson.getInt(OWM_MESSAGE_CODE);
            Log.d("TAG", "getWeatherDataFromJson: " + code);


            if (code != 404) {
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
                String cityName = cityJson.getString(OWM_CITY_NAME);

                JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
                double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
                double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);

                long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

                Vector<ContentValues> cVVector = new Vector<>(weatherArray.length());

                Time dayTime = new Time();
                dayTime.setToNow();

                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
                dayTime = new Time();

                for (int i = 0; i < weatherArray.length(); i++) {
                    // These are the values that will be collected.
                    long dateTime;
                    double pressure;
                    int humidity;
                    double windSpeed;
                    double windDirection;

                    double high;
                    double low;

                    String description;
                    int weatherId;

                    // Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // Cheating to convert this to UTC time, which is what we want anyhow
                    dateTime = dayTime.setJulianDay(julianStartDay + i);

                    pressure = dayForecast.getDouble(OWM_PRESSURE);
                    humidity = dayForecast.getInt(OWM_HUMIDITY);
                    windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
                    windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);

                    // Description is in a child array called "weather", which is 1 element long.
                    // That element also contains a weather code.
                    JSONObject weatherObject =
                            dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);
                    weatherId = weatherObject.getInt(OWM_WEATHER_ID);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    high = temperatureObject.getDouble(OWM_MAX);
                    low = temperatureObject.getDouble(OWM_MIN);

                    ContentValues weatherValues = new ContentValues();

                    weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
                    weatherValues.put(WeatherEntry.COLUMN_DATE, dateTime);
                    weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
                    weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
                    weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
                    weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
                    weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
                    weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
                    weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
                    weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

                    cVVector.add(weatherValues);
                }

                // add to database
                if (cVVector.size() > 0) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    int delete = context.getContentResolver().delete(WeatherEntry.CONTENT_URI,
                            WeatherEntry.COLUMN_DATE + " <= ?",
                            new String[]{dayTime.setJulianDay(julianStartDay - 1) + ""});
                    context.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);
                    Log.d("TAG", delete + " deleted");
                    Intent dataUpdatedIntent = new Intent(ACTION_DATE_UPDATED);
                    context.sendBroadcast(dataUpdatedIntent);
                    if (isNotificationOn(context)) notifyWeather();
                }
                Utility.setLocationStatusPreference(LOCATION_STATUS_OK, context);
            } else {
                Utility.setLocationStatusPreference(LOCATION_STATUS_INVALID, context);
            }
        } catch (JSONException e) {
            Log.e("TAG", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (locationCursor != null && locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = context.getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        if (locationCursor != null) locationCursor.close();
        return locationId;
    }

    private static void onAccountCreated(Account newAccount, Context context) {

        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    private void notifyWeather() {
        Log.d("TAG", "notifyWeather()");
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastNotificationKey = context.getString(R.string.pref_last_notification);
        long lastSync = prefs.getLong(lastNotificationKey, 0);

        if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
            // Last sync was more than 1 day ago, let's send a notification with the weather.
            String locationQuery = Utility.getPreferredLocation(context);

            Uri weatherUri = WeatherEntry
                    .buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

            // we'll query our contentProvider, as always
            Cursor cursor = context.getContentResolver()
                    .query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                double high = cursor.getDouble(INDEX_MAX_TEMP);
                double low = cursor.getDouble(INDEX_MIN_TEMP);
                String desc = cursor.getString(INDEX_SHORT_DESC);

//                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                String title = context.getString(R.string.app_name);

                // Define the text of the forecast.
                String contentText = getForecast(context, high, low, desc);

                Bitmap bitmap;
                if (!Utility.getIconStyle(context).equals("1")) {
                    String url = Utility.getArtUrlForWeatherCondition(weatherId, context);
                    bitmap = null;
                    try {
                        bitmap = Glide.with(context).load(url).asBitmap()
                                .into(R.dimen.notification_large_icon_default, R.dimen.notification_large_icon_default)
                                .get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    bitmap = BitmapFactory.decodeResource(context.getResources(),
                            Utility.getArtResourceForWeatherCondition(cursor,context));
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(bitmap)
                        .setContentTitle(title)
                        .setContentText(contentText);
                Intent intent = new Intent(context, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(intent);
                PendingIntent pendingIntent = stackBuilder
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(pendingIntent);
                NotificationManager manager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(WEATHER_NOTIFICATION_ID, builder.build());

                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.apply();
            }
            if (cursor != null) cursor.close();
        }
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

}
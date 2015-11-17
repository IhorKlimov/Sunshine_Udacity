package com.example.igorklimov.sunshine;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utility {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d", Locale.US);
    static final SimpleDateFormat WEEK_DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.US);
    static final SimpleDateFormat FULL_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private static int today = Integer.parseInt(Utility.DATE_FORMAT.format(new Date(System.currentTimeMillis())));

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.location_key), context.getString(R.string.location_default_value));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.unit_system_key), context.getString(R.string.metric)).equals("" + 1);
    }

    static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if (!isMetric) {
            temp = 9 * temperature / 5 + 32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature, temp);
    }

    static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }

    static void setToday() {
        today = Integer.parseInt(Utility.DATE_FORMAT.format(new Date(System.currentTimeMillis())));
    }

    @NonNull
    static String formatDate(String dt) {
        int day = Integer.parseInt(dt.substring(4, 6));

        try {
            if (day == today) dt = "Today";
            else if (day == today + 1) dt = "Tomorrow";
            else if (day >= today + 2 && day <= today + 6) {
                dt = Utility.WEEK_DAY_FORMAT.format(Utility.FULL_FORMAT.parse(dt));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dt;
    }

    static String getWeekDay(String date) {
        int day = Integer.parseInt(date.substring(4, 6));
        String result = null;

        try {
            if (day == today) result = "Today";
            else if (day == today + 1) result = "Tomorrow";
            else result = WEEK_DAY_FORMAT.format(FULL_FORMAT.parseObject(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String formatHumidity(Context context, int pressure) {
        return context.getString(R.string.format_humidity, pressure);
    }

    public static String formatWindSpeed(Context context,double windSpeed, double degrees) {
        return context.getString(R.string.format_wind_km, windSpeed, degrees);
    }

    public static String formatPressure(Context context, double pressure) {
        return context.getString(R.string.format_pressure, pressure);
    }
}

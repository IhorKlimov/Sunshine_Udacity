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
        if (!isMetric) temp = 9 * temperature / 5 + 32;
        else temp = temperature;

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
        int day = Integer.parseInt(dt.substring(4, dt.indexOf(",")));
        if (day >= today && day <= today + 6) dt = getWeekDay(dt);

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

    public static String formatWind(Context context, double windSpeed, double degrees) {
        String destination = null;

        if (degrees >= 337.5 && degrees <= 360 || degrees >= 0 && degrees <= 22.5)
            destination = "N";
        else if (degrees > 22.5 && degrees < 67.5) destination = "NE";
        else if (degrees >= 67.5 && degrees <= 112.5) destination = "E";
        else if (degrees > 112.5 && degrees < 157.5) destination = "SE";
        else if (degrees >= 157.5 && degrees <= 202.5) destination = "S";
        else if (degrees > 202.5 && degrees < 247.5) destination = "SW";
        else if (degrees >= 247.5 && degrees <= 292.5) destination = "W";
        else if (degrees > 292.5 && degrees < 337.5) destination = "NW";

        return context.getString(R.string.format_wind_km, windSpeed, destination);
    }

    public static String formatPressure(Context context, double pressure) {
        return context.getString(R.string.format_pressure, pressure);
    }
}

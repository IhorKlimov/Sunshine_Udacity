package com.example.igorklimov.sunshine.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.igorklimov.sunshine.R;

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

    public static boolean isNotificationOn(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.key_notifications), true);
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String string = prefs.getString(context.getString(R.string.unit_system_key), context.getString(R.string.metric));
        return string.equals(context.getString(R.string.metric)) || string.equals(1 + "");
    }

    public static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if (!isMetric) temp = 9 * temperature / 5 + 32;
        else temp = temperature;

        return context.getString(R.string.format_temperature, temp);
    }

    public static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }

    public static void setToday() {
        today = Integer.parseInt(Utility.DATE_FORMAT.format(new Date(System.currentTimeMillis())));
    }

    @NonNull
    static String formatDate(String dt) {
        int day = Integer.parseInt(dt.substring(4, dt.indexOf(",")));
        if (day == today) dt = getWeekDay(dt) + ", " + dt.substring(0, dt.indexOf(","));
        else if (day > today && day <= today + 6) dt = getWeekDay(dt);

        return dt;
    }

    public static String getWeekDay(String date) {
        int day = Integer.parseInt(date.substring(4, date.indexOf(",")));
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
        Log.d("TAG", degrees + "");
        if (degrees >= 337.5 || degrees <= 22.5) destination = "N";
        else if (degrees > 22.5 && degrees < 67.5) destination = "NE";
        else if (degrees >= 67.5 && degrees <= 112.5) destination = "E";
        else if (degrees > 112.5 && degrees < 157.5) destination = "SE";
        else if (degrees >= 157.5 && degrees <= 202.5) destination = "S";
        else if (degrees > 202.5 && degrees < 247.5) destination = "SW";
        else if (degrees >= 247.5 && degrees <= 292.5) destination = "W";
        else if (degrees > 292.5 && degrees < 337.5) destination = "NW";
        Log.d("TAG", destination);
        return context.getString(R.string.format_wind_km, windSpeed, destination);
    }

    public static String formatPressure(Context context, double pressure) {
        return context.getString(R.string.format_pressure, pressure);
    }

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     *
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    /**
     * Helper method to provide the art resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     *
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding image. -1 if no relation is found.
     */
    public static int getArtResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_rain;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        }
        return -1;
    }

}

package com.example.igorklimov.sunshine.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.data.WeatherContract;
import com.example.igorklimov.sunshine.fragments.ForecastFragment;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter.LocationStatus;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry.COLUMN_SHORT_DESC;

public class Utility {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d", Locale.US);
    static final SimpleDateFormat WEEK_DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.getDefault());
    static final SimpleDateFormat FULL_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static int today = Integer.parseInt(Utility.DATE_FORMAT
            .format(new Date(System.currentTimeMillis())));

    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.location_key),
                context.getString(R.string.location_default_value));
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

    public static String formatTemperature(Context context, double temperature,
                                           boolean isMetric) {
        double temp;
        if (!isMetric) temp = 9 * temperature / 5 + 32;
        else temp = temperature;

        return context.getString(R.string.format_temperature, temp);
    }

    public static String getForecast(Context context, double high, double low, String desc) {
        return String.format(context.getString(R.string.format_notification),
                desc,
                Utility.formatTemperature(context, high, isMetric(context)),
                Utility.formatTemperature(context, low, isMetric(context)));
    }

    public static Calendar getCalendarDate(long dateInMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateInMillis);
        return cal;
    }

    public static void setToday() {
        today = Integer.parseInt(Utility.DATE_FORMAT.format(new Date(System.currentTimeMillis())));
    }

    public static void setLocationStatusPreference(@LocationStatus int status, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(context.getString(R.string.location_status_key), status).commit();
    }

    public static void resetLocationPreference(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(context.getString(R.string.location_status_key), SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN)
                .apply();
    }

    @SuppressWarnings("ResourceType")
    @LocationStatus
    public static int getLocationStatusPreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.location_status_key), SunshineSyncAdapter.LOCATION_STATUS_OK);
    }

    static String getCalendarDate(Cursor cursor, Context c) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cursor.getLong(ForecastFragment.COL_WEATHER_DATE));

        int day = cal.get(Calendar.DAY_OF_MONTH);
        String res;
        if (day == today) {
            res = getWeekDay(cal, c) + ", "
                    + cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                    + " " + day;
        } else if (day > today && day <= today + 6) {
            res = getWeekDay(cal, c);
        } else {
            res = FULL_FORMAT.format(cal.getTime());
        }

        return res;
    }

    public static String getWeekDay(Calendar date, Context c) {
        int day = date.get(Calendar.DAY_OF_MONTH);
        String result;

        if (day == today) result = c.getString(R.string.today);
        else if (day == today + 1) result = c.getString(R.string.tomorrow);
        else result = WEEK_DAY_FORMAT.format(date.getTime());

        return result;
    }

    public static String formatHumidity(Context context, int pressure) {
        return context.getString(R.string.format_humidity, pressure);
    }

    public static String formatWind(Context c, double windSpeed, double degrees) {
        String destination = null;
        Log.d("TAG", degrees + "");
        if (degrees >= 337.5 || degrees <= 22.5) destination = c.getString(R.string.n);
        else if (degrees > 22.5 && degrees < 67.5) destination = c.getString(R.string.ne);
        else if (degrees >= 67.5 && degrees <= 112.5) destination = c.getString(R.string.e);
        else if (degrees > 112.5 && degrees < 157.5) destination = c.getString(R.string.se);
        else if (degrees >= 157.5 && degrees <= 202.5) destination = c.getString(R.string.s);
        else if (degrees > 202.5 && degrees < 247.5) destination = c.getString(R.string.sw);
        else if (degrees >= 247.5 && degrees <= 292.5) destination = c.getString(R.string.w);
        else if (degrees > 292.5 && degrees < 337.5) destination = c.getString(R.string.nw);
        return c.getString(R.string.format_wind_km, windSpeed, destination);
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

    public static String getArtUrlForWeatherCondition(int weatherId, Context c) {
        String is = PreferenceManager.getDefaultSharedPreferences(c).getString(c.getString(R.string.icons_key), "");
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_storm.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_storm.png";
        } else if (weatherId >= 300 && weatherId <= 321) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_light_rain.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_light_rain.png";
        } else if (weatherId >= 500 && weatherId <= 504) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_rain.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_rain.png";
        } else if (weatherId == 511) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_snow.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_snow.png";
        } else if (weatherId >= 520 && weatherId <= 531) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_rain.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_rain.png";
        } else if (weatherId >= 600 && weatherId <= 622) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_rain.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_rain.png";
        } else if (weatherId >= 701 && weatherId <= 761) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_fog.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_fog.png";
        } else if (weatherId == 761 || weatherId == 781) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_storm.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_storm.png";
        } else if (weatherId == 800) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_clear.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_clear.png";
        } else if (weatherId == 801) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_light_clouds.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_light_clouds.png";
        } else if (weatherId >= 802 && weatherId <= 804) {
            return is.equals("1") ? "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Colored/art_clouds.png"
                    : "https://raw.githubusercontent.com/udacity/sunshine_icons/master/Archive/Mono/art_clouds.png";
        }
        return "";
    }

    public static int getSize(Context context, boolean big) {
        int newSize = 0;
        float density = context.getResources().getDisplayMetrics().density;
        Log.d("TAG", "onLoadFinished: " + density);
        if (big) {
            if (density == 1.0f) newSize = 144;
            else if (density == 1.5f) newSize = 216;
            else if (density == 2.0f) newSize = 288;
            else if (density >= 3.0f) newSize = 432;
            return newSize;
        } else {
            if (density == 1.0f) newSize = 32;
            else if (density == 1.5f) newSize = 48;
            else if (density == 2.0f) newSize = 64;
            else if (density >= 3.0f) newSize = 96;
            return newSize;
        }
    }

    public static String getDescription(Cursor data, Context c) {
        String d = data.getString(data.getColumnIndex(COLUMN_SHORT_DESC));
        switch (d) {
            case "Thunderstorm":
                d = c.getString(R.string.thunderstorm);
                break;
            case "Rain":
                d = c.getString(R.string.rain);
                break;
            case "Snow":
                d = c.getString(R.string.snow);
                break;
            case "Clear":
                d = c.getString(R.string.clear);
                break;
            case "Clouds":
                d = c.getString(R.string.clouds);
                break;
        }

        return d;
    }
}

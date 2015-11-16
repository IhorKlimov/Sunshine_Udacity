package com.example.igorklimov.sunshine;


import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d", Locale.US);
    private static final SimpleDateFormat WEEK_DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.US);
    private static final SimpleDateFormat FULL_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private static final int TODAY = Integer.parseInt(DATE_FORMAT.format(new Date(System.currentTimeMillis())));

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private String getDate(Cursor cursor) {
        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE));
    }

    private String getDetails(Cursor cursor) {
        return cursor.getString(ForecastFragment.COL_WEATHER_DESC);
    }

    private String getHighs(Cursor cursor) {
        return Utility.formatTemperature(cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                Utility.isMetric(mContext)) + "\u00b0";
    }

    private String getLows(Cursor cursor) {
        return Utility.formatTemperature(cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP),
                Utility.isMetric(mContext)) + "\u00b0";
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.


        LinearLayout layout = (LinearLayout) view;
        TextView date = (TextView) layout.findViewById(R.id.item_forecast_textview_date);
        TextView forecast = (TextView) layout.findViewById(R.id.item_forecast_textview_details);
        TextView high = (TextView) layout.findViewById(R.id.item_forecast_textview_high);
        TextView low = (TextView) layout.findViewById(R.id.item_forecast_textview_low);

        String dt = formatDate(getDate(cursor));

        date.setText(dt);
        forecast.setText(getDetails(cursor));
        high.setText(getHighs(cursor));
        low.setText(getLows(cursor));
    }

    @NonNull
    private String formatDate(String dt) {
        int day = Integer.parseInt(dt.substring(4, 6));

        try {
            if (day == TODAY) dt = "Today";
            else if (day == TODAY + 1) dt = "Tomorrow";
            else if (day >= TODAY +2 && day <= TODAY + 6) dt = WEEK_DAY_FORMAT.format(FULL_FORMAT.parse(dt));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dt;
    }
}

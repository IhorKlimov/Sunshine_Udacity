package com.example.igorklimov.sunshine;


import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    private final Context context;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    private String getDate(Cursor cursor) {
        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE));
    }

    private String getDetails(Cursor cursor) {
        return cursor.getString(ForecastFragment.COL_WEATHER_DESC);
    }

    private String getHighs(Cursor cursor) {
        return Utility.formatTemperature(context, cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                Utility.isMetric(mContext));
    }

    private String getLows(Cursor cursor) {
        return Utility.formatTemperature(context, cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP),
                Utility.isMetric(mContext));
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = viewType == VIEW_TYPE_TODAY
                ? R.layout.list_item_forecast_today
                : R.layout.list_item_forecast;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        Log.d("TAG", "newView()");
        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.

        ViewHolder holder = (ViewHolder) view.getTag();

//        LinearLayout layout = (LinearLayout) view;
//        TextView date = (TextView) layout.findViewById(R.id.item_forecast_textview_date);
//        TextView details = (TextView) layout.findViewById(R.id.item_forecast_textview_details);
//        TextView high = (TextView) layout.findViewById(R.id.item_forecast_textview_high);
//        TextView low = (TextView) layout.findViewById(R.id.item_forecast_textview_low);

        String dt = Utility.formatDate(getDate(cursor));

        holder.date.setText(dt);
        holder.details.setText(getDetails(cursor));
        holder.high.setText(getHighs(cursor));
        holder.low.setText(getLows(cursor));
    }


    private static class ViewHolder {
        ImageView image;
        TextView date;
        TextView details;
        TextView high;
        TextView low;

        public ViewHolder(View view) {
            this.image = (ImageView) view.findViewById(R.id.image);
            this.date = (TextView) view.findViewById(R.id.item_forecast_textview_date);
            this.details = (TextView) view.findViewById(R.id.item_forecast_textview_details);
            this.high = (TextView) view.findViewById(R.id.item_forecast_textview_high);
            this.low = (TextView) view.findViewById(R.id.item_forecast_textview_low);
        }

    }

}

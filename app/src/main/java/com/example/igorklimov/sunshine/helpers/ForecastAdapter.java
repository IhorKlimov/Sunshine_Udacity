package com.example.igorklimov.sunshine.helpers;


import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.MainActivity;
import com.example.igorklimov.sunshine.fragments.ForecastFragment;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    private Context context;

    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.context = context;
    }

    public void setContext(Context context) {
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
        int layoutId;
        MainActivity con = (MainActivity) context;
        if (isToday(cursor) && !con.isTablet) {
            layoutId = R.layout.list_item_forecast_today;
        } else layoutId = R.layout.list_item_forecast;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

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
        int conditionId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        String dt = Utility.getCalendarDate(cursor, context);
        MainActivity con = (MainActivity) context;
        holder.date.setText(dt);
        holder.details.setText(Utility.getDescription(cursor, context));
        holder.high.setText(getHighs(cursor));
        holder.low.setText(getLows(cursor));

        String pic = Utility.getArtUrlForWeatherCondition(conditionId, context);
        int newSize = 0;
        if (isToday(cursor) && !con.isTablet) {
            newSize = Utility.getSize(context, true);
        } else {
            newSize = Utility.getSize(context, false);
        }
        holder.image.setMinimumHeight(newSize);
        holder.image.setMinimumWidth(newSize);
        Glide.with(context).load(pic).override(newSize, newSize).into(holder.image);

    }

    private boolean isToday(Cursor cursor) {
        int viewType = getItemViewType(cursor.getPosition());
        return viewType == VIEW_TYPE_TODAY;
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

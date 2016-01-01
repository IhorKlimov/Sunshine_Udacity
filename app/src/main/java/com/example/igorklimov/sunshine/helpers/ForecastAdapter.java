/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.igorklimov.sunshine.helpers;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.MainActivity;
import com.example.igorklimov.sunshine.fragments.ForecastFragment;

import static com.example.igorklimov.sunshine.fragments.ForecastFragment.COL_WEATHER_DATE;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    public Cursor cursor;
    private Context mContext;
    private final ForecastAdapterOnClickHandler mClickHandler;
    private final View mEmptyView;
    private ItemChoiceManager mICM;

    public ForecastAdapter(Context context, Cursor c, ForecastAdapterOnClickHandler handler, View mEmptyView) {
        super();
        cursor = c;
        this.mClickHandler = handler;
        this.mEmptyView = mEmptyView;
        this.mContext = context;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(1);
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ViewHolder) {
            ViewHolder vfh = (ViewHolder) viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        MainActivity con = (MainActivity) mContext;
        if (viewType == VIEW_TYPE_TODAY && !con.isTablet) {
            layoutId = R.layout.list_item_forecast_today;
        } else {
            layoutId = R.layout.list_item_forecast;
        }
        View view = LayoutInflater.from(mContext).inflate(layoutId, parent, false);
        return new ViewHolder(view, mContext);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        int conditionId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        String dt = Utility.getCalendarDate(cursor, mContext);
        MainActivity con = (MainActivity) mContext;
        holder.date.setText(dt);
        holder.details.setText(Utility.getDescription(cursor, mContext));
        holder.high.setText(getHighs(cursor));
        holder.low.setText(getLows(cursor));

        int newSize = 0;
        if (position == 0 && !con.isTablet) {
            newSize = Utility.getSize(mContext, true);
        } else {
            newSize = Utility.getSize(mContext, false);
        }
        if (!Utility.getIconStyle(mContext).equals("1")) {
            String pic = Utility.getArtUrlForWeatherCondition(conditionId, mContext);
            holder.image.setMinimumHeight(newSize);
            holder.image.setMinimumWidth(newSize);
            Glide.with(mContext).load(pic).override(newSize, newSize).into(holder.image);
        } else {
            Glide.with(mContext).load(Utility.getArtResourceForWeatherCondition(cursor, mContext))
                    .override(newSize, newSize).into(holder.image);
        }
        ViewCompat.setTransitionName(holder.image, "iconView" + position);

        mICM.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

    public int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    @Override
    public int getItemCount() {
        if (cursor != null) return cursor.getCount();
        else return 0;
    }

    private String getHighs(Cursor cursor) {
        return Utility.formatTemperature(mContext, cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
    }

    private String getLows(Cursor cursor) {
        return Utility.formatTemperature(mContext, cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
    }

    public void swapCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return cursor;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView image;
        TextView date;
        TextView details;
        TextView high;
        TextView low;
        Context context;

        public ViewHolder(View view, Context context) {
            super(view);
            view.setOnClickListener(this);
            this.context = context;
            this.image = (ImageView) view.findViewById(R.id.list_item_icon);
            this.date = (TextView) view.findViewById(R.id.list_item_date_textview);
            this.details = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            this.high = (TextView) view.findViewById(R.id.list_item_high_textview);
            this.low = (TextView) view.findViewById(R.id.list_item_low_textview);
        }

        @Override
        public void onClick(View v) {
            cursor.moveToPosition(getAdapterPosition());
            long date = cursor.getLong(COL_WEATHER_DATE);
            mClickHandler.onClick(date, this);
            if (Utility.isTablet(context)) mICM.onClick(this);
        }
    }

    public static interface ForecastAdapterOnClickHandler {
        void onClick(long date, ViewHolder holder);
    }

}



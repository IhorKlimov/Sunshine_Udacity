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

package com.example.igorklimov.sunshine.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;

/**
 * Created by Igor Klimov on 12/28/2015.
 */
public class WidgetProvider extends android.appwidget.AppWidgetProvider {

    private static final String TAG = WidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, WidgetService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(SunshineSyncAdapter.ACTION_DATE_UPDATED)) {
            context.startService(new Intent(context, WidgetService.class));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        int width = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int layout = getLayout(width);
        context.startService(new Intent(context, WidgetService.class).putExtra("layout", layout));
    }

    private int getLayout(int width) {
        if (width < 110) return R.layout.appwidget_small;
        else if (width >= 110 && width < 180) return R.layout.appwidget_medium;
        else return R.layout.appwidget_large;
    }
}

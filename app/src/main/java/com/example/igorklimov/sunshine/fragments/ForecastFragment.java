package com.example.igorklimov.sunshine.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.MainActivity;
import com.example.igorklimov.sunshine.data.WeatherContract.LocationEntry;
import com.example.igorklimov.sunshine.data.WeatherContract.WeatherEntry;
import com.example.igorklimov.sunshine.helpers.ForecastAdapter;
import com.example.igorklimov.sunshine.helpers.Utility;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String SELECTED_KEY = "selected_position";
    private static final int FORECAST_LOADER = 0;
    private static final long THREE_HOURS = 3 * 60 * 60 * 1000;
    private static long lastUpdate;

    public ForecastAdapter mForecastAdapter;
    RecyclerView mRecyclerView;
    MainActivity mainActivity;
    private int pos = 0;
    private Context context;
    private boolean mUseTodayLayout;
    private boolean mHoldForTransition;


    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG,
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    public static final int COL_WEATHER_CONDITION_ID = 6;
    public static final int COL_COORD_LAT = 7;
    public static final int COL_COORD_LONG = 8;
    private TextView notAvailable;
    private int mChoiceMode;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_location:
                Uri geoLocation = Uri.parse(getGeoLocation());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The CursorAdapter will take data from our cursor and populate the ListView.
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        notAvailable = (TextView) rootView.findViewById(R.id.recyclerview_forecast_empty);
        mainActivity = (MainActivity) getActivity();
        mForecastAdapter = new ForecastAdapter(getActivity(), null, new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(long date, ForecastAdapter.ViewHolder holder) {
                String location = Utility.getPreferredLocation(context);
                ((Callback) getActivity())
                        .onItemSelected(WeatherEntry.buildWeatherLocationWithDate(location, date), holder);
                pos = holder.getAdapterPosition();
            }
        }, notAvailable);
        // Get a reference to the ListView, and attach this adapter to it.
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);
        mRecyclerView.setAdapter(mForecastAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        final View parallaxBar = rootView.findViewById(R.id.parallax_bar);
        if (parallaxBar != null) {
            Log.d("TAG", "onCreateView: ");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int max = parallaxBar.getHeight();
                        if (dy > 0) {
                            parallaxBar.setTranslationY(
                                    Math.max(-max, parallaxBar.getTranslationY() - dy / 2));
                        } else {
                            parallaxBar.setTranslationY(
                                    Math.min(0, parallaxBar.getTranslationY() - dy / 2));
                        }
                    }
                });
            }
        }

        final AppBarLayout appbarView = (AppBarLayout)rootView.findViewById(R.id.appbar);
        if (null != appbarView) {
            ViewCompat.setElevation(appbarView, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (0 == mRecyclerView.computeVerticalScrollOffset()) {
                            appbarView.setElevation(0);
                        } else {
                            appbarView.setElevation(appbarView.getTargetElevation());
                        }
                    }
                });
            }
        }

        long l = System.currentTimeMillis();
        if (lastUpdate + THREE_HOURS <= l) {
            updateWeather();
            Log.d("TAG", "onCreateView:1 ");
            lastUpdate = l;
        } else if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_KEY)) {
                // The Recycler View probably hasn't even been populated yet.  Actually perform the
                // swapout in onLoadFinished.
                pos = savedInstanceState.getInt(SELECTED_KEY);
            }
            mForecastAdapter.onRestoreInstanceState(savedInstanceState);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
    }

    public void onLocationOrUnitSystemChanged() {
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    @Override
      public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                0, 0);
        mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mHoldForTransition = a.getBoolean(R.styleable.ForecastFragment_sharedElementTransitions, false);
        a.recycle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecyclerView.clearOnScrollListeners();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to RecyclerView.NO_POSITION,
        // so check for that before storing.
        if (pos != RecyclerView.NO_POSITION) {
            outState.putInt(SELECTED_KEY, pos);
        }
        mForecastAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We hold for transition here just in-case the activity
        // needs to be re-created. In a standard return transition,
        // this doesn't actually make a difference.
        if (mHoldForTransition) {
            getActivity().supportPostponeEnterTransition();
        }
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    private String getGeoLocation() {
        String msg = "";
        Cursor cursor = mForecastAdapter.getCursor();
        if (cursor.moveToFirst()) {
            String latitude = cursor.getString(COL_COORD_LAT);
            String longitude = cursor.getString(COL_COORD_LONG);
            msg = String.format("geo:%s,%s", latitude, longitude);
        }
        return msg;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = Utility.getPreferredLocation(getActivity());

        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!cursor.moveToFirst()) setErrorMessage();
        mForecastAdapter.swapCursor(cursor);
        if (cursor.getCount() == 0) {
            getActivity().supportStartPostponedEnterTransition();
        } else {
            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (mRecyclerView.getChildCount() > 0) {
                        mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = mForecastAdapter.getSelectedItemPosition();
                        if (RecyclerView.NO_POSITION == itemPosition) itemPosition = 0;
                        Log.d("TAG", "onPreDraw: " + Utility.isTablet(context));
                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(itemPosition);
                        if (null != vh && Utility.isTablet(context)) {
                            Log.d("TAG", "onPreDraw: ");
                            mForecastAdapter.selectView(vh);
                        }
                        if (mHoldForTransition) {
                            getActivity().supportStartPostponedEnterTransition();
                        }
                        return true;
                    }
                    return false;
                }
            });
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setErrorMessage();
    }

    private void setErrorMessage() {
        ConnectivityManager systemService =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (systemService.getActiveNetworkInfo() == null) {
            notAvailable.setText(R.string.no_weather_available);
            return;
        }

        switch (Utility.getLocationStatusPreference(context)) {
            case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                notAvailable.setText(R.string.empty_forecast_list_server_down);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                notAvailable.setText(R.string.empty_forecast_list_server_error);
                break;
            case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                notAvailable.setText(R.string.empty_forecast_list_invalid_location);
                break;
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onItemSelected(Uri dateUri, ForecastAdapter.ViewHolder h);

    }

}
package com.example.igorklimov.sunshine.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.helpers.Utility;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter.LocationStatus;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "TAG";
    private Context context;
    private String location;
    private EditTextPreference pref;
    public final static int PLACE_PICKER_REQUEST = 9090;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);
        context = getApplicationContext();
        location = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.location_key), "");

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.unit_system_key)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check to see if the result is from our Place Picker intent
        Log.d(TAG, "onActivityResult: ");
        if (requestCode == PLACE_PICKER_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: ");
                Place place = PlacePicker.getPlace(data, this);
                String address = place.getAddress().toString();

                LatLng latLong = place.getLatLng();

                // If the provided place doesn't have an address, we'll form a display-friendly
                // string from the latlng values.
                if (TextUtils.isEmpty(address)) {
                    address = String.format("(%.2f, %.2f)", latLong.latitude, latLong.longitude);
                }

                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.location_key), address);
                // Also store the latitude and longitude so that we can use these to get a precise
                // result from our weather service. We cannot expect the weather service to
                // understand addresses that Google formats.
                editor.putFloat(context.getString(R.string.pref_location_latitude), (float)latLong.latitude);
                editor.putFloat(context.getString(R.string.pref_location_longitude), (float) latLong.longitude);
                // keys defined in strings.xml.

                editor.commit();

                // Tell the SyncAdapter that we've changed the location, so that we can update
                // our UI with new values. We need to do this manually because we are responding
                // to the PlacePicker widget result here instead of allowing the
                // LocationEditTextPreference to handle these changes and invoke our callbacks.
//                Preference locationPreference = findPreference(getString(R.string.location_key));
//                setPreferenceSummary(locationPreference, address);
                Utility.resetLocationPreference(this);
                SunshineSyncAdapter.syncImmediately(this);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(context).getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) preference.setSummary(listPreference.getEntries()[prefIndex]);
            if (!listPreference.getValue().equals(value)) SunshineSyncAdapter
                    .syncImmediately(context);
        } else {
            // For other preferences, set the summary to the value's simple location representation.
            pref = (EditTextPreference) preference;
            preference.setSummary(getSummary(stringValue));
            if (!location.equals(stringValue)) {
                // Wipe out any potential PlacePicker latlng values so that we can use this text entry.
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .edit();
                editor.remove(getString(R.string.pref_location_latitude));
                editor.remove(getString(R.string.pref_location_longitude));
                editor.commit();
                Utility.resetLocationPreference(context);
                SunshineSyncAdapter.syncImmediately(context);
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    public String getSummary(String location) {
        String summary;
        @LocationStatus
        int status = Utility.getLocationStatusPreference(context);
        if (status == SunshineSyncAdapter.LOCATION_STATUS_UNKNOWN) {
            summary = context.getString(R.string.pref_location_unknown_description);
        } else if (status == SunshineSyncAdapter.LOCATION_STATUS_INVALID) {
            summary = context.getString(R.string.pref_location_error_description, location);
        } else {
            summary = location;
        }
        return summary;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(context.getString(R.string.location_status_key))) {
            pref.setSummary(getSummary(Utility.getPreferredLocation(context)));
        } else if (key.equals(context.getString(R.string.icons_key))) {
            Log.d("TAG", "onSharedPreferenceChanged: icon style changed");
            MainActivity.sIconStyleChanged = true;
        }
    }
}

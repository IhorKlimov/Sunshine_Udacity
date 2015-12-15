package com.example.igorklimov.sunshine.activities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncAdapterType;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.helpers.Utility;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter;
import com.example.igorklimov.sunshine.sync.SunshineSyncAdapter.LocationStatus;

/**
 * A {@link PreferenceActivity} that presents a set of application settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private Context context;
    private String location;
    private EditTextPreference pref;

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
        }else if (key.equals(context.getString(R.string.icons_key))) {
            Log.d("TAG", "onSharedPreferenceChanged: icon style changed");
            MainActivity.iconStyleChanged = true;
        }
    }
}

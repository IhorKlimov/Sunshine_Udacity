package com.example.igorklimov.sunshine.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.igorklimov.sunshine.R;
import com.example.igorklimov.sunshine.activities.SettingsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.ui.PlacePicker;

/**
 * Created by Igor Klimov on 12/14/2015.
 */
public class LocationEditTextPreference extends EditTextPreference {
    private static final int DEFAULT_MINIMUM_LENGTH = 2;
    private int minLength;
    private boolean b;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.LocationEditTextPreference, 0, 0);

        try {
            minLength = array.getInt(R.styleable.LocationEditTextPreference_minimumLength,
                    DEFAULT_MINIMUM_LENGTH);
        } finally {
            array.recycle();
        }
        // Check to see if Google Play services is available. The Place Picker API is available
        // through Google Play services, so if this is false, we'll just carry on as though this
        // feature does not exist. If it is true, however, we can add a widget to our preference.
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode == ConnectionResult.SUCCESS) {
            // Add the get current location widget to our location preference
            setWidgetLayoutResource(R.layout.pref_current_location);
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        View currentLocation = view.findViewById(R.id.current_location);
        if (currentLocation != null) {
            currentLocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = getContext();
                    // Launch the Place Picker so that the user can specify their location, and then
                    // return the result to SettingsActivity.
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    // We are in a view right now, not an activity. So we need to get ourselves
                    // an activity that we can use to start our Place Picker intent. By using
                    // SettingsActivity in this way, we can ensure the result of the Place Picker
                    // intent comes to the right place for us to process it.
                    SettingsActivity settingsActivity = (SettingsActivity) context;
                    try {
                        settingsActivity.startActivityForResult(builder.build(settingsActivity),
                                SettingsActivity.PLACE_PICKER_REQUEST);
                    } catch (GooglePlayServicesNotAvailableException
                            | GooglePlayServicesRepairableException e) {
                        //NOP
                    }
                }
            });
        }

        return view;
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog d = (AlertDialog) getDialog();
        final Button b = d.getButton(Dialog.BUTTON_POSITIVE);

        getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() < minLength && b.isEnabled()) {
                    b.setEnabled(false);
                } else if (s.length() >= minLength && !b.isEnabled()) {
                    b.setEnabled(true);
                }
            }
        });
    }
}

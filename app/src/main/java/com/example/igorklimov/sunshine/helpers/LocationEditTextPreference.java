package com.example.igorklimov.sunshine.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

import com.example.igorklimov.sunshine.R;

/**
 * Created by Igor Klimov on 12/14/2015.
 */
public class LocationEditTextPreference extends EditTextPreference {
    private static final int DEFAULT_MINIMUM_LENGTH = 2;
    private int minLength;

    public LocationEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.LocationEditTextPreference, 0, 0
        );

        try {
            minLength = array.getInt(R.styleable.LocationEditTextPreference_minimumLength,
                    DEFAULT_MINIMUM_LENGTH);
        } finally {
            array.recycle();
        }
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

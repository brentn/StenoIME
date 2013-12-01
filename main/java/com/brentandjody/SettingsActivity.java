package com.brentandjody;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by brent on 01/12/13.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

}

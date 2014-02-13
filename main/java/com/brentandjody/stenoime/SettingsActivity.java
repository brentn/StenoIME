package com.brentandjody.stenoime;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Created by brent on 01/12/13.
 * This is my main settings activity - which also handles purchase activity results
 */
public class SettingsActivity extends PreferenceActivity {

    private StenoApp App;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App = ((StenoApp) getApplication());
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!App.handlePurchaseResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}

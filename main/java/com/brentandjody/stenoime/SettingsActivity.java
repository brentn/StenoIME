package com.brentandjody.stenoime;

import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;

import com.brentandjody.stenoime.Translator.Translator;
import com.brentandjody.stenoime.util.IabHelper;
import com.brentandjody.stenoime.util.IabResult;
import com.brentandjody.stenoime.util.Purchase;


/**
 * Created by brent on 01/12/13.
 * This is my main settings activity - which also handles purchase activity results
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "StenoIME";
    private static final int SELECT_DICTIONARY_CODE = 4;
    private static final int PURCHASE_REQUEST_CODE = 20201;
    private static final String PAYLOAD = "jOOnnqldcn20p843nKK;nNl";


    private StenoApp App;
    private SwitchPreference keyboardSwitch;
    private IabHelper iabHelper;
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App = ((StenoApp) getApplication());
        iabHelper = App.getIabHelper();
        setupPurchaseListener();
        addPreferencesFromResource(R.xml.preferences);
        initializeControls();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: request:"+requestCode+" result:"+resultCode);
        if (!App.handlePurchaseResult(requestCode, resultCode, data)) {
            switch (requestCode) {
                case SELECT_DICTIONARY_CODE : {
                    Log.d(TAG, "Dictionaries selected");
                    Preference dict_button = findPreference(StenoApp.KEY_DICTIONARY);
                    dict_button.setSummary(getDictionaryList());
                    break;
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initiatePurchase(String sku) {
        iabHelper.launchPurchaseFlow(this, sku, PURCHASE_REQUEST_CODE, mPurchaseFinishedListner, PAYLOAD);
    }

    private void initializeControls() {
        // about button
        Preference about = findPreference(StenoApp.KEY_ABOUT);
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
                return false;
            }
        });
        // set translator options
        ListPreference translator = (ListPreference) findPreference(StenoApp.KEY_TRANSLATOR_TYPE);
        translator.setSummary(translator.getEntry());
        translator.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference translator = (ListPreference) preference;
                translator.setValue(newValue.toString());
                translator.setSummary(translator.getEntry());
                Translator.TYPE tType = Translator.TYPE.values()[Integer.parseInt(newValue.toString())];
                App.setTranslatorType(tType);
                return false;
            }
        });
        SwitchPreference optimizer = (SwitchPreference) findPreference(StenoApp.KEY_OPTIMIZER_ENABLED);
        optimizer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean setting = ((SwitchPreference) preference).isChecked();
                App.setOptimizerEnabled(setting);
                return true;
            }
        });
        // list dictionaries
        Preference dict_button = findPreference(StenoApp.KEY_DICTIONARY);
        dict_button.setSummary(getDictionaryList());
        dict_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(SettingsActivity.this, SelectDictionaryActivity.class);
                startActivityForResult(intent, SELECT_DICTIONARY_CODE);
                return false;
            }
        });
        // hardware switches
        keyboardSwitch = (SwitchPreference) findPreference(StenoApp.KEY_NKRO_ENABLED);
        assert keyboardSwitch != null;
        keyboardSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (App.isNkroKeyboardPurchased()) {
                    return true;
                } else {
                    ((SwitchPreference) preference).setChecked(false);
                    initiatePurchase( StenoApp.SKU_NKRO_KEYBOARD);
                    return false;
                }
            }
        });

    }

    private String getDictionaryList() {
        StringBuilder dictionaryList = new StringBuilder();
        String[] dictionaries = App.getDictionaryNames();
        if (dictionaries.length>0) {
            for (String d : dictionaries) {
                if (d.contains("/")) {
                    dictionaryList.append(" - ").append(d.substring(d.lastIndexOf("/")+1)).append("\n");
                } else {
                    dictionaryList.append(" - ").append(d).append("\n");
                }
            }
        } else {
            dictionaryList.append("Built-in Dictionary");
        }
        return dictionaryList.toString();
    }



    private void setupPurchaseListener() {
        mPurchaseFinishedListner = new IabHelper.OnIabPurchaseFinishedListener() {
            public void onIabPurchaseFinished(IabResult result, Purchase purchase)
            {
                if (result.isFailure()) {
                    Log.d(TAG, "Error purchasing: " + result);
                    return;
                }
                if (purchase.getSku().equals(StenoApp.SKU_NKRO_KEYBOARD)) {
                    if (purchase.getDeveloperPayload().equals(PAYLOAD)) {
                        Log.d(TAG, "NKRO Keyboard purchased");
                        App.setNKROPurchased(true);
                        //prefs.edit().putBoolean(KEY_NKRO_ENABLED, true).commit();
                        keyboardSwitch.setChecked(true);
                    }
                }
            }
        };
    }

}

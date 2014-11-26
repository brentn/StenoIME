package com.brentandjody.stenoime;

import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.ListView;

import com.brentandjody.stenoime.Translator.Translator;
import com.brentandjody.stenoime.data.DBContract;
import com.brentandjody.stenoime.data.OptimizerTableHelper;
import com.brentandjody.stenoime.util.IabHelper;
import com.brentandjody.stenoime.util.IabResult;
import com.brentandjody.stenoime.util.Purchase;


/**
 * Created by brent on 01/12/13.
 * This is my main settings activity - which also handles purchase activity results
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final int SELECT_DICTIONARY_CODE = 4;
    private static final int PURCHASE_REQUEST_CODE = 20201;
    private static final String PAYLOAD = "jOOnnqldcn20p843nKK;nNl";

    private StenoApp App;
    private CheckBoxPreference keyboardSwitch;
    private IabHelper iabHelper;

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App = ((StenoApp) getApplication());
        iabHelper = App.getIabHelper();
        verifyEnabled();
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
                    Preference dict_button = findPreference(getString(R.string.pref_dictionary_button));
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

    private void verifyEnabled() {
        InputMethodManager imeManager = (InputMethodManager)getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
        for (InputMethodInfo i : imeManager.getEnabledInputMethodList()) {
            if (i.getPackageName().equals(getApplication().getPackageName())) return;
        }
        Log.d(TAG, "Steno Keyboard is not enabled");
        startActivity(new Intent(this, SetupActivity.class));
    }

    private void initializeControls() {
        // tutorial button
        Preference btn_tutorial = findPreference(getString(R.string.key_tutorial_button));
        btn_tutorial.setIcon(R.drawable.ic_tutorial);
        btn_tutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, TutorialActivity.class));
                return false;
            }
        });

        // optimizer button
        SQLiteDatabase db = new OptimizerTableHelper(this).getReadableDatabase();
        boolean has_entries = DatabaseUtils.queryNumEntries(db, DBContract.OptimizationEntry.TABLE_NAME)>0;
        Preference btn_optimizer = findPreference(getString(R.string.key_optimizer_button));
        btn_optimizer.setIcon(R.drawable.ic_list);
        btn_optimizer.setEnabled(has_entries);
        btn_optimizer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SettingsActivity.this, SuggestionsActivity.class));
                return false;
            }
        });

        // set translator options
        CheckBoxPreference zoom = (CheckBoxPreference) findPreference(getString(R.string.pref_zoom_enabled));

        CheckBoxPreference performance = (CheckBoxPreference) findPreference(getString(R.string.key_show_perf_notifications));
        performance.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean setting = ((CheckBoxPreference) preference).isChecked();
                ((CheckBoxPreference) preference).setChecked(setting);
                App.setShowPerfNotifications(setting);
                Log.d(TAG, "Setting performance perefernce;"+setting);
                return true;
            }
        });

        ListPreference translator = (ListPreference) findPreference(getString(R.string.pref_translator));
        translator.setSummary(translator.getEntry());
        translator.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference translator = (ListPreference) preference;
                int value = Integer.parseInt(newValue.toString());
                translator.setSummary(translator.getEntries()[value]);
                Translator.TYPE tType = Translator.TYPE.values()[value];
                App.setTranslatorType(tType);
                Log.d(TAG, "Setting translator type:"+tType);
                findPreference(getResources().getString(R.string.pref_optimizer_enabled)).setEnabled(!newValue.equals("0"));
                return true;
            }
        });
//        PreferenceCategory translator_category = (PreferenceCategory) findPreference(getString("pref_cat_translator"));
        CheckBoxPreference optimizer = (CheckBoxPreference) findPreference(getResources().getString(R.string.pref_optimizer_enabled));
        optimizer.setEnabled(!(translator.getValue().equals("0")));
        optimizer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean setting = ((CheckBoxPreference) preference).isChecked();
                App.setOptimizerEnabled(setting);
                return true;
            }
        });
        // list dictionaries
        Preference dict_button = findPreference(getString(R.string.pref_dictionary_button));
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
        keyboardSwitch = (CheckBoxPreference) findPreference(getString(R.string.pref_kbd_enabled));
        assert keyboardSwitch != null;
        keyboardSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (App.isNkroKeyboardPurchased()) {
                    return true;
                } else {
                    ((CheckBoxPreference) preference).setChecked(false);
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

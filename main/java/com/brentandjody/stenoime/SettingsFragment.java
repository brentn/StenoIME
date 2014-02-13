package com.brentandjody.stenoime;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.brentandjody.stenoime.Translator.Translator;

public class SettingsFragment extends PreferenceFragment {

    private static final int SELECT_DICTIONARY_CODE = 4;
    private StenoApp App;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App = ((StenoApp) getActivity().getApplication());
        addPreferencesFromResource(R.xml.preferences);
        // about button
        Preference about = findPreference(StenoApp.KEY_ABOUT);
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return false;
            }
        });
        // set translator options
        ListPreference translator = (ListPreference) findPreference(StenoApp.KEY_TRANSLATOR_TYPE);
        final Preference suffixes = findPreference(StenoApp.KEY_SUFFIX_CORRECTION);
        translator.setSummary(translator.getEntry());
        translator.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference translator = (ListPreference) preference;
                translator.setValue(newValue.toString());
                translator.setSummary(translator.getEntry());
                Translator.TYPE tType = Translator.TYPE.values()[Integer.parseInt(newValue.toString())];
                ((StenoApp) getActivity().getApplication()).setTranslatorType(tType);
                suffixes.setEnabled(tType == Translator.TYPE.SimpleDictionary);
                return false;
            }
        });
        // list dictionaries
        Preference dict_button = findPreference(StenoApp.KEY_DICTIONARY);
        dict_button.setSummary(getDictionaryList());
        dict_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SelectDictionaryActivity.class);
                startActivityForResult(intent, SELECT_DICTIONARY_CODE);
                return false;
            }
        });
        // hardware switches
        SwitchPreference keyboardSwitch = (SwitchPreference) findPreference(StenoApp.KEY_NKRO_ENABLED);
        keyboardSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (App.isNkroKeyboardPurchased())
                    return true;
                else
                ((SwitchPreference) preference).setChecked(false);
                App.initiatePurchase(getActivity(), App.SKU_NKRO_KEYBOARD);
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode== Activity.RESULT_OK) {
            switch (requestCode) {
                case SELECT_DICTIONARY_CODE : {
                    Preference dict_button = findPreference(StenoApp.KEY_DICTIONARY);
                    dict_button.setSummary(getDictionaryList());
                    break;
                }
            }
        }

    }

    private String getDictionaryList() {
        StringBuilder dictionaryList = new StringBuilder();
        String[] dictionaries = ((StenoApp)getActivity().getApplication()).getDictionaryNames();
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





}
package com.brentandjody.stenoime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.brentandjody.stenoime.Translator.Translator;

public class SettingsFragment extends PreferenceFragment {

    private static final int SELECT_DICTIONARY_CODE = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        // set translator text
        ListPreference translator = (ListPreference) findPreference("pref_translator");
        translator.setSummary(translator.getEntry());
        translator.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference translator = (ListPreference) preference;
                translator.setValue(newValue.toString());
                translator.setSummary(translator.getEntry());
                int val = Integer.parseInt(newValue.toString());
                ((StenoApp) getActivity().getApplication()).setTranslatorType(Translator.TYPE.values()[val]);
                return false;
            }
        });
        // list dictionaries
        Preference dict_button = findPreference("pref_dictionary_button");
        dict_button.setSummary(getDictionaryList());
        dict_button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), SelectDictionaryActivity.class);
                startActivityForResult(intent, SELECT_DICTIONARY_CODE);
                return false;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==SELECT_DICTIONARY_CODE) {
            Preference dict_button = findPreference("pref_dictionary_button");
            dict_button.setSummary(getDictionaryList());
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
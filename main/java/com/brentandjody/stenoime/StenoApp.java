package com.brentandjody.stenoime;

import android.app.Application;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.widget.ProgressBar;

import com.brentandjody.stenoime.Input.StenoMachine;
import com.brentandjody.stenoime.Translator.Dictionary;
import com.brentandjody.stenoime.Translator.Translator;

/**
 * Created by brent on 30/11/13.
 * Hold some state, such as the loaded dictionary
 * and other various settings
 */
public class StenoApp extends Application {

    public static final String DELIMITER = ":";
    public static final String KEY_DICTIONARIES = "dictionaries";
    public static final String KEY_DICTIONARY_SIZE = "dictionary_size";
    public static final String KEY_MACHINE_TYPE = "default_machine_type";
    public static final String KEY_TRANSLATOR_TYPE = "selected_translator_type";

    private Dictionary mDictionary;
    private StenoMachine mInputDevice;
    private UsbDevice mUsbDevice;
    private Translator.TYPE mTranslatorType;
    private StenoMachine.TYPE mMachineType;
    private SharedPreferences prefs;
    private ProgressBar mProgressBar;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mDictionary = new Dictionary(getApplicationContext());
        mProgressBar = new ProgressBar(getApplicationContext());
        mInputDevice =null;
        mTranslatorType = Translator.TYPE.values()[prefs.getInt(StenoApp.KEY_TRANSLATOR_TYPE, 1)];//TODO:change default ot 0
        mMachineType = StenoMachine.TYPE.values()[prefs.getInt(KEY_MACHINE_TYPE, 0)]; //default is virtual
    }

    // Setters
    public void setInputDevice(StenoMachine sm) {
        mInputDevice = sm;}
    public void setUsbDevice(UsbDevice ud) { mUsbDevice = ud; }
    public void setProgressBar(ProgressBar pb) { mProgressBar = pb; }
    public void setMachineType(StenoMachine.TYPE t) { mMachineType = t; }
    public void setTranslatorType(Translator.TYPE t) { mTranslatorType = t; }

    // Getters
    public StenoMachine getInputDevice() {return mInputDevice; }
    public UsbDevice getUsbDevice() { return mUsbDevice; }
    public StenoMachine.TYPE getMachineType() { return mMachineType; }
    public Translator.TYPE getTranslatorType() { return mTranslatorType; }

    public Dictionary getDictionary(Dictionary.OnDictionaryLoadedListener listener) {
        // if dictionary is empty, load it - otherwise just return it
        // if listener is null, don't reset it (use last registered listener)
        if ((!isDictionaryLoaded()) && (!mDictionary.isLoading()) ) {
            if (listener!= null)
                mDictionary.setOnDictionaryLoadedListener(listener);
            int size = prefs.getInt(KEY_DICTIONARY_SIZE, 100000);
            mProgressBar.setProgress(0);
            String[] dictionaries = getDictionaryNames();
            if (dictionaries != null) {
                mDictionary.load(getDictionaryNames(), mProgressBar, size);
            }
        }
        return mDictionary;
    }

    public void unloadDictionary() {
        mDictionary = new Dictionary(getApplicationContext());
    }

    public String[] getDictionaryNames() {
        String data = prefs.getString(KEY_DICTIONARIES, "");
        if (data.isEmpty()) {
            return null;
        }
        return data.split(DELIMITER);
    }

    public boolean isDictionaryLoaded() { return (mDictionary.size() > 10); }
}

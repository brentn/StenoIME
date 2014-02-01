package com.brentandjody.stenoime;

import android.app.Application;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.widget.ProgressBar;

import com.brentandjody.stenoime.Input.StenoMachine;
import com.brentandjody.stenoime.Translator.Dictionary;
import com.brentandjody.stenoime.Translator.Translator;
import com.brentandjody.stenoime.util.IabHelper;
import com.brentandjody.stenoime.util.IabResult;
import com.brentandjody.stenoime.util.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brent on 30/11/13.
 * Hold some state, such as the loaded dictionary
 * and other various settings
 */
public class StenoApp extends Application {

    public static final String DELIMITER = ":";
    public static final String TAG = "Steno Keyboard";

    public static final String KEY_DICTIONARIES = "dictionaries";
    public static final String KEY_DICTIONARY_SIZE = "dictionary_size";
    public static final String KEY_MACHINE_TYPE = "default_machine_type";
    public static final String KEY_TRANSLATOR_TYPE = "pref_translator";
    public static final String KEY_NKRO_ENABLED = "pref_kbd_enabled";

    private static final String PUBLICKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnyCtAAdSMc6ErV+EaMzTesLJSStqYq9cKBf4e8Cy9byfTIaclMK49SU/3+cPsXPX3LoVvmNitfWx4Cd5pUEIad3SEkYRWxGlfwdh4CGY2Cxy7bQEw/y+vIvHX5qXvPljcs6LtoJn9Ui01LTtEQ130rg6p61VuA4+MAuNZS2ReHf4IB7pqnNpMYQbWghpEN+rIrGnfTj2Bz/lZzNqmM+BHir4WH4Uu9zKExlxN+fe2CaKWTLMCi+xhwvZpjm2IgRWQ02wdf2aVezDSDPg7Ze/yKU/3aCWpzdMtBuheWJCf7tS1QjF8XCBi70iVngb20EPAkfnOjkP7F7y08Gg3AF9OQIDAQAB";
    private static final String SKU_NKRO_KEYBOARD = "nkro_keyboard_connection";

    private Dictionary mDictionary;
    private StenoMachine mInputDevice = null;
    private UsbDevice mUsbDevice;
    private Translator.TYPE mTranslatorType;
    private StenoMachine.TYPE mMachineType;
    private SharedPreferences prefs;
    private ProgressBar mProgressBar = null;
    private IabHelper iabHelper;
    private IabHelper.QueryInventoryFinishedListener mQueryFinishedListener;
    private String nkroPrice;
    private boolean nkro_enabled = false;
    private boolean txbolt_enabled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        mDictionary = new Dictionary(getApplicationContext());
        nkro_enabled = prefs.getBoolean(KEY_NKRO_ENABLED, false);
        int val = Integer.parseInt(prefs.getString(StenoApp.KEY_TRANSLATOR_TYPE, "1"));
        mTranslatorType = Translator.TYPE.values()[val];
        mMachineType = StenoMachine.TYPE.VIRTUAL;
        iabHelper = new IabHelper(this, PUBLICKEY);
        setupBillingListener();
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d(TAG, "Problem setting up In-app Billing: " + result);
                }
                // Success! Query purchased items
                List additionalSkuList = new ArrayList();
                additionalSkuList.add(SKU_NKRO_KEYBOARD);
                iabHelper.queryInventoryAsync(true, additionalSkuList, mQueryFinishedListener);
            }
        });

    }

    @Override
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.unregisterActivityLifecycleCallbacks(callback);
        if (iabHelper != null) iabHelper.dispose();
        iabHelper = null;
    }

    // Setters
    public void setInputDevice(StenoMachine sm) {
        mInputDevice = sm;}
    public void setUsbDevice(UsbDevice ud) { mUsbDevice = ud; }
    public void setProgressBar(ProgressBar pb) { mProgressBar = pb; }
    public void setMachineType(StenoMachine.TYPE t) {
        nkro_enabled = prefs.getBoolean(KEY_NKRO_ENABLED, false);
        switch (t) {
            case VIRTUAL: mMachineType = t;
                break;
            case KEYBOARD: if (nkro_enabled) mMachineType = t;
                break;
            case TXBOLT: if (txbolt_enabled) mMachineType = t;
        }
        if (mMachineType==null) mMachineType= StenoMachine.TYPE.VIRTUAL;
    }
    public void setTranslatorType(Translator.TYPE t) { mTranslatorType = t; }

    // Getters
    public StenoMachine getInputDevice() {return mInputDevice; }
    public UsbDevice getUsbDevice() { return mUsbDevice; }
    public StenoMachine.TYPE getMachineType() { return mMachineType; }
    public Translator.TYPE getTranslatorType() { return mTranslatorType; }
    public boolean useWordList() { return prefs.getBoolean("pref_suffix_correction", false); }
    public boolean isNkro_enabled() {
        nkro_enabled = prefs.getBoolean(KEY_NKRO_ENABLED, false);
        return nkro_enabled;
    }

    public Dictionary getDictionary(Dictionary.OnDictionaryLoadedListener listener) {
        // if dictionary is empty, load it - otherwise just return it
        // if listener is null, don't reset it (use last registered listener)
        if ((!isDictionaryLoaded()) && (!mDictionary.isLoading()) ) {
            if (listener!= null)
                mDictionary.setOnDictionaryLoadedListener(listener);
            int size = prefs.getInt(KEY_DICTIONARY_SIZE, 100000);
            mProgressBar.setProgress(0);
            mDictionary.load(getDictionaryNames(), getAssets(), mProgressBar, size);

        }
        return mDictionary;
    }

    public void unloadDictionary() {
        mDictionary = new Dictionary(getApplicationContext());
    }

    public String[] getDictionaryNames() {
        String data = prefs.getString(KEY_DICTIONARIES, "");
        if (data.isEmpty()) {
            return new String[0];
        }
        return data.split(DELIMITER);
    }

    public boolean isDictionaryLoaded() { return (mDictionary.size() > 10); }

    private void setupBillingListener() {
        mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (result.isFailure()) {
                    Log.e(TAG, "Unable to connect to in-app-billing server");
                    return;
                }
                nkroPrice = inventory.getSkuDetails(SKU_NKRO_KEYBOARD).getPrice();
                // update the UI
            }
        };
    }
}

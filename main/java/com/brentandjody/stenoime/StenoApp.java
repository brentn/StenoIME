package com.brentandjody.stenoime;

import android.app.Application;
import android.content.Intent;
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
    public static final String TAG = StenoApp.class.getSimpleName();

    private static final String PUBLICKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnyCtAAdSMc6ErV+EaMzTesLJSStqYq9cKBf4e8Cy9byfTIaclMK49SU/3+cPsXPX3LoVvmNitfWx4Cd5pUEIad3SEkYRWxGlfwdh4CGY2Cxy7bQEw/y+vIvHX5qXvPljcs6LtoJn9Ui01LTtEQ130rg6p61VuA4+MAuNZS2ReHf4IB7pqnNpMYQbWghpEN+rIrGnfTj2Bz/lZzNqmM+BHir4WH4Uu9zKExlxN+fe2CaKWTLMCi+xhwvZpjm2IgRWQ02wdf2aVezDSDPg7Ze/yKU/3aCWpzdMtBuheWJCf7tS1QjF8XCBi70iVngb20EPAkfnOjkP7F7y08Gg3AF9OQIDAQAB";
    public static final String SKU_NKRO_KEYBOARD = "nkro_keyboard_connection";
    private static boolean NKRO_KEYBOARD_PURCHASED = false;

    private Dictionary mDictionary;
    private StenoMachine mInputDevice = null;
    private UsbDevice mUsbDevice;
    private Translator.TYPE mTranslatorType;
    private StenoMachine.TYPE mMachineType;
    private SharedPreferences prefs;
    private ProgressBar mProgressBar = null;
    private IabHelper iabHelper;
    private IabHelper.QueryInventoryFinishedListener mQueryFinishedListener;
    private boolean nkro_enabled = false;
    private boolean txbolt_enabled = false;
    private boolean optimizer_enabled = false;
    private boolean show_perf_notifications = false;

    private static final boolean NO_PURCHASES_NECESSARY=true;
    private static final boolean RESET_PURCHASES_FOR_TESTING =false;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        mDictionary = new Dictionary(getApplicationContext());
        nkro_enabled = prefs.getBoolean(getString(R.string.pref_kbd_enabled), false);
        optimizer_enabled = prefs.getBoolean(getString(R.string.pref_optimizer_enabled), false);
        show_perf_notifications = prefs.getBoolean(getString(R.string.key_show_perf_notifications), false);
        int val = Integer.parseInt(prefs.getString(getString(R.string.pref_translator), "1"));
        mTranslatorType = Translator.TYPE.values()[val];
        mMachineType = StenoMachine.TYPE.VIRTUAL;
        iabHelper = new IabHelper(this, PUBLICKEY);
        setupBillingListeners();
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
        nkro_enabled = prefs.getBoolean(getString(R.string.pref_kbd_enabled), false);
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
    public void setNKROPurchased(boolean purchased) {
        NKRO_KEYBOARD_PURCHASED = purchased;
    }
    public void setOptimizerEnabled(boolean setting) {optimizer_enabled = setting; }

    // Getters
    public StenoMachine getInputDevice() {return mInputDevice; }
    public UsbDevice getUsbDevice() { return mUsbDevice; }
    public StenoMachine.TYPE getMachineType() { return mMachineType; }
    public Translator.TYPE getTranslatorType() { return mTranslatorType; }
    public ProgressBar getProgressBar() {return mProgressBar; }
    public boolean showPerformanceNotifications() {return show_perf_notifications;}
    public boolean isNkroKeyboardPurchased() { return NKRO_KEYBOARD_PURCHASED || NO_PURCHASES_NECESSARY; }
    public boolean isNkro_enabled() {
        if (! (NKRO_KEYBOARD_PURCHASED || NO_PURCHASES_NECESSARY)) return false;
        nkro_enabled = prefs.getBoolean(getString(R.string.pref_kbd_enabled), false);
        return nkro_enabled;
    }
    public boolean isOptimizerEnabled() {return optimizer_enabled;}
    public IabHelper getIabHelper() {return iabHelper;}

    public Dictionary getDictionary(Dictionary.OnDictionaryLoadedListener listener) {
        // if dictionary is empty, load it - otherwise just return it
        // if listener is null, don't reset it (use last registered listener)
        if ((!isDictionaryLoaded()) && (!mDictionary.isLoading()) ) {
            int size = prefs.getInt(getString(R.string.key_dictionary_size), 100000);
            mDictionary.load(getDictionaryNames(), getAssets(), size);
        }
        if (listener != null) {
            mDictionary.setOnDictionaryLoadedListener(listener);
        } else {
            Log.w(TAG, "Dictionary callback is null");
        }

        return mDictionary;
    }

    public void unloadDictionary() {
        if (mDictionary!=null) {
            Log.d(TAG, "Unloading Dictionary");
            mDictionary.clear();
            mDictionary = new Dictionary(getApplicationContext());
        }
    }

    public String[] getDictionaryNames() {
        String data = prefs.getString(getString(R.string.key_dictionaries), "");
        if (data.isEmpty()) {
            return new String[0];
        }
        return data.split(DELIMITER);
    }

    public boolean isDictionaryLoaded() {
        return (mDictionary!=null && (!mDictionary.isLoading()) && mDictionary.size() > 10);
    }

    public boolean handlePurchaseResult(int requestCode, int resultCode, Intent data) {
        return iabHelper.handleActivityResult(requestCode, resultCode, data);
    }

    private void setupBillingListeners() {
        mQueryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                if (result.isFailure()) {
                    Log.e(TAG, "Unable to connect to in-app-billing server");
                    return;
                }
                NKRO_KEYBOARD_PURCHASED = inventory.hasPurchase(SKU_NKRO_KEYBOARD);

                //*************************
                if (RESET_PURCHASES_FOR_TESTING) {
                    iabHelper.consumeAsync(inventory.getPurchase(SKU_NKRO_KEYBOARD), null);
                    NKRO_KEYBOARD_PURCHASED = false;
                    Log.w(TAG, "TESTING MODE SET: Purchases removed from account");
                }
                //*************************

                if (NKRO_KEYBOARD_PURCHASED || NO_PURCHASES_NECESSARY) {
                    Log.d(TAG, "NKRO Keyboard is in inventory");
                } else {
                    Log.d(TAG, "NKRO Keyboard is NOT in inventory");
                    prefs.edit().putBoolean(getString(R.string.pref_kbd_enabled), false).commit();
                }
            }
        };

    }
}

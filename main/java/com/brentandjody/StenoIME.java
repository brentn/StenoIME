package com.brentandjody;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.brentandjody.Keyboard.NKeyRolloverMachine;
import com.brentandjody.Keyboard.StenoMachine;
import com.brentandjody.Keyboard.TouchLayer;
import com.brentandjody.Translator.Dictionary;
import com.brentandjody.Translator.RawStrokeTranslator;
import com.brentandjody.Translator.Stroke;
import com.brentandjody.Translator.Translator;

import java.util.Set;

/**
 * Created by brent on 30/11/13.
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeCompleteListener, StenoMachine.OnStrokeListener {

    private static final String TAG = "StenoIME";
    private static final String ACTION_USB_PERMISSION = "com.brentandjody.USB_PERMISSION";
    private static final String MACHINE_TYPE = "current_machine_type";
    private static final String TRANSLATOR_TYPE = "selected_translator_type";

    private StenoApplication App;
    private SharedPreferences prefs;
    private StenoMachine.TYPE mMachineType;
    private Dictionary mDictionary;
    private LinearLayout mKeyboard;
    private Translator mTranslator;
    private PendingIntent mPermissionIntent;
    private View overlay;

    @Override
    public void onCreate() {
        super.onCreate();
        App = ((StenoApplication) getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDictionary = App.getDictionary();
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        initializeTranslator(Translator.TYPE.values()[prefs.getInt(TRANSLATOR_TYPE, 0)]);
    }

    @Override
    public View onCreateInputView() {
        mMachineType = StenoMachine.TYPE.values()[prefs.getInt(MACHINE_TYPE, 0)];
        mKeyboard = new LinearLayout(this);
        if (mMachineType == StenoMachine.TYPE.VIRTUAL) {
            mKeyboard.addView(getLayoutInflater().inflate(R.layout.keyboard, null));
             launchVirtualKeyboard();
        } else {
            removeVirtualKeyboard();
        }
        return mKeyboard;
    }

    @Override
    public View onCreateCandidatesView() {
        View view = getLayoutInflater().inflate(R.layout.preview, null);
        overlay = view.findViewById(R.id.preview_overlay);
        return view;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
        //TODO: STUB
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            setMachineType(StenoMachine.TYPE.KEYBOARD);
        }
        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            setMachineType(StenoMachine.TYPE.VIRTUAL);
        }
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbReceiver, filter);

    }

    @Override
    public void onBindInput() {
        super.onBindInput();
        setCandidatesViewShown(true);
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        //TODO: STUB
    }

    @Override
    public void onUnbindInput() {
        super.onUnbindInput();
        setCandidatesViewShown(false);
        //TODO: STUB
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((ViewGroup) mKeyboard.getParent()).removeAllViews();
        unregisterReceiver(mUsbReceiver);
        mKeyboard=null;
        //TODO: STUB
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return dispatchKeyEvent(event);
    }

    // Implemented Interfaces
    @Override
    public void onStrokeComplete(Stroke stroke) {
        processStroke(stroke);
    }

    @Override
    public void onStroke(Set<String> keys) {
        Stroke stroke = new Stroke(keys);
        processStroke(stroke);
    }

    // Private methods

    private boolean dispatchKeyEvent(KeyEvent event) {
        StenoMachine inputDevice = App.getInputDevice();
        if (inputDevice instanceof NKeyRolloverMachine) {
            ((NKeyRolloverMachine) inputDevice).handleKeys(event);
        }
        return true;
    }

    private void setMachineType(StenoMachine.TYPE t) {
        if (t==null) t= StenoMachine.TYPE.VIRTUAL;
        if (mMachineType==t) return; //short circuit
        mMachineType = t;
        saveIntPreference(MACHINE_TYPE, mMachineType.ordinal());
        switch (mMachineType) {
            case VIRTUAL:
                App.setInputDevice(null);
                if (mKeyboard!=null) launchVirtualKeyboard();
                break;
            case KEYBOARD:
                Toast.makeText(this,"Physical Keyboard Detected",Toast.LENGTH_SHORT).show();
                if (mKeyboard!=null) removeVirtualKeyboard();
                registerMachine(new NKeyRolloverMachine());
                break;
            case TXBOLT:
                Toast.makeText(this,"TX-Bolt Machine Detected",Toast.LENGTH_SHORT).show();
                //TODO:
                if (mKeyboard!=null) removeVirtualKeyboard();
                break;
        }
    }

    private void initializeTranslator(Translator.TYPE t) {
        switch (t) {
            case STROKES: mTranslator = new RawStrokeTranslator(); break;
            //TODO:
            //case DICTIONARY: mTranslator = new DictionaryTranslator(); break;
        }
    }

    private void processStroke(Stroke stroke) {
        sendText(mTranslator.translate(stroke));
    }

    private void registerMachine(StenoMachine machine) {
        App.setInputDevice(machine);
        App.getInputDevice().setOnStrokeListener(this);
    }

    private void sendText(String message) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return; //short circuit
        // deals with backspaces
        if (message.contains("\b")) {
            // deal with any backspaces at the start first
            int i = 0;
            while (i < message.length() && message.charAt(i)=='\b')
                i++;
            if (i > 0) {
                connection.deleteSurroundingText(i,0);
                message = message.substring(i);
            }
            // split the text on the first backspace, and recurse
            if (message.contains("\b")) {
                i = message.indexOf('\b');
                sendText(message.substring(0,i));
                sendText(message.substring(i));
            }
        } else {
            connection.commitText(message, 1);
        }
    }

    private void launchVirtualKeyboard() {
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        keyboard.setOnStrokeCompleteListener(this);
        keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_in));
        mKeyboard.setVisibility(View.VISIBLE);
        mKeyboard.invalidate();
    }

    private void removeVirtualKeyboard() {
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_out));
        mKeyboard.invalidate();
        mKeyboard.setVisibility(View.GONE);
     }

    private void saveIntPreference(String name, int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(name, value);
        editor.commit();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "mUSBReceiver: received detached event");
                setMachineType(StenoMachine.TYPE.VIRTUAL);
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "mUSBReceiver: received attach event");
                setMachineType(StenoMachine.TYPE.TXBOLT);
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            ((UsbManager) getSystemService(Context.USB_SERVICE)).requestPermission(device, mPermissionIntent);
                            //TODO: (also add stuff to known devices list)
                            // setMachineType(StenoMachine.TYPE.TXBOLT);
                            // registerMachine(new TXBoltMachine(device, null));
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };

}

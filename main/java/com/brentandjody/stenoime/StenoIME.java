package com.brentandjody.stenoime;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.brentandjody.stenoime.Input.NKeyRolloverMachine;
import com.brentandjody.stenoime.Input.StenoMachine;
import com.brentandjody.stenoime.Input.TXBoltMachine;
import com.brentandjody.stenoime.Input.TouchLayer;
import com.brentandjody.stenoime.Translator.Dictionary;
import com.brentandjody.stenoime.Translator.RawStrokeTranslator;
import com.brentandjody.stenoime.Translator.SimpleTranslator;
import com.brentandjody.stenoime.Translator.Stroke;
import com.brentandjody.stenoime.Translator.TranslationResult;
import com.brentandjody.stenoime.Translator.Translator;

import java.util.Set;

/**
 * Created by brent on 30/11/13.
 * Replacement Keyboard
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeListener,
        StenoMachine.OnStrokeListener, Dictionary.OnDictionaryLoadedListener {

    private static final String TAG = "StenoIME";
    private static final String ACTION_USB_PERMISSION = "com.brentandjody.USB_PERMISSION";
    private static final boolean INLINE_PREVIEW = true;

    private StenoApp App;
    private SharedPreferences prefs;
    private LinearLayout mKeyboard;
    private Translator mTranslator;
    private PendingIntent mPermissionIntent;
    private TextView preview;
    private TextView debug;
    private LinearLayout preview_overlay;
    private int preview_length = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        App = ((StenoApp) getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(mUsbReceiver, filter); //listen for plugged/unplugged events
    }

    @Override
    public View onCreateInputView() {
        mKeyboard = new LinearLayout(this);
        mKeyboard.addView(getLayoutInflater().inflate(R.layout.keyboard, null));
        ((Button) mKeyboard.findViewById(R.id.settings_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectDictionaries();
            }
        });
        return mKeyboard;
    }

    @Override
    public View onCreateCandidatesView() {
        View view = getLayoutInflater().inflate(R.layout.preview, null);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendText(mTranslator.submitQueue());
            }
        });
        preview_overlay = (LinearLayout) view.findViewById(R.id.preview_overlay);
        App.setProgressBar((ProgressBar) preview_overlay.findViewById(R.id.progressBar));
        preview = (TextView) view.findViewById(R.id.preview);
        debug = (TextView) view.findViewById(R.id.debug);
        return view;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (preview!=null) preview.setText("");
        if (debug!=null) debug.setText("");
        setCandidatesViewShown(true);
        mTranslator.reset();
        preview_length=0;
        initializeTranslator(App.getTranslatorType());
        if (App.getMachineType() == StenoMachine.TYPE.VIRTUAL) {
            launchVirtualKeyboard();
        } else {
            removeVirtualKeyboard();
        }
    }

    @Override
    public void onUpdateCursor(Rect newCursor) {
        super.onUpdateCursor(newCursor);
//        history.clear();
//        preview_length=0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ViewGroup parent = (ViewGroup) mKeyboard.getParent();
        parent.removeAllViews();
        setCandidatesViewShown(false);
        unregisterReceiver(mUsbReceiver);
        mKeyboard=null;
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
    public void onStroke(Set<String> keys) {
        Stroke stroke = new Stroke(keys);
        processStroke(stroke);
    }

    @Override
    public void onDictionaryLoaded() {
        unlockKeyboard();
   }

    // Private methods

    private void initializeTranslator(Translator.TYPE t) {
        switch (t) {
            case RawStrokes: mTranslator = new RawStrokeTranslator(); break;
            case SimpleDictionary:
                mTranslator = new SimpleTranslator();
                ((SimpleTranslator) mTranslator).setDictionary(App.getDictionary(this));
                break;
        }
        if (mTranslator.usesDictionary())
            loadDictionary();
    }

    private void processStroke(Stroke stroke) {
        TranslationResult translation = mTranslator.translate(stroke);
        sendText(translation);
    }

    private void selectDictionaries() {
        Intent intent = new Intent(mKeyboard.getContext(), SelectDictionaryActivity.class);
        lockKeyboard();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void loadDictionary() {
        if (!App.isDictionaryLoaded()) {
            lockKeyboard();
            App.getDictionary(this);
        } else {
            unlockKeyboard();
        }
    }

    private void sendText(TranslationResult tr) {
        preview.setText(tr.getPreview());
        debug.setText(tr.getExtra());
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return; //short circuit
        connection.beginBatchEdit();
        if (INLINE_PREVIEW && preview_length>0) {
            connection.deleteSurroundingText(preview_length, 0);
        }
        // deal with backspaces
        if (tr.getBackspaces()==-1) {  // this is a special signal to remove the prior word
            smartDelete(connection);
        } else if (tr.getBackspaces() > 0) {
            connection.deleteSurroundingText(tr.getBackspaces(), 0);
        }
        connection.commitText(tr.getText(), 1);
        if (INLINE_PREVIEW) {
            String p = tr.getPreview();
            connection.commitText(p, 1);
            preview_length = p.length();
        }
        connection.endBatchEdit();
    }

    private void smartDelete(InputConnection connection) {
        try {
            String t = connection.getTextBeforeCursor(2, 0).toString();
            while (! (t.length()==0 || t.equals(" "))) {
                connection.deleteSurroundingText(1, 0);
                t = connection.getTextBeforeCursor(1, 0).toString();
            }
        } finally {
            connection.commitText("", 1);
        }
    }


    private void saveIntPreference(String name, int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(name, value);
        editor.commit();
    }

    // *** NKeyRollover Keyboard ***

    private boolean dispatchKeyEvent(KeyEvent event) {
        StenoMachine inputDevice = App.getInputDevice();
        if (inputDevice instanceof NKeyRolloverMachine) {
            ((NKeyRolloverMachine) inputDevice).handleKeys(event);
        }
        return (event.getKeyCode() != KeyEvent.KEYCODE_BACK);
    }

    // *** Virtual Keyboard ***

    private void launchVirtualKeyboard() {
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        keyboard.setOnStrokeListener(this);
        //keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_in));
        keyboard.setVisibility(View.VISIBLE);
        if (App.getDictionary(this).isLoading())
            lockKeyboard();
        else
            unlockKeyboard();
    }

    private void removeVirtualKeyboard() {
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        if (keyboard != null) {
            //keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_out));
            keyboard.setVisibility(View.GONE);
        }
    }

    private void lockKeyboard() {
        View overlay;
        if (App.getMachineType() == StenoMachine.TYPE.VIRTUAL) {
            overlay = mKeyboard.findViewById(R.id.overlay);
            if (overlay != null) overlay.setVisibility(View.VISIBLE);
        }
        if (preview_overlay != null) preview_overlay.setVisibility(View.VISIBLE);
        if (mTranslator!=null) mTranslator.lock();
    }

    private void unlockKeyboard() {
        View overlay = mKeyboard.findViewById(R.id.overlay);
        if (overlay!=null) overlay.setVisibility(View.INVISIBLE);
        if (preview_overlay != null) preview_overlay.setVisibility(View.INVISIBLE);
        if (mTranslator!=null) mTranslator.unlock();
    }


    // *** Stuff to change Input Device ***

    private void setMachineType(StenoMachine.TYPE t) {
        if (t==null) t= StenoMachine.TYPE.VIRTUAL;
        if (App.getMachineType()==t) return; //short circuit
        App.setMachineType(t);
        saveIntPreference(StenoApp.KEY_MACHINE_TYPE, App.getMachineType().ordinal());
        switch (App.getMachineType()) {
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
                if (mKeyboard!=null) removeVirtualKeyboard();
                ((UsbManager)getSystemService(Context.USB_SERVICE))
                        .requestPermission(App.getUsbDevice(), mPermissionIntent);
                break;
        }
    }

    private void registerMachine(StenoMachine machine) {
        if (App.getInputDevice()!=null) App.getInputDevice().stop(); //stop the prior device
        App.setInputDevice(machine);
        machine.setOnStrokeListener(this);
        machine.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            setMachineType(StenoMachine.TYPE.KEYBOARD);
            setCandidatesViewShown(true);
        }
        if(newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            setMachineType(StenoMachine.TYPE.VIRTUAL);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "mUSBReceiver: received detached event");
                App.setUsbDevice(null);
                setMachineType(StenoMachine.TYPE.VIRTUAL);
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "mUSBReceiver: received attach event");
                App.setUsbDevice((UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
                setMachineType(StenoMachine.TYPE.TXBOLT);
             }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    App.setUsbDevice(device);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //TODO: (also add stuff to known devices list)
                            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                            registerMachine(new TXBoltMachine(usbManager, device));
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

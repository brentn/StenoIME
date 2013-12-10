package com.brentandjody;

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
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.brentandjody.Input.NKeyRolloverMachine;
import com.brentandjody.Input.StenoMachine;
import com.brentandjody.Input.TXBoltMachine;
import com.brentandjody.Input.TouchLayer;
import com.brentandjody.Translator.Dictionary;
import com.brentandjody.Translator.RawStrokeTranslator;
import com.brentandjody.Translator.SimpleTranslator;
import com.brentandjody.Translator.Stroke;
import com.brentandjody.Translator.TranslationResult;
import com.brentandjody.Translator.Translator;

import java.util.Set;
import java.util.Stack;

/**
 * Created by brent on 30/11/13.
 * Replacement Keyboard
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeListener,
        StenoMachine.OnStrokeListener, Dictionary.OnDictionaryLoadedListener {

    private static final String TAG = "StenoIME";
    private static final String ACTION_USB_PERMISSION = "com.brentandjody.USB_PERMISSION";
    private static final String MACHINE_TYPE = "current_machine_type";
    private static final String TRANSLATOR_TYPE = "selected_translator_type";
    public static final String DICTIONARY_SIZE = "dictionary_size";

    private StenoApplication App;
    private SharedPreferences prefs;
    private StenoMachine.TYPE mMachineType;
    private Translator.TYPE mTranslatorType;
    private Dictionary mDictionary;
    private LinearLayout mKeyboard;
    private Translator mTranslator;
    private PendingIntent mPermissionIntent;
    private TextView preview;
    private TextView debug;
    private LinearLayout preview_overlay;
    private Stack<Integer> history = new Stack<Integer>();

    @Override
    public void onCreate() {
        super.onCreate();
        App = ((StenoApplication) getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mMachineType = StenoMachine.TYPE.values()[prefs.getInt(MACHINE_TYPE, 0)];
        mTranslatorType = Translator.TYPE.values()[prefs.getInt(TRANSLATOR_TYPE, 1)];//TODO:change default ot 0
        mDictionary = App.getDictionary();
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
        if (mMachineType == StenoMachine.TYPE.VIRTUAL) {
             launchVirtualKeyboard();
        } else {
            removeVirtualKeyboard();
        }
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
        preview = (TextView) view.findViewById(R.id.preview);
        debug = (TextView) view.findViewById(R.id.debug);
        preview_overlay = (LinearLayout) view.findViewById(R.id.preview_overlay);
        setCandidatesViewShown(true);
        return view;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (preview!=null) preview.setText("");
        if (debug!=null) debug.setText("");
        history.clear();
        setCandidatesViewShown(true);
        initializeTranslator(mTranslatorType);
        if (mTranslator.usesDictionary()) {
            mDictionary.setOnDictionaryLoadedListener(this);
            loadDictionary();
        }

    }

    @Override
    public void onUpdateCursor(Rect newCursor) {
        super.onUpdateCursor(newCursor);
        history.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((ViewGroup) mKeyboard.getParent()).removeAllViews();
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
                ((SimpleTranslator) mTranslator).setDictionary(mDictionary);
                break;
        }
    }

    private void processStroke(Stroke stroke) {
        TranslationResult translation = mTranslator.translate(stroke);
        sendText(translation);
        int undo_size = 0;
        if (translation.getBackspaces()>=0) undo_size = translation.getText().length()-translation.getBackspaces();
        if (undo_size > 0) history.push(undo_size);
    }

    private void loadDictionary() {
        //TODO:get rid of default dictionary
        mDictionary=App.getDictionary();
        if (mDictionary.size() == 0 ) {
            lockKeyboard();
            ProgressBar progressBar;
            if (preview_overlay==null) progressBar = new ProgressBar(getApplicationContext());
            else progressBar = (ProgressBar) preview_overlay.findViewById(R.id.progressBar);
            progressBar.setProgress(0);
            int size = prefs.getInt(DICTIONARY_SIZE, 100000);
            mDictionary.load("dict.json", progressBar, size);
        } else {
            unlockKeyboard();
        }
    }

    private void sendText(TranslationResult tr) {
        preview.setText(tr.getPreview());
        debug.setText(tr.getExtra());
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return; //short circuit
        // deal with backspaces
        connection.beginBatchEdit();
        if (tr.getBackspaces()==-1) {  // this is a special signal to remove the prior word
            if (history.isEmpty()) {
                smartDelete(connection);
            } else {
                connection.deleteSurroundingText(history.pop(), 0);
            }
        } else if (tr.getBackspaces() > 0) {
            connection.deleteSurroundingText(tr.getBackspaces(), 0);
        }
        connection.commitText(tr.getText(), 1);
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
        setCandidatesViewShown(false);
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        keyboard.setOnStrokeListener(this);
        keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_in));
        keyboard.setVisibility(View.VISIBLE);
        if (mDictionary.isLoading())
            lockKeyboard();
        else
            unlockKeyboard();
        mKeyboard.invalidate();
        setCandidatesViewShown(isInputViewShown());
    }

    private void removeVirtualKeyboard() {
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        if (keyboard != null) {
            setCandidatesViewShown(false);
            keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_out));
            keyboard.setVisibility(View.GONE);
        }
        mKeyboard.invalidate();
    }

    private void lockKeyboard() {
        View overlay;
        if (mMachineType == StenoMachine.TYPE.VIRTUAL) {
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
                setCandidatesViewShown(true);
                break;
            case TXBOLT:
                Toast.makeText(this,"TX-Bolt Machine Detected",Toast.LENGTH_SHORT).show();
                if (mKeyboard!=null) removeVirtualKeyboard();
                ((UsbManager)getSystemService(Context.USB_SERVICE))
                        .requestPermission(App.getUsbDevice(), mPermissionIntent);
                setCandidatesViewShown(true);
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

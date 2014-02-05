package com.brentandjody.stenoime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
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

import java.util.Date;
import java.util.Set;

/**
 * Created by brent on 30/11/13.
 * Replacement Keyboard
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeListener,
        StenoMachine.OnStrokeListener, Dictionary.OnDictionaryLoadedListener {

    private static final String TAG = "StenoIME";
    private static final String ACTION_USB_PERMISSION = "com.brentandjody.USB_PERMISSION";

    private static boolean TXBOLT_CONNECTED=false;

    private StenoApp App; // to make it easier to access the Application class
    private SharedPreferences prefs;
    private boolean inline_preview;
    private boolean keyboard_locked=false;
    private boolean configuration_changed;
    private Translator mTranslator;
    //TXBOLT:private PendingIntent mPermissionIntent;

    //layout vars
    private LinearLayout mKeyboard;
    private LinearLayout preview_overlay;
    private View candidates_view;
    private View candidates_bar;
    private TextView preview;
    private TextView debug_text;

    private int preview_length = 0;
    private boolean redo_space;
    
    private int stats_strokes;
    private int stats_chars;
    private long stats_start;
    private int stats_corrections;


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();
        //initialize global stuff
        App = ((StenoApp) getApplication());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        configuration_changed=false;
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false); //load default values
        resetStats();
        //TXBOLT:mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        configuration_changed=true;
    }


    @Override
    public void onInitializeInterface() {
        // called before initialization of interfaces, and after config changes
        Log.d(TAG, "onInitializeInterface()");
        super.onInitializeInterface();
        // create the candidates_view here (early), because we need to show progress when loading dictionary
        initializeCandidatesView();
        App.setMachineType(StenoMachine.TYPE.VIRTUAL);
        initializeMachine();
    }

    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView()");
        super.onCreateInputView();
        mKeyboard = new LinearLayout(this);
        mKeyboard.addView(getLayoutInflater().inflate(R.layout.keyboard, null));
        TouchLayer touchLayer = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        touchLayer.setOnStrokeListener(this);
        mKeyboard.findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchSettingsActivity();
            }
        });
        if (App.isDictionaryLoaded()) unlockKeyboard();
        return mKeyboard;
    }

    @Override
    public View onCreateCandidatesView() {
        Log.d(TAG, "onCreateCandidatesView()");
        super.onCreateCandidatesView();
        return candidates_view;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        Log.d(TAG, "onStartInput()");
        super.onStartInput(attribute, restarting);
        if (!isTextFieldSelected(attribute)) { //no edit field is selected
            setCandidatesViewShown(false);
            removeVirtualKeyboard();
        } else {
            initializeMachine();
            initializePreview();
            if (mTranslator!= null) {
                if (mTranslator.usesDictionary())  loadDictionary();
                if (!restarting) mTranslator.reset(); // clear stroke history
            }
            drawUI();
        }
        initializeTranslator(); //this has to come after drawing everything else (to show the progressbar)
    }

    @Override
    public void onFinishInput() {
        Log.d(TAG, "onFinishInput()");
        super.onFinishInput();
        if (configuration_changed) {
            configuration_changed=false;
            return;
        }
        setCandidatesViewShown(false);
        removeVirtualKeyboard();
    }

    @Override
    public void onUnbindInput() {
        Log.d(TAG, "onUnbindInput()");
        super.onUnbindInput();
        recordStats();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//TXBOLT:        unregisterReceiver(mUsbReceiver);
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

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
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

    private boolean isTextFieldSelected(EditorInfo editorInfo) {
        if (editorInfo == null) return false;
        return (editorInfo.initialSelStart >= 0 || editorInfo.initialSelEnd >= 0);
    }

    private boolean isKeyboardConnected() {
        Configuration config = getResources().getConfiguration();
        return (App.isNkro_enabled()
                && config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
                && config.keyboard == Configuration.KEYBOARD_QWERTY);
    }

    private void initializeMachine() {
        if (isKeyboardConnected()) {
            setMachineType(StenoMachine.TYPE.KEYBOARD);
        } else {
            setMachineType(StenoMachine.TYPE.VIRTUAL);
            if (App.isDictionaryLoaded()) unlockKeyboard();
        }
    }

    private void initializeCandidatesView() {
        candidates_view = getLayoutInflater().inflate(R.layout.preview, null);
        candidates_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendText(mTranslator.submitQueue());
            }
        });
        preview = (TextView) candidates_view.findViewById(R.id.preview);
        preview_overlay = (LinearLayout) candidates_view.findViewById(R.id.preview_overlay);
        debug_text = (TextView) candidates_view.findViewById(R.id.debug);
        // register the progress bar with the application, for dictionary loading
        App.setProgressBar((ProgressBar) preview_overlay.findViewById(R.id.progressBar));
        if (isKeyboardConnected()) onCreateCandidatesView();  //make sure to run this
    }

    private void initializePreview() {
        Log.d(TAG, "initializePreview()");
        inline_preview = prefs.getBoolean("pref_inline_preview", false);
        if (App.isDictionaryLoaded()) {
            if (getCurrentInputConnection()==null)
                showPreviewBar(false);
            else
                showPreviewBar(!inline_preview);
        } else {
            showPreviewBar(true);
        }
        if (! inline_preview) {
            if (preview!=null) preview.setText("");
            if (debug_text !=null) debug_text.setText("");
        }
        preview_length=0;
    }

    private void initializeTranslator() {
        Log.d(TAG, "initializeTranslator()");
        switch (App.getTranslatorType()) {
            case RawStrokes: mTranslator = new RawStrokeTranslator(); break;
            case SimpleDictionary:
                mTranslator = new SimpleTranslator(getApplicationContext());
                ((SimpleTranslator) mTranslator).setDictionary(App.getDictionary(this));
                break;
        }
        if (mTranslator.usesDictionary()) {
            loadDictionary();
        }
    }

    private void drawUI() {
        showPreviewBar(!inline_preview);
        if (isKeyboardConnected()) {
            removeVirtualKeyboard();
        } else {
            launchVirtualKeyboard();
        }
    }

    private void processStroke(Stroke stroke) {
        if (!keyboard_locked) {
            sendText(mTranslator.translate(stroke));
            stats_strokes++;
        }
        if (stroke.isCorrection()) {
            stats_corrections++;
        }
    }

    private void launchSettingsActivity() {
        Intent intent = new Intent(mKeyboard.getContext(), SettingsActivity.class);
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
        debug_text.setText(tr.getExtra());
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return; //short circuit
        connection.beginBatchEdit();
        //remove the preview
        if (inline_preview && preview_length>0) {
            connection.deleteSurroundingText(preview_length, 0);
            if (redo_space) {
                connection.commitText(" ", 1);
            }
        }
        // deal with backspaces
        if (tr.getBackspaces()==-1) {  // this is a special signal to remove the prior word
            smartDelete(connection);
        } else if (tr.getBackspaces() > 0) {
            connection.deleteSurroundingText(tr.getBackspaces(), 0);
            stats_chars -= tr.getBackspaces();
        }
        connection.commitText(tr.getText(), 1);
        stats_chars += tr.getText().length();
        //draw the preview
        if (inline_preview) {
            String p = tr.getPreview();
            if (mTranslator instanceof SimpleTranslator) {
                int bs = ((SimpleTranslator) mTranslator).preview_backspaces();
                redo_space=(bs > 0);
            }
            if (redo_space)
                connection.deleteSurroundingText(1, 0);
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
                stats_chars --;
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
    
    private void resetStats() {
        stats_chars=0;
        stats_strokes=0;
        stats_start= new Date().getTime();
    }
    
    private void recordStats() {
        if (stats_strokes==0) return;
        Double stats_duration = (new Date().getTime() - stats_start)/60000d;
        Log.i(TAG,"Strokes:"+stats_strokes+" Words:"+(stats_chars/5)+" Duration:"+stats_duration);
        if (stats_strokes>0 && stats_duration>.01) Log.i(TAG,"Speed:"+((stats_chars/5)/(stats_duration)+" Ratio: 1:"+(stats_chars/stats_strokes)));
        //TODO: save to database
        resetStats();
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
        Log.d(TAG, "launchVirtualKeyboard()");
        if (mKeyboard!=null) {
            showPreviewBar(false);
            if (mKeyboard.getVisibility()==View.VISIBLE) return;
            //mKeyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_in));
            mKeyboard.setVisibility(View.VISIBLE);
            showPreviewBar(!inline_preview);
        }
    }

    private void removeVirtualKeyboard() {
        Log.d(TAG, "removeVirtualKeyboard()");
        //TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        if (mKeyboard != null) {
            if (mKeyboard.getVisibility()==View.GONE) return;
            //mKeyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_out));
            mKeyboard.setVisibility(View.GONE);
        }
    }

    private void lockKeyboard() {
        Log.d(TAG, "lockKeyboard()");
        keyboard_locked=true;
        showPreviewBar(true);
        if ((mKeyboard != null) && (App.getMachineType() == StenoMachine.TYPE.VIRTUAL)) {
            View overlay = mKeyboard.findViewById(R.id.overlay);
            if (overlay != null) overlay.setVisibility(View.VISIBLE);
        }
        if (!App.isDictionaryLoaded() && preview_overlay != null) preview_overlay.setVisibility(View.VISIBLE);
        if (mTranslator!=null) mTranslator.lock();
    }

    private void unlockKeyboard() {
        Log.d(TAG, "unlockKeyboard()");
        if (mKeyboard != null) {
            View overlay = mKeyboard.findViewById(R.id.overlay);
            if (overlay!=null) overlay.setVisibility(View.INVISIBLE);
        }
        if (preview_overlay != null) preview_overlay.setVisibility(View.GONE);
        if (mTranslator!=null) mTranslator.unlock();
        keyboard_locked=false;
        showPreviewBar(!inline_preview);
    }

    private void showPreviewBar(boolean show) {
        if (!isTextFieldSelected(getCurrentInputEditorInfo())) {
            setCandidatesViewShown(false);
            return;
        }
        setCandidatesViewShown(show);
        if (mKeyboard==null) return;
        View shadow = mKeyboard.findViewById(R.id.shadow);
        if (shadow!=null) {
            if (show) shadow.setVisibility(View.GONE);
            else shadow.setVisibility(View.VISIBLE);
        }
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
                if (mKeyboard==null) onCreateInputView();
                if (candidates_view==null) onCreateCandidatesView();
                if (mKeyboard!=null) launchVirtualKeyboard();
                break;
            case KEYBOARD:
                Toast.makeText(this,"Physical Keyboard Detected",Toast.LENGTH_SHORT).show();
                if (mKeyboard!=null) removeVirtualKeyboard();
                if (candidates_view==null) onCreateCandidatesView();
                registerMachine(new NKeyRolloverMachine());
                break;
//TXBOLT:            case TXBOLT:
//                Toast.makeText(this,"TX-Bolt Machine Detected",Toast.LENGTH_SHORT).show();
//                if (mKeyboard!=null) removeVirtualKeyboard();
//                ((UsbManager)getSystemService(Context.USB_SERVICE))
//                        .requestPermission(App.getUsbDevice(), mPermissionIntent);
//                break;
        }
    }

    private void registerMachine(StenoMachine machine) {
        if (App.getInputDevice()!=null) App.getInputDevice().stop(); //stop the prior device
        App.setInputDevice(machine);
        machine.setOnStrokeListener(this);
        machine.start();
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

package com.brentandjody.stenoime;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
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
import com.brentandjody.stenoime.performance.Database;
import com.brentandjody.stenoime.performance.PerformanceItem;

import java.util.Date;
import java.util.Set;

/**
 * Created by brent on 30/11/13.
 * Replacement Keyboard
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeListener,
        StenoMachine.OnStrokeListener, Dictionary.OnDictionaryLoadedListener {

    private static final String STENO_STROKE = "com.brentandjody.STENO_STROKE";
    private static final String TAG = StenoIME.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "com.brentandjody.USB_PERMISSION";

    private static boolean TXBOLT_CONNECTED=false;

    private StenoApp App; // to make it easier to access the Application class
    private SharedPreferences prefs;
    private boolean inline_preview;
    private boolean keyboard_locked=false;
    private boolean configuration_changed;
    private Translator mTranslator;
    private long last_notification_time;
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

    private PerformanceItem stats = new PerformanceItem();


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
                showSpecialKeysDialog();
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
        if (mTranslator!=null && mTranslator instanceof SimpleTranslator) {
            ((SimpleTranslator) mTranslator).releaseOptimizer();
        }
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
        Intent intent = new Intent(STENO_STROKE);
        intent.putExtra("stroke", stroke.rtfcre());
        sendBroadcast(intent);
    }

    @Override
    public void onDictionaryLoaded() {
        unlockKeyboard();
    }

    // Private methods

    private void showSpecialKeysDialog() {
        final AlertDialog alert;
        LayoutInflater inflater = getLayoutInflater();
        View dialog_view = inflater.inflate(R.layout.specialkeys, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialog_view)
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        alert = builder.create();
        Window window = alert.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mKeyboard.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        lp.verticalMargin = 0.2f;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        Button submit_button = (Button) dialog_view.findViewById(R.id.submit_button);
        submit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();

                StenoIME.this.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
            }
        });
        Button settings_button = (Button) dialog_view.findViewById(R.id.settings_button);
        settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                launchSettingsActivity();
            }
        });
        Button switch_input_button = (Button) dialog_view.findViewById(R.id.switch_input_button);
        switch_input_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert.dismiss();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
            }
        });
        alert.show();
    }

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
        inline_preview = prefs.getBoolean(App.KEY_INLINE_PREVIEW, false);
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
            case RawStrokes: mTranslator = new RawStrokeTranslator();
                break;
            case SimpleDictionary:
                mTranslator = new SimpleTranslator(getApplicationContext());
                ((SimpleTranslator) mTranslator).setDictionary(App.getDictionary(this));
                break;
        }
//        if (mTranslator.usesDictionary()) {
//            loadDictionary();
//        }
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
            TranslationResult t = mTranslator.translate(stroke);
            sendText(t);
            stats.addStroke();
            sendNotification();
        }
        if (stroke.isCorrection()) {
            stats.addCorrection();
        }
    }

    private void launchSettingsActivity() {
        Context context = mKeyboard.getContext();
        Intent intent = new Intent(context, SettingsActivity.class);
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
            stats.addLetters(-tr.getBackspaces());
        }
        connection.commitText(tr.getText(), 1);
        stats.addLetters(tr.getText().length());
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
                stats.addLetters(-1);
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
        stats = new PerformanceItem();
    }
    
    private void recordStats() {
        if (stats.strokes() == 0) return;
        Double stats_duration = (new Date().getTime() - stats.when().getTime()) / 60000d;
        stats.setMinutes(stats_duration);
        Double stats_words = stats.letters() / 5d;
        Double stats_ratio = stats_words / stats.strokes();
        Log.i(TAG, "Strokes:" + stats.strokes() + " Words:" + (stats_words) + " Duration:" + stats_duration);
        if (stats.strokes() > 0 && stats_duration > .01) {
            Log.i(TAG, "Speed:" + ((stats.letters() / 5d) / (stats_duration) + " Ratio: 1:" + (stats_ratio)));
            new Database(getApplicationContext()).insert(stats);
        }
        resetStats();
    }

    private void sendNotification() {
        if (App.showPerformanceNotifications()) {
            long time = new Date().getTime();
            //don't notify more than every 2 seconds
            if (time - last_notification_time > 2000) {
                last_notification_time = time;
                Double minutes = (new Date().getTime() - stats.when().getTime()) / 60000d;
                Double words = stats.letters() / 5.0;
                Double speed = Math.round(words * 10.0 / minutes) / 10.0;
                Double strokes_per_word = Math.round(stats.strokes() * 100.0 / words) / 100.0;
                long stats_accuracy = 100 - Math.round(stats.corrections() * 100.0 / stats.strokes());
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_stat_stenoime)
                                .setContentTitle("Steno Performance")
                                .setContentText("Speed:" + speed + " strokes/word:" + (strokes_per_word) + " Accuracy: " + stats_accuracy + "%");
                int mNotificationId = 1;
                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            }
        }
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

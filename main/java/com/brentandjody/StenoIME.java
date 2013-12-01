package com.brentandjody;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.brentandjody.Keyboard.NKeyRolloverMachine;
import com.brentandjody.Keyboard.StenoMachine;
import com.brentandjody.Keyboard.TouchLayer;
import com.brentandjody.Translator.Dictionary;
import com.brentandjody.Translator.Stroke;

import java.util.Set;

/**
 * Created by brent on 30/11/13.
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeCompleteListener, StenoMachine.OnStrokeListener {

    private static final String TAG = "StenoIME";
    private static final String ACTION_USB_PERMISSION = "com.brentandjody.USB_PERMISSION";

    private StenoApplication App;
    private StenoMachine.TYPE mMachineType;
    private Dictionary mDictionary;
    private LinearLayout mKeyboard;
    private PendingIntent mPermissionIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        App = ((StenoApplication) getApplication());
        mDictionary = App.getDictionary();
    }

    @Override
    public View onCreateInputView() {
        mKeyboard = null;
        if (App.getMachineType() == StenoMachine.TYPE.VIRTUAL) {
            mKeyboard = new LinearLayout(this);
            LayoutInflater layoutInflater = getLayoutInflater();
            mKeyboard = (LinearLayout) layoutInflater.inflate(R.layout.keyboard, null);
            launchVirtualKeyboard();
        }
        return mKeyboard;
    }

    @Override
    public View onCreateCandidatesView() {
        return super.onCreateCandidatesView();
        //TODO: STUB
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
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        if (mMachineType==null) mMachineType = App.getMachineType();
        //TODO: STUB
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        //TODO: STUB
    }

    @Override
    public void onUnbindInput() {
        super.onUnbindInput();
        //TODO: STUB
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //TODO: STUB
    }

    // Implemented Interfaces
    @Override
    public void onStrokeComplete(Stroke stroke) {
        //TODO: STUB
    }

    @Override
    public void onStroke(Set<String> keys) {
        //TODO: STUB
    }

    private void setMachineType(StenoMachine.TYPE t) {
        if (t==null) t= StenoMachine.TYPE.VIRTUAL;
        if (mMachineType==t) return; //short circuit
        mMachineType = t;
        App.setMachineType(t);
        switch (t) {
            case VIRTUAL:
                App.setInputDevice(null);
                Toast.makeText(this, "Virtual Keyboard Selected", Toast.LENGTH_SHORT).show();
                launchVirtualKeyboard();
                break;
            case KEYBOARD:
                Toast.makeText(this,"Physical Keyboard Detected",Toast.LENGTH_SHORT).show();
                removeVirtualKeyboard();
                registerMachine(new NKeyRolloverMachine());
                break;
            case TXBOLT:
                Toast.makeText(this,"TX-Bolt Machine Detected",Toast.LENGTH_SHORT).show();
                //TODO:
                removeVirtualKeyboard();
                break;
        }
        try {unregisterReceiver(mUsbReceiver);} catch(Exception e){}
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }



    private void registerMachine(StenoMachine machine) {
        App.setInputDevice(machine);
        App.getInputDevice().setOnStrokeListener(this);
    }

    private void launchVirtualKeyboard() {
        TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
        keyboard.setOnStrokeCompleteListener(this);
        keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_in));
    }

    private void removeVirtualKeyboard() {
        if (mKeyboard != null) {
            TouchLayer keyboard = (TouchLayer) mKeyboard.findViewById(R.id.keyboard);
            keyboard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_out));
            mKeyboard.invalidate();
            mKeyboard=null;
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "mUSBReceiver: received detached event");
                setMachineType(StenoMachine.TYPE.VIRTUAL);
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
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

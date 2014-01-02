package com.brentandjody.stenoime.Input;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import com.brentandjody.stenoime.driver.UsbSerialDriver;
import com.brentandjody.stenoime.driver.UsbSerialProber;
import com.brentandjody.stenoime.driver.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by brent on 03/12/13.
 * Implements the TX Bolt (Serial Transmit Only) protocol
 */

public class TXBoltMachine implements StenoMachine {

    private static String[] STENO_KEYS = {"S-", "T-", "K-", "P-", "W-", "H-",
                                          "R-", "A-", "O-", "*", "-E", "-U",
                                          "-F", "-R", "-P", "-B", "-L", "-G",
                                          "-T", "-S", "-D", "-Z", "#"};
    private static final String TAG = "StenoIME";
    private static final int TIMEOUT = 500;
    private boolean finished=false;
    private UsbSerialDriver mDriver;
    private OnStrokeListener onStrokeListener;

    public TXBoltMachine(UsbManager manager, UsbDevice device) {
        mDriver = UsbSerialProber.acquire(manager, device);
        Log.d(TAG, "Instantiated USB Serial Driver: "+manager+device+mDriver);
        try {
            mDriver.open();
            mDriver.setParameters(9600, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
        } catch (IOException e) {
            Log.e(TAG, "Driver failed to initialize");
        }
        stop();
        start();
    }

    @Override
    public void setOnStrokeListener(OnStrokeListener listener) {
        onStrokeListener = listener;
    }

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
    new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int size = data.length;
                    int key_set;
                    int last_key_set = 0;
                    byte b;
                    List<String> keys = new ArrayList<String>();
                    if (size > 0) Log.d(TAG, "Data: "+size+" bytes");
                    for (byte aData : data) {
                        b = aData;
                        key_set = b >> 6;
                        if (key_set < last_key_set) { //new stroke
                            TXBoltMachine.this.onStrokeListener.onStroke(new LinkedHashSet<String>(keys));
                            Log.d(TAG, "Stroke: " + keys.toString());
                            keys.clear();
                        }
                        addKeys(b, keys);
                    }
                }
            }).start();
        }
    };


    public void stop() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    public void start() {
        if (mDriver != null) {
            mSerialIoManager = new SerialInputOutputManager(mDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void addKeys(byte data, List<String> keys) {
        int key_set = data >> 6;
        for (int i=0; i<6; i++) {
            if (((data >> i) & 1) == 1) {
                keys.add(STENO_KEYS[(key_set*6)+i]);
            }
        }
    }
}

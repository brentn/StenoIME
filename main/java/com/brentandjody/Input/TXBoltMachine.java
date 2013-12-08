package com.brentandjody.Input;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by brent on 03/12/13.
 * Implements the TX Bolt (Serial Transmit Only) protocol
 */

public class TXBoltMachine extends SerialDevice implements StenoMachine {

    private static String[] STENO_KEYS = {"S-", "T-", "K-", "P-", "W-", "H-",
                                          "R-", "A-", "O-", "*", "-E", "-U",
                                          "-F", "-R", "-P", "-B", "-L", "-G",
                                          "-T", "-S", "-D", "-Z", "#"};
    private static final String TAG = "StenoIME";
    private static final int TIMEOUT = 500;
    private boolean finished=false;
    private OnStrokeListener onStrokeListener;

    public TXBoltMachine(UsbDevice device, UsbDeviceConnection connection) {
        super(device, connection);
    }

    @Override
    public void setOnStrokeListener(OnStrokeListener listener) {
        onStrokeListener = listener;
    }

    public void stop() {
        finished=true;
    }

    public void start() {
        finished=false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int size;
                int key_set;
                int last_key_set=0;
                byte data;
                byte[] buffer = new byte[64];
                List<String> keys = new ArrayList<String>();
                try {
                    Log.w(TAG, "about to connect");
                    connect();
                    Log.w(TAG, "connected?");
                    while (!finished) {
                        Log.w(TAG, "begin loop");
                        size = read(buffer, TIMEOUT);
                        for (int i=0; i<size; i++) {
                            data = buffer[i];
                            key_set=data >> 6;
                            if (key_set < last_key_set) { //new stroke
                                onStrokeListener.onStroke(new LinkedHashSet<String>(keys));
                                Log.d(TAG, "Stroke: "+keys.toString());
                                keys.clear();
                            }
                            addKeys(data, keys);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading data");
                }
            }
        }).start();
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

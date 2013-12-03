package com.brentandjody.Input;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

/**
 * Created by brent on 03/12/13.
 * Implements the TX Bolt (Serial Transmit Only) protocol
 */

public class TXBoltMachine extends SerialDevice implements StenoMachine {

    private static final String TAG = "StenoIME";
    private OnStrokeListener onStrokeListener;

    public TXBoltMachine(UsbDevice device, UsbDeviceConnection connection) {
        super(device, connection);
    }

    @Override
    public void setOnStrokeListener(OnStrokeListener listener) {
        onStrokeListener = listener;
    }
}

package com.brentandjody;

import android.app.Application;
import android.hardware.usb.UsbDevice;

import com.brentandjody.Input.StenoMachine;
import com.brentandjody.Translator.Dictionary;

/**
 * Created by brent on 30/11/13.
 */
public class StenoApplication extends Application {

    private Dictionary dictionary;
    private StenoMachine inputDevice;
    private UsbDevice usbDevice;
    private StenoMachine.TYPE machineType = StenoMachine.TYPE.VIRTUAL;

    @Override
    public void onCreate() {
        super.onCreate();
        dictionary = new Dictionary(getApplicationContext());
        inputDevice=null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    // Setters
    public void setInputDevice(StenoMachine sm) {inputDevice = sm;}
    public void setUsbDevice(UsbDevice ud) { usbDevice = ud; }

    // Getters
    public Dictionary getDictionary() {
        return dictionary;
    }
    public StenoMachine getInputDevice() {return inputDevice; }
    public UsbDevice getUsbDevice() { return usbDevice; }

}

package com.brentandjody;

import android.app.Application;

import com.brentandjody.Keyboard.StenoMachine;
import com.brentandjody.Translator.Dictionary;

/**
 * Created by brent on 30/11/13.
 */
public class StenoApplication extends Application {

    private Dictionary dictionary;
    private StenoMachine inputDevice;
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
    public void setMachineType(StenoMachine.TYPE t) {
        machineType = t;
    }
    public void setInputDevice(StenoMachine sm) {inputDevice = sm;}

    // Getters
    public Dictionary getDictionary() {
        return dictionary;
    }
    public StenoMachine.TYPE getMachineType() {
        return machineType;
    }
    public StenoMachine getInputDevice() {return inputDevice; }

}

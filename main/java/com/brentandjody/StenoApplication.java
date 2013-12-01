package com.brentandjody;

import android.app.Application;

import com.brentandjody.Keyboard.StenoMachine;
import com.brentandjody.Translator.Dictionary;

/**
 * Created by brent on 30/11/13.
 */
public class StenoApplication extends Application {

    private final Dictionary dictionary = new Dictionary(this);
    private StenoMachine.TYPE machineType = StenoMachine.TYPE.VIRTUAL;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    // Setters
    public void setMachineType(StenoMachine.TYPE t) {
        machineType = t;
    }

    // Getters
    public Dictionary getDictionary() {
        return dictionary;
    }

    public StenoMachine.TYPE getMachineType() {
        return machineType;
    }

}

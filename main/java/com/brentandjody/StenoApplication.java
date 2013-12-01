package com.brentandjody;

import android.app.Application;

import com.brentandjody.Keyboard.StenoMachine;
import com.brentandjody.Translator.Dictionary;

/**
 * Created by brent on 30/11/13.
 */
public class StenoApplication extends Application {

    private Dictionary dictionary;
    private StenoMachine.TYPE machineType = StenoMachine.TYPE.VIRTUAL;

    @Override
    public void onCreate() {
        super.onCreate();
        dictionary = new Dictionary(getApplicationContext());
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

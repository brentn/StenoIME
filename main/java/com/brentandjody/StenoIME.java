package com.brentandjody;

import android.content.Intent;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.brentandjody.Keyboard.StenoMachine;
import com.brentandjody.Keyboard.TouchLayer;
import com.brentandjody.Translator.Dictionary;
import com.brentandjody.Translator.Stroke;

/**
 * Created by brent on 30/11/13.
 */
public class StenoIME extends InputMethodService implements TouchLayer.OnStrokeCompleteListener {

    private StenoApplication App;
    private StenoMachine.TYPE mMachineType;
    private Dictionary mDictionary;
    private LinearLayout mKeyboard;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
        if (mMachineType==null) mMachineType = App.getMachineType();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
    }

    @Override
    public void onUnbindInput() {
        super.onUnbindInput();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Implemented Interfaces
    @Override
    public void onStrokeComplete(Stroke stroke) {

    }

    private void setMachineType(StenoMachine.TYPE t) {
        if (t==null) t= StenoMachine.TYPE.VIRTUAL;
        mMachineType = t;
        App.setMachineType(t);
        switch (t) {
            case VIRTUAL:
//                App.setInputDevice(null);
                Toast.makeText(this, "Virtual Keyboard Selected", Toast.LENGTH_SHORT).show();
                launchVirtualKeyboard();
                break;
//            case KEYBOARD:
//                Toast.makeText(this,"Physical Keyboard Detected",Toast.LENGTH_SHORT).show();
//                removeVirtualKeyboard();
//                registerMachine(new NKeyRolloverMachine());
//                break;
//            case TXBOLT:
//                Toast.makeText(this,"TX-Bolt Machine Detected",Toast.LENGTH_SHORT).show();
//                removeVirtualKeyboard();
//                break;
        }
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


}

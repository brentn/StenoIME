package com.brentandjody.Input;

import android.util.Log;
import android.view.KeyEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Created by brentn on 22/11/13.
 * Accept input from an N-Key Rollover (or any) keyboard such as MS Sidewinder
 */
public class NKeyRolloverMachine implements StenoMachine {

    private static final String TAG = "StenoIME";

    private static final HashMap<Integer,String> STENO_KEYS = new LinkedHashMap<Integer, String>() {{
        put(KeyEvent.KEYCODE_1, "#");
        put(KeyEvent.KEYCODE_2, "#");
        put(KeyEvent.KEYCODE_3, "#");
        put(KeyEvent.KEYCODE_4, "#");
        put(KeyEvent.KEYCODE_5, "#");
        put(KeyEvent.KEYCODE_6, "#");
        put(KeyEvent.KEYCODE_7, "#");
        put(KeyEvent.KEYCODE_8, "#");
        put(KeyEvent.KEYCODE_9, "#");
        put(KeyEvent.KEYCODE_0, "#");
        put(KeyEvent.KEYCODE_MINUS, "#");
        put(KeyEvent.KEYCODE_EQUALS, "#");
        put(KeyEvent.KEYCODE_Q, "S-");
        put(KeyEvent.KEYCODE_A, "S-");
        put(KeyEvent.KEYCODE_W, "T-");
        put(KeyEvent.KEYCODE_S, "K-");
        put(KeyEvent.KEYCODE_E, "P-");
        put(KeyEvent.KEYCODE_D, "W-");
        put(KeyEvent.KEYCODE_R, "H-");
        put(KeyEvent.KEYCODE_F, "R-");
        put(KeyEvent.KEYCODE_C, "A-");
        put(KeyEvent.KEYCODE_V, "O-");
        put(KeyEvent.KEYCODE_T, "*");
        put(KeyEvent.KEYCODE_G, "*");
        put(KeyEvent.KEYCODE_Y, "*");
        put(KeyEvent.KEYCODE_H, "*");
        put(KeyEvent.KEYCODE_N, "-E");
        put(KeyEvent.KEYCODE_M, "-U");
        put(KeyEvent.KEYCODE_U, "-F");
        put(KeyEvent.KEYCODE_J, "-R");
        put(KeyEvent.KEYCODE_I, "-P");
        put(KeyEvent.KEYCODE_K, "-B");
        put(KeyEvent.KEYCODE_O, "-L");
        put(KeyEvent.KEYCODE_L, "-G");
        put(KeyEvent.KEYCODE_P, "-T");
        put(KeyEvent.KEYCODE_SEMICOLON, "-S");
        put(KeyEvent.KEYCODE_LEFT_BRACKET, "-D");
        put(KeyEvent.KEYCODE_APOSTROPHE, "-Z");
    }};


    private OnStrokeListener onStrokeListener;
    private Set<String> stroke = new HashSet<String>();
    private int total_keys=0;

    public NKeyRolloverMachine() {
    }

    public void handleKeys(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (STENO_KEYS.containsKey(event.getKeyCode()))
                if (event.getRepeatCount() == 0)
                    total_keys++;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (STENO_KEYS.containsKey(event.getKeyCode())) {
                total_keys--;
                stroke.add(STENO_KEYS.get(event.getKeyCode()));
                if (total_keys == 0) {
                    Log.d(TAG, "Stroke: " + stroke.toString());
                    onStrokeListener.onStroke(stroke);
                    stroke.clear();
                }
                if (total_keys < 0) {
                    Log.e(TAG, "totalKeys is less than 0: "+total_keys);
                }
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void setOnStrokeListener(OnStrokeListener listener) {
        onStrokeListener = listener;
    }

}

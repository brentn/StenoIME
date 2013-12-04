package com.brentandjody.Input;

import java.util.Set;

/**
 * Created by brentn on 22/11/13.
 * An abstract class for implementing various steno machine hardware
 */
public interface StenoMachine {

    public static enum TYPE {VIRTUAL, KEYBOARD, TXBOLT }

    public interface OnStrokeListener {
        public void onStroke(Set<String> keys);
    }
    public interface OnStateChangeListener {
        public void onStateChange(String state);
    }

    public abstract void start();
    public abstract void stop();
    public abstract void setOnStrokeListener(OnStrokeListener listener);
}

package com.brentandjody.Translator;

import com.brentandjody.StenoApplication;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by brent on 01/12/13.
 */
public class SimpleTranslator extends Translator {

    private boolean locked = false;
    private Dictionary mDictionary;
    private Deque<String> strokeQ = new ArrayDeque<String>();

    public SimpleTranslator() {
    }

    public void setDictionary(Dictionary dictionary) {
        mDictionary = dictionary;
    }

    @Override
    public boolean usesDictionary() {
        return true;
    }

    @Override
    public void lock() {
        locked = true;
    }

    @Override
    public void unlock() {
        locked=false;
    }

    @Override
    public TranslationResult translate(Stroke stroke) {
        int backspaces = 0;
        String text = "";
        String preview = "";
        String result;
        String full_stroke;
        if (!locked) {
            if (strokeQ.isEmpty()) {
                full_stroke = stroke.rtfcre();
            } else {
                full_stroke = strokesInQueue() + "/" + stroke.rtfcre();
            }
            result = mDictionary.lookup(full_stroke);
            if (found(result)) {
                if (ambiguous(result)) {
                    strokeQ.add(stroke.rtfcre());
                    preview = mDictionary.forceLookup(strokesInQueue());
                } else {
                    text=result+" ";
                    preview = "";
                    strokeQ.clear();
                }
            } else { // not found
                text = mDictionary.lookup(strokesInQueue());
                strokeQ.clear();
                strokeQ.add(stroke.rtfcre());
                preview = mDictionary.forceLookup(stroke.rtfcre());
            }
        }
        return new TranslationResult(backspaces, text, preview);
    }

    private boolean found(String s) {return (s != null); }
    private boolean ambiguous(String s) { return s.equals("");}

    private String strokesInQueue() {
        if (strokeQ.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : strokeQ) {
            sb.append(s).append("/");
        }
        return sb.substring(0, sb.lastIndexOf("/")-1);
    }
}

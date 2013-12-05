package com.brentandjody.Translator;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

/**
 * Created by brent on 01/12/13.
 * Basic dictionary lookup, nothing fancy
 */
public class SimpleTranslator extends Translator {

    private static final String TAG = "StenoIME";
    private boolean locked = false;
    private Dictionary mDictionary;
    private Formatter mFormatter;
    private Deque<String> strokeQ = new ArrayDeque<String>();

    public SimpleTranslator() {
        mFormatter = new Formatter();
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
        if (stroke==null) return new TranslationResult(0, "", "", "");
        int backspaces = 0;
        String text = "";
        String preview = "";
        String lookupResult;
        String partial_stroke;
        if (!locked) {
            if (stroke.rtfcre().equals("*")) { //undo
                if (mFormatter.hasQueue()) {
                    mFormatter.removeItemFromQueue();
                } else {
                    if (!strokeQ.isEmpty()) {
                        strokeQ.removeLast();
                    } else {
                        backspaces=-1; // special code for "remove an entire word"
                    }
                }
            } else {
                if (strokeQ.isEmpty())
                    lookupResult = mDictionary.lookup(stroke.rtfcre());
                else
                    lookupResult = mDictionary.lookup(strokesInQueue()+"/"+stroke.rtfcre());
                if (found(lookupResult)) {
                    if (ambiguous(lookupResult)) {
                        text = "";
                        strokeQ.add(stroke.rtfcre());
                    } else {
                        text = lookupResult;
                        strokeQ.clear();
                    }
                } else { // (not found)
                    partial_stroke = stroke.rtfcre();
                    lookupResult = mDictionary.lookup(partial_stroke);
                    while (!(found(lookupResult) || strokeQ.isEmpty())) {
                        partial_stroke = strokeQ.removeLast()+"/"+partial_stroke;
                        lookupResult = mDictionary.lookup(partial_stroke);
                    }
                    // at this point, either a lookup has been found, or the queue is empty
                    if (found(lookupResult)) {
                        text = mDictionary.forceLookup(strokesInQueue());
                        strokeQ.clear();
                        addToQueue(partial_stroke);
                    } else {
                        addToQueue(partial_stroke);
                        text=strokeQ.removeLast();
                        text=mDictionary.forceLookup(strokesInQueue())+" "+text;
                        strokeQ.clear();
                    }
                }
                preview = mDictionary.forceLookup(strokesInQueue());
                text = mFormatter.format(text);
                while (text.length()>0 && text.charAt(0)=='\b') {
                    backspaces++;
                    text=text.substring(1);
                }
            }
        }
        Log.d(TAG, "text:"+text+" preview:"+preview);
        return new TranslationResult(backspaces, text, preview, Integer.toString(strokeQ.size()));
    }

    private boolean found(String s) {return (s != null); }
    private boolean ambiguous(String s) { return s.equals("");}

    private String strokesInQueue() {
        if (strokeQ.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : strokeQ) {
            sb.append(s).append("/");
        }
        return sb.substring(0, sb.lastIndexOf("/"));
    }

    private void addToQueue(String input) {
        Collections.addAll(strokeQ, input.split("/"));
    }
}


package com.brentandjody.Translator;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Stack;

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
        if (stroke==null || stroke.rtfcre().isEmpty()) return new TranslationResult(0, "", "", "");
        int bs = 0;
        String outtext = "";
        String preview = "";
        String extra = "";
        TranslationResult tr;
        for (String s : stroke.rtfcre().split("/")) {
            tr = translate_simple_stroke(s);
            outtext += tr.getText();
            bs += tr.getBackspaces();
            preview = tr.getPreview();
            extra = tr.getExtra();
        }
        return new TranslationResult(bs, outtext, preview, extra);
    }

    private TranslationResult translate_simple_stroke(String stroke) {
        if (stroke==null) return new TranslationResult(0, "", "", "");
        if (mDictionary.size()<10) return new TranslationResult(0, "", "Dictionary Not Loaded", "");
        int backspaces = 0;
        String text = "";
        String preview = "";
        String lookupResult;
        if (!locked) {
            if (stroke.equals("*")) { //undo
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
                strokeQ.add(stroke);
                lookupResult = mDictionary.lookup(strokesInQueue());
                if (found(lookupResult)) {
                    if (! ambiguous(lookupResult)) {
                        strokeQ.clear();
                        text = mFormatter.format(lookupResult);
                    } // else stroke is already added to queue
                } else {
                    if (strokeQ.size()==1) {
                        text = mFormatter.format(strokesInQueue());
                        strokeQ.clear();
                    } else {  // process strokes in queue
                        Stack<String> tempQ = new Stack<String>();
                        while (!(found(lookupResult) || strokeQ.isEmpty())) {
                            tempQ.push(strokeQ.removeLast());
                            lookupResult = mDictionary.forceLookup(strokesInQueue());
                        }
                        // at this point, either a lookup was found, or the queue is empty
                        if (found(lookupResult)) {
                            text = mFormatter.format(lookupResult);
                            if (text.isEmpty()) text = mDictionary.forceLookup(strokesInQueue());
                            strokeQ.clear();
                            while (!tempQ.isEmpty()) {
                                strokeQ.add(tempQ.pop());
                            }
                            // lookup remaining strokes in queue
                            TranslationResult result = translate_simple_stroke(strokeQ.removeLast()); //recurse
                            text = text.substring(0, text.length()-result.getBackspaces()) + result.getText();

                        } else {
                            while (!tempQ.isEmpty()) {
                                strokeQ.add(tempQ.pop());
                            }
                            text = mFormatter.format(strokesInQueue());
                            strokeQ.clear();
                        }
                    }
                }
                preview = mDictionary.forceLookup(strokesInQueue());
                if (preview==null || preview.isEmpty()) preview = strokesInQueue();
                while (text.length()>0 && text.charAt(0)=='\b') {
                    backspaces++;
                    text=text.substring(1);
                }
            }
        }
        Log.d(TAG, "text:"+text+" preview:"+preview);
        return new TranslationResult(backspaces, text, preview, "("+Integer.toString(strokeQ.size())+")");
    }

    @Override
    public TranslationResult submitQueue() {
        String queue = mFormatter.format(mDictionary.forceLookup(strokesInQueue()));
        strokeQ.clear();
        return new TranslationResult(0, queue, "", "");
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

    private String remove_backspaces(String input) {
        StringBuilder result = new StringBuilder();
        int prefix_bs = 0;
        boolean start=true;
        for (int i=0; i< input.length(); i++) {
            if (start) {
                if (input.charAt(i)=='\b') {
                    prefix_bs++;
                } else {
                    start=false;
                    char[] backspaces = new char[prefix_bs];
                    Arrays.fill(backspaces, '\b');
                    result.append(new String(backspaces));
                }
            }
            if (!start) {
                if ((i < (input.length()-1)) && input.charAt(i+1)=='\b') {
                    i++;
                } else {
                    result.append(input.charAt(i));
                }
            }
        }
        return result.toString();
    }

    private void addToQueue(String input) {
        Collections.addAll(strokeQ, input.split("/"));
    }
}


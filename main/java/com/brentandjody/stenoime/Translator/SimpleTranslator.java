package com.brentandjody.stenoime.Translator;

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
    private Stack<HistoryItem> history = new Stack<HistoryItem>();
    private int preview_backspaces=0;


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
    public void reset() {
        history.removeAllElements();
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
        Formatter.State state;
        int backspaces = 0;
        String text = "";
        String preview_text = "";
        String lookupResult;
        if (!locked) {
            if (stroke.equals("*")) { //undo
                if (!strokeQ.isEmpty()) {
                    strokeQ.removeLast();
                } else {
                    if (!history.isEmpty()) {
                        HistoryItem reset = undoStrokeFromHistory();
                        backspaces = reset.length();
                        text = reset.stroke();
                    } else {
                        backspaces=-1; // special code for "remove last word"
                    }
                }
            } else {
                strokeQ.add(stroke);
                lookupResult = mDictionary.lookup(strokesInQueue());
                if (found(lookupResult)) {
                    if (! ambiguous(lookupResult)) {
                        state = mFormatter.getState();
                        text = mFormatter.format(lookupResult);
                        backspaces = mFormatter.backspaces();
                        history.push(new HistoryItem(text.length(), strokesInQueue(), backspaces, state));
                        strokeQ.clear();
                    } // else stroke is already added to queue
                } else {
                    if (strokeQ.size()==1) {
                        state = mFormatter.getState();
                        text = mFormatter.format(strokesInQueue());
                        backspaces = mFormatter.backspaces();
                        history.push(new HistoryItem(text.length(), strokesInQueue(), backspaces, state));
                        strokeQ.clear();
                    } else {  // process strokes in queue
                        Stack<String> tempQ = new Stack<String>();
                        while (!(found(lookupResult) || strokeQ.isEmpty())) {
                            tempQ.push(strokeQ.removeLast());
                            lookupResult = mDictionary.forceLookup(strokesInQueue());
                        }
                        // at this point, either a lookup was found, or the queue is empty
                        if (found(lookupResult)) {
                            state = mFormatter.getState();
                            text = mFormatter.format(lookupResult);
                            if (text.isEmpty()) text = mFormatter.format(mDictionary.forceLookup(strokesInQueue()));
                            backspaces = mFormatter.backspaces();
                            history.push(new HistoryItem(text.length(), strokesInQueue(), backspaces, state));
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
                            state = mFormatter.getState();
                            text = mFormatter.format(strokesInQueue());
                            backspaces = mFormatter.backspaces();
                            history.push(new HistoryItem(text.length(), strokesInQueue(), backspaces, state));
                            strokeQ.clear();
                        }
                    }
                }
            }
            preview_text = lookupQueue();
        }
        Log.d(TAG, "text:"+text+" preview:"+preview_text);
        return new TranslationResult(backspaces, text, preview_text, "");
    }

    @Override
    public TranslationResult submitQueue() {
        String queue = mFormatter.format(mDictionary.forceLookup(strokesInQueue()));
        strokeQ.clear();
        return new TranslationResult(0, queue, "", "");
    }

    public int preview_backspaces() { return preview_backspaces; }

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

    private String lookupQueue() {
        preview_backspaces=0;
        if (strokeQ.isEmpty()) return "";
        String lookupResult = mDictionary.forceLookup(strokesInQueue());
        if (lookupResult==null) {
            return strokesInQueue();
        } else {
            String result = mFormatter.format(lookupResult, true);
            preview_backspaces = mFormatter.backspaces();
            return result;
        }
    }

    private HistoryItem undoStrokeFromHistory() {
        HistoryItem result = new HistoryItem(0, "", 0, null);
        HistoryItem hItem = history.pop();
        int num_spaces=hItem.backspaces();
        result.setStroke(spaces(num_spaces));
        result.setLength(hItem.length());
        String hStroke = hItem.stroke();
        if (hStroke.contains("/")) {
            mFormatter.restoreState(hItem.getState());
            mDictionary.forceLookup(hStroke.substring(hStroke.lastIndexOf("/") + 1));
            hStroke = hStroke.substring(0, hStroke.lastIndexOf("/"));
            Collections.addAll(strokeQ, hStroke.split("/"));
        } else { // replay prior stroke (in case it was ambiguous)
            if (!history.isEmpty()) {
                hItem = history.pop();
                mFormatter.restoreState(hItem.getState());
                result.setStroke(spaces(hItem.backspaces()));
                result.increaseLength(hItem.length()-num_spaces);
                hStroke = hItem.stroke();
                Collections.addAll(strokeQ, hStroke.split("/"));
            }
        }
        return result;
    }


    private void addToQueue(String input) {
        Collections.addAll(strokeQ, input.split("/"));
    }

    private String spaces(int length) {
        char[] result = new char[length];
        Arrays.fill(result, ' ');
        return new String(result);
    }

    class HistoryItem {
        private int length;
        private String stroke;
        private int backspaces;
        private Formatter.State state;

        public HistoryItem(int l, String s, int bs, Formatter.State st) {
            length = l;
            stroke = s;
            backspaces = bs;
            state = st;
        }

        public void setLength(int length) {
            this.length=length;
        }
        public void setStroke(String stroke) {
            this.stroke = stroke;
        }

        public void increaseLength(int amount) {
            this.length += amount;
        }

        public int length() {
            return length;
        }
        public String stroke() {
            return stroke;
        }
        public int backspaces() {
            return backspaces;
        }
        public Formatter.State getState() {
            return state;
        }
    }
}


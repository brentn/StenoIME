package com.brentandjody.stenoime.Translator;


import android.content.Context;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Stack;

/**
 * Created by brent on 01/12/13.
 * Basic dictionary lookup, nothing fancy
 */
public class SimpleTranslator extends Translator {

    private static final TranslationResult BLANK_RESULT = new TranslationResult(0, "", "", "");

    private boolean locked=false;
    private Dictionary mDictionary=null;
    private Stack<HistoryItem> mHistory = new Stack<HistoryItem>();
    private Deque<String> mStrokeQueue = new ArrayDeque<String>();

    public SimpleTranslator(Context context) {

    }

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public boolean usesDictionary() {
        return true;
    }

    public void setDictionary(Dictionary dictionary) {
        mDictionary = dictionary;
    }

    @Override
    public TranslationResult translate(Stroke stroke) {
        if (locked || stroke==null || stroke.rtfcre().isEmpty()) return BLANK_RESULT;
        String rtfcre = stroke.rtfcre();
        TranslationResult result = BLANK_RESULT;
        if (rtfcre.equals("*")) {
            result = undo();
        } else {
            result = lookup(rtfcre);
        }
        return result;
    }

    //*******Private methods*****

    private TranslationResult undo() {
        // if queue is not empty, just delete an item from the queue
        // otherwise remove an item from history
        TranslationResult result = BLANK_RESULT;
        if (mStrokeQueue.isEmpty()) {
            if (mHistory.isEmpty()) {
                result = new TranslationResult(-1, "", "", "");// special code that deletes prior word
            } else {
                int backspaces = unCommitStroke();
                mStrokeQueue.removeLast();
                result = new TranslationResult(backspaces, "", "", "");
            }
        } else {
            mStrokeQueue.removeLast();
            result = new TranslationResult(0, "", "", "");
        }
        // if stroke_queue is empty, undo one more stroke, and replay the queue
        if (mStrokeQueue.isEmpty()) {
            int backspaces = unCommitStroke();
            result = append(result, new TranslationResult(backspaces, "", "", ""));
            result = append(result, replayQueue());
        }
        result=addPreview(result);
        return result;
    }

    private TranslationResult lookup(String rtfcre) {
        TranslationResult result = BLANK_RESULT;
        mStrokeQueue.add(rtfcre);
        if (mDictionary==null || !mDictionary.isLoaded()) {
            result = new TranslationResult(0, "", "Dictionary not loaded...", "");
        } else {
            boolean done = false;
            while (!mStrokeQueue.isEmpty() && !done) {
                String lookupResult = mDictionary.lookup(strokesInQueue());
                if (found(lookupResult)) {
                    if (no_other_options(lookupResult)) {
                        result = append(result, commitQueue(lookupResult));
                        done = true;
                    } else { //ambiguous
                        done = true;
                    }
                } else {
                    TranslationResult split_result = splitQueue();
                    result = append(result, split_result);
                    done = split_result.getText().isEmpty();
                }
            }
            result = addPreview(result);
        }
        return result;
    }

    private String forceLookup(String rtfcre) {
        // return as much translated English as possible for a given stroke
        String result = mDictionary.forceLookup(rtfcre);
        if (result == null) { //split it up
            result="";
            String word, partial_stroke;
            while (!rtfcre.isEmpty()) {
                partial_stroke=rtfcre;
                word="";
                while (word.isEmpty() && partial_stroke.contains("/")) {
                    partial_stroke = partial_stroke.substring(0, partial_stroke.lastIndexOf("/"));
                    word = format(mDictionary.forceLookup(partial_stroke));
                }
                if (word.isEmpty()) {
                    word = format(mDictionary.forceLookup(partial_stroke));
                    if (word.isEmpty()) {
                        word="/"+rtfcre;
                        partial_stroke=rtfcre;
                    }
                }
                result += word;
                rtfcre = rtfcre.replaceAll("^"+partial_stroke+"(/)?", ""); //remove partial_stroke from start of rtfcre
            }
            result = result.replaceAll("^/",""); //remove leading slash
        }
        return result.trim();
    }

    private int unCommitStroke() {
        // take a stroke out of history, and put it back on the queue
        // return the number of backspaces to remove item
        HistoryItem item = mHistory.pop();
        Collections.addAll(mStrokeQueue, item.rtfcre().split("/"));
        return item.length();
    }

    private TranslationResult commitQueue(String translation) {
        String text = format(translation);
        mHistory.push(new HistoryItem(text.length(), strokesInQueue()));
        mStrokeQueue.clear();
        return new TranslationResult(0, text, "", "");
    }

    private TranslationResult splitQueue() {
        // commit the longest valid stroke at that start of the queue
        // and leave the remaining strokes on the queue
        TranslationResult result = BLANK_RESULT;
        Stack<String> temp = new Stack<String>();
        String strokes_in_full_queue = strokesInQueue();
        String lookupResult = mDictionary.lookup(strokesInQueue());
        while (!found(lookupResult) && !mStrokeQueue.isEmpty()) {
            temp.push(mStrokeQueue.removeLast());
            lookupResult = mDictionary.lookup(strokesInQueue());
        }
        // at this point either a lookup was found, or mStrokeQueue is empty (or both)
        if (found(lookupResult)) {
            lookupResult = forceLookup(strokesInQueue());
            result = commitQueue(lookupResult);
            // put strokes that were not used back into the queue
            mStrokeQueue.clear();
            while (!temp.isEmpty()) {
                mStrokeQueue.add(temp.pop());
            }
            result = addPreview(result);
        } else { // entire queue is not found
            result = commitQueue(strokes_in_full_queue);
        }
        return result;
    }

    private TranslationResult replayQueue() {
        TranslationResult result = BLANK_RESULT;
        Stack<String> temp = new Stack<String>();
        while (!mStrokeQueue.isEmpty()) {
            temp.push(mStrokeQueue.removeLast());
        }
        while (!temp.isEmpty()) {
            String rtfcre = temp.pop();
            result = append(result, lookup(rtfcre));
        }
        return result;
    }

    private TranslationResult append(TranslationResult a, TranslationResult b) {
        if (a==null && b==null) return BLANK_RESULT;
        if (a==null) return b;
        if (b==null) return a;
        int backspaces = a.getBackspaces();
        String text = a.getText();
        if (b.getBackspaces() > 0) {
            if (b.getBackspaces() > text.length()) {
                if (backspaces>=0) {
                    backspaces += (b.getBackspaces() - text.length());
                }
                text = "";
            } else {
                int end = text.length()-b.getBackspaces();
                text = text.substring(0, end-1);
            }
        }
        text = text + b.getText();
        return new TranslationResult(backspaces, text, getPreview(), "");
    }

    private boolean found(String lookupResult) {
        // if dictionary.lookup is not found, it returns null
        return (lookupResult != null);
    }

    private boolean no_other_options(String lookupResult) {
        // if a dictionary.lookup has other options, it returns "".  Otherwise it returns the result;
        return (lookupResult != null && !lookupResult.isEmpty());
    }

    private String strokesInQueue() {
        if (mStrokeQueue.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : mStrokeQueue) {
            sb.append(s).append("/");
        }
        return sb.substring(0, sb.lastIndexOf("/"));
    }

    private String getPreview() {
        // force lookup of strokesInQueue(),
        // and if not found, return raw strokes
        if (mStrokeQueue.isEmpty()) return "";
        return format(forceLookup(strokesInQueue()));
    }

    private TranslationResult addPreview(TranslationResult tr) {
        return new TranslationResult(tr.getBackspaces(), tr.getText(), getPreview(), tr.getExtra());
    }

    private String format(String input) {
        // Dead simple formatting (remove braces, and add space if braces were not found)
        if (input==null || input.isEmpty()) return "";
        String result = input.replace("{","").replace("}", "");
        if (!input.contains("{") && result.charAt(result.length()-1)!=' ') //don't add a 2nd space, or any space to special translations
            result += " ";
        return result;
    }

    private class HistoryItem {
        private final int mLength;
        private final String mRtfcre;

        public HistoryItem(int length, String rtfcre) {
            mLength = length;
            mRtfcre = rtfcre;
        }

        public int length() {return mLength;}
        public String rtfcre() {return mRtfcre;}
    }

}

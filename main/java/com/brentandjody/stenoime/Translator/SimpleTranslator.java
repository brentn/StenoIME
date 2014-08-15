package com.brentandjody.stenoime.Translator;


import android.content.Context;

import com.brentandjody.stenoime.StenoApp;

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

    protected final HistoryItem DUMMY_HISTORY_ITEM = new HistoryItem(0, "DUMMY", "", 0, null);

    protected boolean locked=false;
    protected boolean use_optimizer=false;
    protected Context mContext;
    protected Formatter mFormatter;
    protected Optimizer mOptimizer=null;
    protected Formatter.State mPriorState;
    protected Dictionary mDictionary=null;
    protected Stack<HistoryItem> mHistory = new Stack<HistoryItem>();
    protected Deque<String> mStrokeQueue = new ArrayDeque<String>();

    public SimpleTranslator(Context context) {
        mContext = context;
        mFormatter = new Formatter();
        if (context instanceof StenoApp)
            use_optimizer = ((StenoApp) context).isOptimizerEnabled();
    }

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public void start() {
        if (use_optimizer) {
            if (mOptimizer==null) {
                mOptimizer = new Optimizer(mContext);
                mOptimizer.initialize();
            } else {
                mOptimizer.resume();
            }
        } else {
            mOptimizer = null;
        }
        reset();
    }

    public void resume() {
        if (use_optimizer && mOptimizer!=null) {
            mOptimizer.resume();
        }
        reset();
    }

    public void pause() {
        if (mOptimizer!=null) {
            mOptimizer.pause();
        }
    }

    public void stop() {
        if (mOptimizer!=null) {
            mOptimizer.stop();
            mOptimizer = null;
        }
    }

    public void reset() {
        mHistory.removeAllElements();
        flush();
    }

    public TranslationResult flush() {
        TranslationResult result = BLANK_RESULT;
        if (mDictionary.isLoaded()) {
            result = commitQueue(forceLookup(strokesInQueue()));
        }
        mFormatter.resetState();
        return result;
    }

    public boolean usesDictionary() {
        return true;
    }

    public void setDictionary(Dictionary dictionary) {
        mDictionary = dictionary;
    }

    public void onDictionaryLoaded() {
        if (use_optimizer && mOptimizer!=null) {
            mOptimizer.initialize();
        }
    }

    @Override
    public TranslationResult translate(Stroke stroke) {
        if (locked || stroke==null || stroke.rtfcre().isEmpty()) return BLANK_RESULT;
        mPriorState = mFormatter.getState();
        String rtfcre = stroke.rtfcre();
        TranslationResult result = BLANK_RESULT;
        if (rtfcre.equals("*")) {
            result = undo();
        } else {
            result = lookup(rtfcre);
        }
        return result;
    }

    @Override
    public TranslationResult insertIntoHistory(TranslationResult tr) {
        TranslationResult result =  flush();
        result = append(result, tr);
        mHistory.add(new HistoryItem(tr.getText().length(), "", tr.getText(), tr.getBackspaces(), mFormatter.getState()));
        return result;
    }

    //*******Private methods*****

    private TranslationResult undo() {
        // if queue is not empty, just delete an item from the queue
        // otherwise remove an item from history
        TranslationResult result = BLANK_RESULT;
        if (!mStrokeQueue.isEmpty()) {
            mStrokeQueue.removeLast();
            if (mStrokeQueue.isEmpty()) {
                mHistory.push(DUMMY_HISTORY_ITEM); //this will be removed in the next step
            }
        }
        if (mStrokeQueue.isEmpty()) {
            if (!mHistory.isEmpty()) {
                TranslationResult unCommit = unCommitTwoStrokes();
                mStrokeQueue.removeLast(); //undo the last stroke
                result = append(result, unCommit);
                result = append(result, replayQueue());
            } else {
                result = TranslationResult.deletePriorWord();
            }
        }
        return addPreview(result);
    }

    private TranslationResult lookup(String rtfcre) {
        TranslationResult result = BLANK_RESULT;
        mStrokeQueue.add(rtfcre);
        if (mDictionary==null || !mDictionary.isLoaded()) {
            result = new TranslationResult(0, "", 0, "Dictionary not loaded...", "");
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

    protected String forceLookup(String rtfcre) {
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
                    word = mDictionary.forceLookup(partial_stroke);
                    if (word == null) word="";
                }
                if (word.isEmpty()) {
                    word = mDictionary.forceLookup(partial_stroke);
                    if (word == null) {
                        word=rtfcre;
                        partial_stroke=rtfcre;
                    }
                }
                result += word+" ";
                rtfcre = rtfcre.replaceAll("^" + partial_stroke.replace("*","\\*") + "(/)?", ""); //remove partial_stroke from start of rtfcre
            }
            result = result.replaceAll("^/",""); //remove leading slash
        }
        return result;
    }

    private TranslationResult unCommitTwoStrokes() {
        // take two strokes out of history, and put them back on the queue
        // return the number of backspaces to remove them, and any redo-spaces from the first
        int backspaces = 0;
        String text = "";
        if (!mHistory.isEmpty()) {
            HistoryItem item1 = mHistory.pop();
            backspaces = item1.length() - item1.backspaces();
            if (item1.getState() != null) {
                mFormatter.restoreState(item1.getState());
            }

            if (!mHistory.isEmpty()) {
                HistoryItem item2 = mHistory.pop();
                backspaces += item2.length();
                text = spaces(item2.backspaces());
                if (item2.getState() != null) {
                    mFormatter.restoreState(item2.getState());
                }
                Collections.addAll(mStrokeQueue, item2.rtfcre().split("/"));
            } else {
                text = spaces(item1.backspaces());
            }
            Collections.addAll(mStrokeQueue, item1.rtfcre().split("/"));
        }
        return new TranslationResult(backspaces, text);
    }

    private TranslationResult commitQueue(String translation) {
        String text = mFormatter.format(translation, Formatter.ACTION.Update_State);
        String stroke = strokesInQueue();
        int bs = mFormatter.backspaces();
        mHistory.push(new HistoryItem(text.length(), stroke, text, bs, mPriorState));
        if (mOptimizer!=null)
            mOptimizer.analyze(stroke, bs, text);
        mStrokeQueue.clear();
        mPriorState = mFormatter.getState();
        return new TranslationResult(mFormatter.backspaces(), text);
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
            while (!temp.isEmpty()) {
                mStrokeQueue.add(temp.pop());
            }
//            result = addPreview(result);
        } else { // entire queue is not found
            result = commitQueue(forceLookup(strokes_in_full_queue));  //do this lookup to allow suffix folding
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

    protected TranslationResult append(TranslationResult a, TranslationResult b) {
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
                text = text.substring(0, end);
            }
        }
        text = text + b.getText();
        return new TranslationResult(backspaces, text);
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

    private TranslationResult getPreview() {
        // force lookup of strokesInQueue(),
        // and if not found, return raw strokes
        if (mStrokeQueue.isEmpty()) return BLANK_RESULT;
        String translation = forceLookup(strokesInQueue()).trim();
        // format, but don't update formatter state
        Formatter.State state = mFormatter.getState();
        String text = mFormatter.format(translation);
        int backspaces = mFormatter.backspaces();
        mFormatter.restoreState(state);
        return new TranslationResult(0, "", backspaces, text, "");
    }

    protected TranslationResult addPreview(TranslationResult tr) {
        TranslationResult preview = getPreview();
        return new TranslationResult(tr.getBackspaces(), tr.getText(), preview.getPreviewBackspaces(), preview.getPreview(), tr.getExtra());
    }

    protected String spaces(int length) {
        char[] result = new char[length];
        Arrays.fill(result, ' ');
        return new String(result);
    }


    protected class HistoryItem {
        private int mLength;
        private String mRtfcre;
        private String mText;
        private int mBackspaces;
        private Formatter.State mState;

        public HistoryItem(int length, String rtfcre, String text, int bs, Formatter.State state) {
            this.mLength = length;
            this.mRtfcre = rtfcre;
            this.mText = text;
            this.mBackspaces = bs;
            this.mState = state;
        }

        public void setLength(int length) {
            this.mLength=length;
        }
        public void setStroke(String rtfcre) {
            this.mRtfcre = rtfcre;
        }
        public void setText(String text) {this.mText = text;}

        public int length() {
            return mLength;
        }
        public String rtfcre() {
            return mRtfcre;
        }
        public String text() {
            return mText;
        }
        public int backspaces() {
            return mBackspaces;
        }
        public Formatter.State getState() {
            return mState;
        }
    }

}

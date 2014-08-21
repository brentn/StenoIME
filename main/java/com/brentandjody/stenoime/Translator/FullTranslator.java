package com.brentandjody.stenoime.Translator;


import android.content.Context;

import java.util.regex.Pattern;

/**
 * Created by brent on 01/12/13.
 * Implement as much of Plover dictionary format as possible
 */
public class FullTranslator extends SimpleTranslator {

    private EnglishRules EnglishRules;

    public FullTranslator(Context context) {
        super(context);
        EnglishRules = new EnglishRules(context);
    }

    @Override
    protected TranslationResult commitQueue(String translation) {
        String text = mFormatter.format(translation);
        String stroke = strokesInQueue();
        int bs = mFormatter.backspaces();
        mHistory.push(new HistoryItem(text.length(), stroke, text, bs, mPriorState));
        if (mOptimizer!=null)
            mOptimizer.analyze(stroke, bs, text);
        mStrokeQueue.clear();
        mPriorState = mFormatter.getState();
        TranslationResult result = new TranslationResult(mFormatter.backspaces(), text);
        if (mFormatter.wasSuffix()) {
            result = applyEnglishRules(result);
        }
        return result;
    }

    @Override
    protected String forceLookup(String rtfcre) {
        // return as much translated English as possible for a given stroke
        if (rtfcre.contains(LITERAL_KEYWORD)) {
            return rtfcre.replace(LITERAL_KEYWORD, "");
        }
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
                        word = applySuffixFolding(partial_stroke);
                        partial_stroke=rtfcre;
                    }
                }
                result += word+" ";
                rtfcre = rtfcre.replaceAll("^" + Pattern.quote(partial_stroke) + "(/)?", ""); //remove partial_stroke from start of rtfcre
            }
        }
        return result;
    }

    private TranslationResult applyEnglishRules(TranslationResult input) {
        //although the items are already in history
        //the latest stroke hasn't been committed to the screen yet.
        if (mHistory.isEmpty()) return input;
        HistoryItem suffixItem = mHistory.pop(); //this was the current suffix.  Remove it, because we will be modifying it
        String suffix = suffixItem.text();
        if (mHistory.isEmpty()) return input;
        HistoryItem item = mHistory.peek();    //leave this item in history
        String word = item.text();             //this was the root word
        int backspaces = word.length();
        mFormatter.restoreState(item.getState());
        String result = mFormatter.format(EnglishRules.bestMatch(word, suffix));
        // replace the suffix stroke in history
        mHistory.push(new HistoryItem(result.length(), suffixItem.rtfcre(), result, backspaces, suffixItem.getState()));
        return new TranslationResult(backspaces, result );
    }

    private String applySuffixFolding(String stroke) {
        // if the word is not in the dictionary, but ends with -D, -G, or -S
        // then add the appropriate suffix anyhow.
        //
        if (stroke == null) return null;
        char last_char = stroke.charAt(stroke.length()-1);
        if ("GDS".indexOf(last_char)>=0) {
            String lookup = mDictionary.forceLookup(stroke.substring(0, stroke.length() - 1));
            if (lookup != null) {
                switch (last_char) {
                    case 'G':
                        return lookup + "{^ing}";
                    case 'D':
                        return lookup + "{^ed}";
                    case 'S':
                        return lookup + "{^s}";
                }
            }
        }
//        //otherwise
//        String prefix = mDictionary.longestPrefix(stroke);
//        if (prefix.isEmpty()) {
            return stroke;
//        } else {
//            return prefix;
//        }
    }


}

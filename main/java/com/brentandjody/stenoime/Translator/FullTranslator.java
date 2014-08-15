package com.brentandjody.stenoime.Translator;


import android.content.Context;

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
    public TranslationResult translate(Stroke stroke) {
        if (locked || stroke==null || stroke.rtfcre().isEmpty()) return BLANK_RESULT;
        TranslationResult result = super.translate(stroke);
        if (stroke!=null && !stroke.rtfcre().equals("*")) {
            if (mFormatter.wasSuffix()) {
                result = append(result, applyEnglishRules(result, stroke.rtfcre()));
            }
        }
        return addPreview(result);
    }

    @Override
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
                        word = applySuffixFolding(partial_stroke);
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

    private TranslationResult applyEnglishRules(TranslationResult input, String stroke) {
        if (mHistory.isEmpty()) return input;
        HistoryItem suffixItem = mHistory.pop(); //this was the current suffix
        String suffix = suffixItem.text();
        int backspaces = suffix.length()-suffixItem.backspaces();
        if (mHistory.isEmpty()) return input;
        HistoryItem item = mHistory.pop();
        String word = item.text();             //this was the root word
        backspaces += word.length();
        String spaces = spaces(item.backspaces());
        mFormatter.restoreState(item.getState());
        String result = EnglishRules.bestMatch(word, suffix);
        mHistory.push(new HistoryItem(result.length(), item.rtfcre() + "/" + stroke, result, item.backspaces(), item.getState()));
        return new TranslationResult(backspaces, spaces + result );
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
                        return lookup + "ing";
                    case 'D':
                        return lookup + "ed";
                    case 'S':
                        return lookup + "s";
                }
            }
        }
        //otherwise
        String prefix = mDictionary.longestPrefix(stroke);
        if (prefix.isEmpty()) {
            return stroke;
        } else {
            return prefix;
        }
    }


}

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
        TranslationResult result = super.translate(stroke);
        if (stroke!=null && !stroke.rtfcre().equals("*")) {
            if (mFormatter.wasSuffix()) {
                result = applyEnglishRules(result, stroke.rtfcre());
            }
        }
        return result;
    }

    private TranslationResult applyEnglishRules(TranslationResult input, String stroke) {
        if (mHistory.isEmpty()) return input;
        String suffix = mHistory.pop().text(); //this was the current suffix
        if (mHistory.isEmpty()) return input;
        HistoryItem item = mHistory.pop();
        String word = item.text();             //this was the root word
        mFormatter.restoreState(item.getState());
        String result = EnglishRules.bestMatch(word, suffix);
        mHistory.push(new HistoryItem(result.length(), item.rtfcre() + "/" + stroke, result, item.backspaces(), item.getState()));
        return new TranslationResult(item.length(), result , getPreview(),"");
    }

}

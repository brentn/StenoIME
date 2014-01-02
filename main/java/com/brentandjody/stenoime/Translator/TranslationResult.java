package com.brentandjody.stenoime.Translator;

/**
 * Created by brent on 01/12/13.
 * A class to hold the result of the translation process
 */
public class TranslationResult {

    private final int mBackspaces;
    private final String mText;
    private final String mPreview;
    private final String mExtra;

    public TranslationResult(int backspaces, String text, String preview, String extra) {
        mBackspaces = backspaces;
        mText = text;
        mPreview = preview;
        mExtra = extra;
    }

    public int getBackspaces() {
        return mBackspaces;
    }
    public String getText() {
        return mText;
    }
    public String getPreview() {
        return mPreview;
    }
    public String getExtra() { return mExtra; }

}

package com.brentandjody.Translator;

/**
 * Created by brent on 01/12/13.
 */
public class TranslationResult {

    private final int mBackspaces;
    private final String mText;
    private final String mPreview;

    public TranslationResult(int backspaces, String text, String preview) {
        mBackspaces = backspaces;
        mText = text;
        mPreview = preview;
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

}

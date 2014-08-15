package com.brentandjody.stenoime.Translator;

/**
 * Created by brent on 01/12/13.
 * A class to hold the result of the translation process
 */
public class TranslationResult {

    private static final int DELETE_PRIOR_WORD = -1;

    private final int mBackspaces;
    private final String mText;
    private final int mPreviewBackspaces;
    private final String mPreview;
    private final String mExtra;

    public TranslationResult(int backspaces, String text) {
        mBackspaces = backspaces;
        mText = text;
        mPreviewBackspaces = 0;
        mPreview = "";
        mExtra = "";
    }

    public TranslationResult(int backspaces, String text, int preview_backspaces, String preview, String extra) {
        mBackspaces = backspaces;
        mText = text;
        mPreviewBackspaces = preview_backspaces;
        mPreview = preview;
        mExtra = extra;
    }

    public int getBackspaces() {
        return mBackspaces;
    }
    public String getText() {
        return mText;
    }
    public int getPreviewBackspaces() {return mPreviewBackspaces;}
    public String getPreview() {
        return mPreview;
    }
    public String getExtra() { return mExtra; }

    public static TranslationResult deletePriorWord() {
        return new TranslationResult(DELETE_PRIOR_WORD, "");
    }

    public boolean hasCodeToDeletePriorWord() {
        return mBackspaces==DELETE_PRIOR_WORD;
    }

}

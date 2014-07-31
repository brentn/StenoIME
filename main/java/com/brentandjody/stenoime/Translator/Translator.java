package com.brentandjody.stenoime.Translator;

import android.content.Context;

/**
 * Created by brent on 01/12/13.
 * Abstract class for Translators
 */
public abstract class Translator {

    public static enum TYPE {RawStrokes, SimpleDictionary}

    public boolean usesDictionary() {return false;};
    public void lock() {};
    public void unlock() {};
    public void reset() {};
    public void onStart(Context context) {};
    public void onStop() {};
    public TranslationResult flush() {return new TranslationResult(0, "", "", "");};
    public abstract TranslationResult translate(Stroke stroke);


}

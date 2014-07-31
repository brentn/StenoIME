package com.brentandjody.stenoime.Translator;

/**
 * Created by brent on 01/12/13.
 * Abstract class for Translators
 */
public abstract class Translator {

    public static enum TYPE {RawStrokes, SimpleDictionary}

    public boolean usesDictionary() {return false;}
    public void lock() {}
    public void unlock() {}
    public void reset() {}
    public void start() {}
    public void stop() {}
    public void pause() {}
    public void onDictionaryLoaded() {}
    public TranslationResult flush() {return new TranslationResult(0, "", "", "");}
    public abstract TranslationResult translate(Stroke stroke);


}

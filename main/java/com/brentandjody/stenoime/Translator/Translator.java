package com.brentandjody.stenoime.Translator;

/**
 * Created by brent on 01/12/13.
 * Abstract class for Translators
 */
public abstract class Translator {

    protected static final TranslationResult BLANK_RESULT = new TranslationResult(0, "", "", "");

    public static enum TYPE {RawStrokes, SimpleDictionary, FullDictionary}

    public boolean usesDictionary() {return false;}
    public void lock() {}
    public void unlock() {}
    public void reset() {}
    public void start() {}
    public void stop() {}
    public void pause() {}
    public void resume() {}
    public void setDictionary(Dictionary dictionary) {}
    public void onDictionaryLoaded() {}
    public TranslationResult flush() {return BLANK_RESULT;}
    public abstract TranslationResult translate(Stroke stroke);


}

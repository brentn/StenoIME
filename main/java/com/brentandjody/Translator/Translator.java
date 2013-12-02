package com.brentandjody.Translator;

/**
 * Created by brent on 01/12/13.
 */
public abstract class Translator {

    public static enum TYPE { STROKES, DICTIONARY }

    public abstract String translate(Stroke stroke);


}

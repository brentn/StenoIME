package com.brentandjody.Translator;

/**
 * Created by brent on 01/12/13.
 * Simulate a paper tape output for a steno machine
 */
public class RawStrokeTranslator extends Translator {

    private boolean locked=false;

    @Override
    public boolean usesDictionary() {
        return false;
    }

    @Override
    public void lock() {
        locked=true;
    }

    @Override
    public void unlock() {
        locked=false;
    }

    @Override
    public TranslationResult submitQueue() {
        return new TranslationResult(0, "", "", "");
    }

    @Override
    public TranslationResult translate(Stroke stroke) {
        if (locked) return null;
        if (stroke==null || stroke.rtfcre().equals("")) return new TranslationResult(0, "", "", "");
        StringBuilder sb = new StringBuilder();

        for (String s : Stroke.STENO_KEYS) {
            if (stroke.getKeySet().contains(s)) {
                sb.append(s.replace("-",""));
            } else {
                sb.append("_");
            }
        }
        sb.append("\n");
        return new TranslationResult(0, sb.toString(), null, null);
    }
}

package com.brentandjody.Translator;

/**
 * Created by brent on 01/12/13.
 */
public class RawStrokeTranslator extends Translator {

    @Override
    public String translate(Stroke stroke) {
        StringBuilder sb = new StringBuilder();

        for (String s : Stroke.STENO_KEYS) {
            if (stroke.getKeys().contains(s)) {
                sb.append(s.replace("-",""));
            } else {
                sb.append("_");
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}

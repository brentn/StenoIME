package com.brentandjody.stenoime.Translator;

/**
 * Created by brent on 21/07/14.
 */
public class Optimizer {
    private static TST<String> thesaurus = new TST<String>();

    public Optimizer(Dictionary dictionary) {
        String translation, existing_stroke;
        for (String stroke : dictionary.allKeys()) {
            translation = dictionary.lookup(stroke);
            if (translation != null) {
                existing_stroke = thesaurus.get(translation);
                thesaurus.put(translation, shorterOf(existing_stroke, stroke));
            }
        }
    }

    public String findBetterStroke(String stroke, String translation) {
        String bestStroke = thesaurus.get(translation);
        if (countStrokes(bestStroke) < countStrokes(stroke)) {
            return bestStroke;
        }
        return null;
    }

    private String shorterOf(String s1, String s2) {
        //Returns the shorter of s1 and s2, based on number of strokes, or if equal, number of keys in strokes.
        //or if equal, returns s1;
        if (s1==null) return s2;
        if (s2==null) return s1;
        int c1, c2;
        c1 = countStrokes(s1);
        c2 = countStrokes(s2);
        if (c1 < c2) return s1;
        if (c2 < c1) return s2;
        if (s2.length() < s1.length()) return s2;
        return s1;
    }

    private int countStrokes(String s) {
        return s.length()-s.replace("/","").length();
    }
}

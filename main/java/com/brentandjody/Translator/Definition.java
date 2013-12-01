package com.brentandjody.Translator;


/**
 * Created by brent on  13/11/13.
 * This is the main translation class
 * and represents a group of strokes, and the translation they represents
 */
public class Definition {

    private final Stroke[] strokes;
    private final String english;
    private State formatting;

    @Override
    public String toString() {
        return "Definition(" + rtfcre() + " : " + english + ")";
    }

    public Definition(String outlineString, String translation) {
        strokes = Stroke.separate(outlineString);
        english = translation;
        formatting = new State();
        if (translation!=null)
            formatting.addBackspaces(translation.length());
    }

    public Definition(Stroke[] outline, String translation) {
        // outline: a series of stroke objects
        // translation: a translation for the outline, or null
        strokes = outline;
        english = translation;
        formatting = new State();
        if (translation!=null)
            formatting.addBackspaces(translation.length());
    }

    public Stroke[] strokes() {
        return strokes;
    }

    public String english() {
        return english;
    }

    public String rtfcre() {
        return Stroke.combine(strokes);
    }

    public void setFormatting(State f) {
        formatting = f;
    }

    public State getFormatting() {
        return formatting;
    }

}


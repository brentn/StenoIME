package com.brentandjody.Translator;


/**
 * Created by brent on  13/11/13.
 * This is the main translation class
 * and represents a group of strokes, and the translation they represents
 */
public class Definition {

    public static enum CAPITALIZATION {NORMAL, UPPERCASE, LOWERCASE};

    private final Stroke[] strokes;
    private final String english;

    // state
    private CAPITALIZATION capitalization = CAPITALIZATION.NORMAL;
    private boolean attachedStart = false;
    private boolean attachedEnd = false;
    private boolean glue = false;

    @Override
    public String toString() {
        return "Definition(" + rtfcre() + " : " + english + ")";
    }

    public Definition(String outlineString, String translation) {
        strokes = Stroke.separate(outlineString);
        english = translation;
    }

    public Definition(Stroke[] outline, String translation) {
        // outline: a series of stroke objects
        // translation: a translation for the outline, or null
        strokes = outline;
        english = translation;
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

    public void setCapitalization (CAPITALIZATION c) {
        capitalization = c;
    }
    public void attachStart() {
        attachedStart =true;
    }
    public void attachEnd() {
        attachedEnd =true;
    }
    public void addGlue() {
        glue=true;
    }

    public CAPITALIZATION getCapitalization() {
        return capitalization;
    }
    public boolean isAttachedStart() {
        return attachedStart;
    }
    public boolean isAttachedEnd() {
        return attachedEnd;
    }
    public boolean hasGlue() {
        return glue;
    }

}


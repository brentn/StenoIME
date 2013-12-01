package com.brentandjody.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.Translator.Definition;
import com.brentandjody.Translator.Stroke;

import java.util.Arrays;
import java.util.HashSet;


public class DefinitionTest extends AndroidTestCase {

    private static final Stroke S = new Stroke(new HashSet<String>() {{add("S-");}});
    private static final Stroke T = new Stroke(new HashSet<String>() {{add("T-");}});

    private Stroke[] strokeResult;

    public void setUp() throws Exception {
        super.setUp();
        strokeResult = new Stroke[2]; strokeResult[0]=S; strokeResult[1]=T;
    }

    public void testNoTranslation() throws Exception {
        Definition t = new Definition(strokeResult, null);
        assertTrue(Arrays.equals(strokeResult, t.strokes()));
        assertEquals("S/T", t.rtfcre());
        assertNull(t.english());
    }

    public void testTranslation() throws Exception {
        Definition t = new Definition(strokeResult, "translation");
        assertTrue(Arrays.equals(strokeResult, t.strokes()));
        assertEquals("S/T", t.rtfcre());
        assertEquals("translation", t.english());
    }

}
package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.stenoime.Translator.Orthography;

/**
 * Created by brentn on 16/01/14.
 */
public class OrthographyTest extends AndroidTestCase {

    public void testOrthography() throws Exception {
        Orthography mOrthography = new Orthography();
        assertEquals("artistically", mOrthography.match("artistic", "ly"));
        assertEquals("establishes", mOrthography.match("establish", "s"));
        assertEquals("speeches", mOrthography.match("speech", "s"));
        assertEquals("cherries", mOrthography.match("cherry", "s"));
        assertEquals("dying", mOrthography.match("die", "ing"));
        assertEquals("metallurgist", mOrthography.match("metallurgy", "ist"));
        assertEquals("beautiful", mOrthography.match("beauty", "ful"));
        assertEquals("narrating", mOrthography.match("narrate", "ing"));
        assertEquals("deferred", mOrthography.match("defer", "ed"));
    }
}
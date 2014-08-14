package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.stenoime.Translator.EnglishRules;

/**
 * Test Created by brentn on 16/01/14.
 */
public class englishRulesTest extends AndroidTestCase {

    public void testOrthography() throws Exception {
        EnglishRules mSuffixes = new EnglishRules(getContext());
        assertEquals("artistically ", mSuffixes.bestMatch("artistic", "ly"));
        assertEquals("establishes ", mSuffixes.bestMatch("establish", "s"));
        assertEquals("speeches ", mSuffixes.bestMatch("speech", "s"));
        assertEquals("cherries ", mSuffixes.bestMatch("cherry", "s"));
        assertEquals("dying ", mSuffixes.bestMatch("die", "ing"));
        assertEquals("metallurgist ", mSuffixes.bestMatch("metallurgy", "ist"));
        assertEquals("beautiful ", mSuffixes.bestMatch("beauty", "ful"));
        assertEquals("narrating ", mSuffixes.bestMatch("narrate", "ing"));
        assertEquals("deferred ", mSuffixes.bestMatch("defer", "ed"));
        assertEquals("noncombustible ", mSuffixes.bestMatch("noncombust", "able"));
        //this should produce multiple scored resluts
        assertEquals("publicly ", mSuffixes.bestMatch("public", "ly"));
    }

}
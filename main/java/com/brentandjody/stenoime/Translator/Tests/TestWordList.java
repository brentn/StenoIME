package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.stenoime.Translator.WordList;

/**
 * Created by brentn on 18/01/14.
 */
public class TestWordList extends AndroidTestCase {

    private WordList list;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new WordList(getContext());
    }

    public void testContains() throws Exception {
        assertFalse(list.contains(""));
        assertFalse(list.contains("frapbob"));
        assertTrue(list.contains("hilly"));
    }

    public void testGet() throws Exception {
        assertEquals(-1, list.score(""));
        assertEquals(-1, list.score("ssiif"));
        assertEquals(55, list.score("christian"));
        assertEquals(10, list.score("Christian"));
        assertEquals(50, list.score("dirigible"));
    }
}

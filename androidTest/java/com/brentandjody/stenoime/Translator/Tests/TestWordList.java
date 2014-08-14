package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.stenoime.data.WordListHelper;

/**
 * Created by brentn on 18/01/14.
 * Test the word list
 */
public class TestWordList extends AndroidTestCase {

    private WordListHelper list;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        list = new WordListHelper(getContext());
    }

    public void testContains() throws Exception {
        assertFalse(list.contains(""));
        assertFalse(list.contains("frapbob"));
        assertTrue(list.contains("hilly"));
    }

    public void testGet() throws Exception {
        assertEquals(WordListHelper.NOT_FOUND, list.score(""));
        assertEquals(WordListHelper.NOT_FOUND, list.score("ssiif"));
        assertEquals(55, list.score("christian"));
        assertEquals(10, list.score("Christian"));
        assertEquals(50, list.score("dirigible"));
    }

    public void testScore() throws Exception {
        assertEquals(WordListHelper.NOT_FOUND, list.score("availible"));
        assertEquals(10, list.score("available"));
    }
}

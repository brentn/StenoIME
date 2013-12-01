package com.brentandjody.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.Translator.TST;

import java.util.LinkedList;
import java.util.Queue;

public class TSTTest extends AndroidTestCase {

    private TST<String> dictionary;

    public void setUp() throws Exception {
        super.setUp();
        dictionary = new TST<String>();
    }

    public void testSize() throws Exception {
        assertEquals(0, dictionary.size());
        dictionary.put("able", "");
        assertEquals(1, dictionary.size());
        dictionary.put("beta", "");
        assertEquals(2, dictionary.size());
        //inserting a duplicate key should not increase the size
        dictionary.put("able", "");
        assertEquals(2, dictionary.size());
    }

    public void testContains() throws Exception {
        assertFalse(dictionary.contains("able"));
        dictionary.put("able", "");
        assertTrue(dictionary.contains("able"));
        assertFalse(dictionary.contains("beta"));
    }

    public void testPutAndGet() throws Exception {
        assertNull(dictionary.get("alpha"));
        dictionary.put("alpha", "A");
        dictionary.put("bravo", "B");
        dictionary.put("alphabet", "C");
        assertEquals("A", dictionary.get("alpha"));
        assertEquals("B", dictionary.get("bravo"));
        assertNull(dictionary.get("Alpha"));
        dictionary.put("bravo", "D");
        assertEquals("D", dictionary.get("bravo"));
        assertEquals("C", dictionary.get("alphabet"));
    }

    public void testLongestPrefixOf() throws Exception {
        assertEquals("", dictionary.longestPrefixOf("bob"));
        dictionary.put("ab", "1");
        dictionary.put("able", "2");
        assertEquals("ab", dictionary.longestPrefixOf("abracadabra"));
        assertEquals("able", dictionary.longestPrefixOf("able"));
        dictionary.put("a", "3");
        assertEquals("", dictionary.longestPrefixOf("finger"));
    }

    public void testKeys() throws Exception {
        Queue<String> result = new LinkedList<String>();
        assertEquals(result, dictionary.keys());
        dictionary.put("1", "one");
        result.add("1");
        assertEquals(result, dictionary.keys());
        dictionary.put("2", "two");
        result.add("2");
        assertEquals(result, dictionary.keys());
    }

    public void testPrefixMatch() throws Exception {
        Queue<String> result = new LinkedList<String>();
        assertEquals(result, dictionary.prefixMatch("a"));
        dictionary.put("gun", "1");
        dictionary.put("glue", "2");
        dictionary.put("pea", "3");
        result.add("glue");
        result.add("gun");
        assertEquals(result, dictionary.prefixMatch("g"));
        result.clear();
        assertEquals(result, dictionary.prefixMatch("3"));
    }

}

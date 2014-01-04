package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.stenoime.Translator.Formatter;


import java.util.List;

public class FormatterTest extends AndroidTestCase {
    public void testFormat() throws Exception {
        Formatter formatter = new Formatter();
        // regular text
        assertEquals("", formatter.format(null));
        assertEquals("", formatter.format(""));
        assertEquals("Bob ", formatter.format("Bob"));
        // special keys
        assertEquals("\n", formatter.format("{#Return}"));
        assertEquals("\b", formatter.format("{#BackSpace}"));
        assertEquals("\t", formatter.format("{#Tab}"));
        assertEquals("\bspin ", formatter.format("{^spin}"));
        assertEquals("cycle", formatter.format("{cycle^}"));
        // flags
        assertFalse(formatter.hasQueue());
        assertEquals("", formatter.format("{-|}"));
        assertTrue(formatter.hasQueue());
        assertEquals("Hobby ", formatter.format("hobby"));
        assertFalse(formatter.hasQueue());
        formatter.format("{>}");
        assertEquals("cRAYON ", formatter.format("CRAYON"));
        assertEquals("a ", formatter.format("{&a}"));
        assertEquals("\bb ", formatter.format("{&b}"));
        assertEquals("\bc ", formatter.format("{&c"));
        assertTrue(formatter.hasQueue());
        assertEquals("d ", formatter.format("d"));
        assertFalse(formatter.hasQueue());
        // combinations
        assertEquals("\n  ", formatter.format("{#Return}{  }{-|}"));
        assertTrue(formatter.hasQueue());
        assertEquals("Canada ", formatter.format("canada"));






    }

    public void testHasQueue() throws Exception {
        Formatter formatter = new Formatter();
        assertFalse(formatter.hasQueue());
        formatter.format("{#Return}");
        assertFalse(formatter.hasQueue());
        formatter.format("{-|}");
        assertTrue(formatter.hasQueue());
    }

    public void testRemoveItemFromQueue() throws Exception {
        Formatter formatter = new Formatter();
        assertFalse(formatter.hasQueue());
        formatter.format("{-|}");
        formatter.format("{&e");
        assertTrue(formatter.hasQueue());
        formatter.removeItemFromQueue();
        assertFalse(formatter.hasQueue());
    }

    public void testBreakApart() throws Exception {
        Formatter formatter = new Formatter();
        List<String> result = formatter.breakApart("");
        assertEquals(0, result.size());
        result = formatter.breakApart("west");
        assertEquals(1, result.size());
        assertEquals("west", result.get(0));
        result = formatter.breakApart("{x}");
        assertEquals(1, result.size());
        assertEquals("{x}", result.get(0));
        result = formatter.breakApart("abc{DEF}g");
        assertEquals(3, result.size());
        assertEquals("abc", result.get(0));
        assertEquals("{DEF}", result.get(1));
        assertEquals("g", result.get(2));
    }

}

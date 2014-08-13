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
        assertEquals("", formatter.format("{#BackSpace}"));
        assertEquals(1, formatter.backspaces());
        assertEquals("\t", formatter.format("{#Tab}"));
        assertEquals("spin ", formatter.format("{^spin}"));
        assertEquals(0, formatter.backspaces());
        assertEquals("cycle", formatter.format("{cycle^}"));
        // flags
        assertFalse(formatter.hasFlags());
        assertEquals("", formatter.format("{-|}"));
        assertTrue(formatter.hasFlags());
        assertEquals("Hobby ", formatter.format("hobby"));
        assertFalse(formatter.hasFlags());
        formatter.format("{>}");
        assertEquals("cRAYON ", formatter.format("CRAYON"));
        assertEquals("a ", formatter.format("{&a}"));
        assertEquals("b ", formatter.format("{&b}"));
        assertEquals(1, formatter.backspaces());
        assertEquals("c ", formatter.format("{&c"));
        assertEquals(1, formatter.backspaces());
        assertTrue(formatter.hasFlags());
        assertEquals("d ", formatter.format("d"));
        assertFalse(formatter.hasFlags());
        // combinations
        assertEquals("\n  ", formatter.format("{#Return}{  }{-|}"));
        assertTrue(formatter.hasFlags());
        assertEquals("Canada ", formatter.format("canada"));
    }

    public void testIsSuffix() throws Exception {
        Formatter formatter = new Formatter();
        formatter.format("abc");
        assertFalse(formatter.wasSuffix());
        formatter.format("^x");
        assertFalse(formatter.wasSuffix());
        formatter.format("{^a}");
        assertTrue(formatter.wasSuffix());
        formatter.format("{^}");
        assertFalse(formatter.wasSuffix());
        formatter.format ("{^ ^}");
        assertFalse(formatter.wasSuffix());
        formatter.format("{^alnlnf");
        assertTrue(formatter.wasSuffix());
    }

    public void testHasQueue() throws Exception {
        Formatter formatter = new Formatter();
        assertFalse(formatter.hasFlags());
        formatter.format("{#Return}");
        assertFalse(formatter.hasFlags());
        formatter.format("{-|}");
        assertTrue(formatter.hasFlags());
    }

    public void testRemoveItemFromQueue() throws Exception {
        Formatter formatter = new Formatter();
        assertFalse(formatter.hasFlags());
        formatter.format("{-|}");
        formatter.format("{&e");
        assertTrue(formatter.hasFlags());
        formatter.removeItemFromQueue();
        assertFalse(formatter.hasFlags());
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

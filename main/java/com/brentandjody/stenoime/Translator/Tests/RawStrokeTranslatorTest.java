package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;

import com.brentandjody.stenoime.Translator.RawStrokeTranslator;
import com.brentandjody.stenoime.Translator.Stroke;

public class RawStrokeTranslatorTest extends AndroidTestCase {

    public void testUsesDictionary() throws Exception {
        RawStrokeTranslator translator = new RawStrokeTranslator();
        assertFalse(translator.usesDictionary());
    }

    public void testLockAndUnlock() throws Exception {
        RawStrokeTranslator translator = new RawStrokeTranslator();
        assertEquals("___________________T___\n", translator.translate(new Stroke("-T")).getText());
        translator.lock();
        assertNull(translator.translate(new Stroke("-T")));
        translator.unlock();
        assertEquals("___________________T___\n", translator.translate(new Stroke("-T")).getText());
    }

    public void testTranslate() throws Exception {
        RawStrokeTranslator translator = new RawStrokeTranslator();
        assertEquals("", translator.translate(null).getText());
        assertEquals("", translator.translate(new Stroke("")).getText());
        assertEquals("__T____RAO__U__PB______\n", translator.translate(new Stroke("TRAOUPB")).getText());
    }
}

package com.brentandjody.Translator.Tests;

import android.test.AndroidTestCase;
import android.widget.ProgressBar;

import com.brentandjody.Translator.Dictionary;
import com.brentandjody.Translator.SimpleTranslator;
import com.brentandjody.Translator.Stroke;
import com.brentandjody.Translator.TranslationResult;

public class SimpleTranslatorTest extends AndroidTestCase {

    public void testUsesDictionary() throws Exception {
        SimpleTranslator translator = new SimpleTranslator();
        assertTrue(translator.usesDictionary());
    }

    public void testLockAndUnlock() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator();
        final Dictionary dictionary = new Dictionary(getContext());
        dictionary.load("test.json", new ProgressBar(getContext()), 10);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                translator.setDictionary(dictionary);
                assertEquals("adds", translator.translate(new Stroke("ADZ")).getPreview());
                translator.lock();
                assertEquals("",translator.translate(new Stroke("ADZ")).getPreview());
                translator.unlock();
                assertEquals("adds", translator.translate(new Stroke("ADZ")).getPreview());

            }
        });
    }


    public void testTranslate() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator();
        final Dictionary dictionary = new Dictionary(getContext());
        dictionary.load("test.json", new ProgressBar(getContext()), 10);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                translator.setDictionary(dictionary);
                // null
                checkResults(translator.translate(null), 0, "", "");
                // not found (with & without queue)
                checkResults(translator.translate(new Stroke("-T")), 0, "-T ", "");
                checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds");
                checkResults(translator.translate(new Stroke("PHEUT")), 0, "admit ", "");
                // deterministic (with & without queue)
                checkResults(translator.translate(new Stroke("AEFLD")), 0, "realized ", "");
                checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds");
                checkResults(translator.translate(new Stroke("AEFLD")), 0, "adds realized ", "");
                // ambiguous (with & without queue)
                checkResults(translator.translate(new Stroke("AD")), 0, "", "AD");
                checkResults(translator.translate(new Stroke("TKRESZ")), 0, "", "address");
                checkResults(translator.translate(new Stroke("SAOE")), 0, "addressee ", "");
                // endings (with & without queue
                checkResults(translator.translate(new Stroke("AD")), 0, "", "AD");
                checkResults(translator.translate(new Stroke("ULT")), 0, "adult ", "");
                checkResults(translator.translate(new Stroke("-g")), 0, "\bing ", "");
                checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds");
                checkResults(translator.translate(new Stroke("HREU")), 0, "addsly ", "");
                checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds");
                checkResults(translator.translate(new Stroke("PHEUT")), 0, "admit ", "");
                checkResults(translator.translate(new Stroke("-D")), 0, "\bed ", "");
                // undo (with & without queue)
                checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds");
                checkResults(translator.translate(new Stroke("*")), 0, "", "");
                checkResults(translator.translate(new Stroke("AEFLD")), 0, "realized ", "");
                checkResults(translator.translate(new Stroke("*")), 9, "", "");
                checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds");
                checkResults(translator.translate(new Stroke("HREU")), 0, "addsly ", "");
                checkResults(translator.translate(new Stroke("*")), 2, "", "");
                checkResults(translator.translate(new Stroke("*")), 5, "", "");
            }
        });
    }

    private void checkResults(TranslationResult result, int bs, String text, String preview) {
        assertEquals(bs, result.getBackspaces());
        assertEquals(text, result.getText());
        assertEquals(preview, result.getPreview());
    }
}

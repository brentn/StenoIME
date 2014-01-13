package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;
import android.util.Log;
import android.widget.ProgressBar;

import com.brentandjody.stenoime.Translator.Dictionary;
import com.brentandjody.stenoime.Translator.SimpleTranslator;
import com.brentandjody.stenoime.Translator.Stroke;
import com.brentandjody.stenoime.Translator.TranslationResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public class SimpleTranslatorTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        InputStream in = getContext().getAssets().open("test.json");
        File outFile = new File("/sdcard", "test.json");
        OutputStream out = new FileOutputStream(outFile);
        copyFile(in, out);
        in.close();
        out.flush();
        out.close();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        File outFile = new File("/sdcard", "test.json");
        outFile.delete();
    }


    public void testUsesDictionary() throws Exception {
        SimpleTranslator translator = new SimpleTranslator();
        assertTrue(translator.usesDictionary());
    }

    public void testLockAndUnlock() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator();
        final Dictionary dictionary = new Dictionary(getContext());
        dictionary.load(new String[] {"/sdcard/test.json"}, null, new ProgressBar(getContext()), 10);
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        translator.setDictionary(dictionary);
        assertEquals("adds ", translator.translate(new Stroke("ADZ")).getPreview());
        translator.lock();
        assertEquals("",translator.translate(new Stroke("ADZ")).getPreview());
        translator.unlock();
        assertEquals("adds ", translator.translate(new Stroke("ADZ")).getPreview());
    }


    public void testTranslate() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator();
        final Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/test.json"}, null, new ProgressBar(getContext()), 10);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        translator.setDictionary(dictionary);
        // null
        checkResults(translator.translate(null), 0, "", "");
        // not found (with & without queue)
        checkResults(translator.translate(new Stroke("-T")), 0, "-T ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("PHEUT")), 0, "admit ", "");
        // deterministic (with & without queue)
        checkResults(translator.translate(new Stroke("AEFLD")), 0, "realized ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("AEFLD")), 0, "adds realized ", "");
        // ambiguous (with & without queue)
        checkResults(translator.translate(new Stroke("AD")), 0, "", "AD");
        checkResults(translator.translate(new Stroke("TKRESZ")), 0, "", "address ");
        checkResults(translator.translate(new Stroke("SAOE")), 0, "addressee ", "");
        // endings (with & without queue
        checkResults(translator.translate(new Stroke("AD")), 0, "", "AD");
        checkResults(translator.translate(new Stroke("ULT")), 0, "adult ", "");
        checkResults(translator.translate(new Stroke("-G")), 1, "ing ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("HREU")), 0, "addsly ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("PHEUT")), 0, "admit ", "");
        checkResults(translator.translate(new Stroke("-D")), 1, "ed ", "");
        // undo (with & without queue)
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        checkResults(translator.translate(new Stroke("AEFLD")), 0, "realized ", "");
        checkResults(translator.translate(new Stroke("*")), 12, " ", "ed ");
        checkResults(translator.translate(new Stroke("ADZ")), 1, "ed ", "adds ");
        checkResults(translator.translate(new Stroke("HREU")), 0, "addsly ", "");
        checkResults(translator.translate(new Stroke("*")), 7, "", "adds ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        checkResults(translator.translate(new Stroke("EUPL")), 0, "", "im");
        checkResults(translator.translate(new Stroke("PHORT")), 0, "", "EUPL/PHORT");
        checkResults(translator.translate(new Stroke("AL")), 0, "immortal ", "");
        checkResults(translator.translate(new Stroke("*")), 9, "", "EUPL/PHORT");
        checkResults(translator.translate(new Stroke("*")), 0, "", "im");
        checkResults(translator.translate(new Stroke("PHORT")), 0, "", "EUPL/PHORT");
        checkResults(translator.translate(new Stroke("AL")), 0, "immortal ", "");
        checkResults(translator.translate(new Stroke("-PL")), 0, "", ".  ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        // test period and subsequent capital with undo
        checkResults(translator.translate(new Stroke("-PL")), 0, "", ".  ");
        checkResults(translator.translate(new Stroke("THAPBG")), 1, ".  Thank ", "");
        checkResults(translator.translate(new Stroke("U")), 0, "you ", "");
        checkResults(translator.translate(new Stroke("*")), 10, "", "Thank ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "Adds ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        checkResults(translator.translate(new Stroke("*")), 11, "", "immortal ");
        checkResults(translator.translate(new Stroke("THAPBG")), 0, "immortal thank ", "");
        checkResults(translator.translate(new Stroke("-FL")), 1, "ful ", "");
        checkResults(translator.translate(new Stroke("-PBS")), 1, "ness ", "");
        checkResults(translator.translate(new Stroke("*")), 8, " ", "ful ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        checkResults(translator.translate(new Stroke("*")), 15, "", "immortal ");
        // test fingerspelling and undo
        checkResults(translator.translate(new Stroke("A*")), 0, "immortal a ", "");
        checkResults(translator.translate(new Stroke("PW*")), 1, "b ", "");
        checkResults(translator.translate(new Stroke("KR*")), 1, "c ", "");
        checkResults(translator.translate(new Stroke("*")), 3, " ", "b ");
        checkResults(translator.translate(new Stroke("*")), 0, "", "");
        // caps should persist through enter
        checkResults(translator.translate(new Stroke("THAPBG")), 0, "thank ", "");
        checkResults(translator.translate(new Stroke("KPA")), 0, "", "");
        checkResults(translator.translate(new Stroke("U")), 0, "You ", "");
        checkResults(translator.translate(new Stroke("-PL")), 0, "", ".  ");
        checkResults(translator.translate(new Stroke("R-R")), 1, ".  \n", "");
        checkResults(translator.translate(new Stroke("THAPBG")), 0, "Thank ", "");
    }

    public void testSpecialCases() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator();
        final Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/test.json"}, null, new ProgressBar(getContext()), 10);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        translator.setDictionary(dictionary);
        // special cases
        checkResults(translator.translate(new Stroke("EU")), 0, "", "I ");
        checkResults(translator.translate(new Stroke("APL")), 0, "I ", "am ");
        checkResults(translator.translate(new Stroke("PWEUG")), 0, "", "APL/PWEUG");
        checkResults(translator.translate(new Stroke("S-P")), 0, "am big ", "");
    }

    private void checkResults(TranslationResult result, int bs, String text, String preview) {
        Log.w("TEST", bs+"="+result.getBackspaces()+" "+text+"="+result.getText()+" "+preview+"="+result.getPreview());
        assertEquals(bs, result.getBackspaces());
        assertEquals(preview, result.getPreview());
        assertEquals(text, result.getText());
    }
}


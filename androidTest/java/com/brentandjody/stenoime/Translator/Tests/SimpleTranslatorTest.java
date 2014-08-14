package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;
import android.util.Log;
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
        SimpleTranslator translator = new SimpleTranslator(getContext());
        assertTrue(translator.usesDictionary());
    }

    public void testLockAndUnlock() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator(getContext());
        final Dictionary dictionary = new Dictionary(getContext());
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 10);
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
        final SimpleTranslator translator = new SimpleTranslator(getContext());
        final Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 10);
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
        checkResults(translator.translate(new Stroke("-B")), 0, "-B ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("PHEUT")), 0, "", "admit ");
        checkResults(translator.translate(new Stroke("-D")), 0, "admitted ", "");
        // deterministic (with & without queue)
        checkResults(translator.translate(new Stroke("AEFLD")), 0, "realized ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("AEFLD")), 0, "adds realized ", "");
        // ambiguous (with & without queue)
        checkResults(translator.translate(new Stroke("AD")), 0, "", "AD ");
        checkResults(translator.translate(new Stroke("TKRESZ")), 0, "", "address ");
        checkResults(translator.translate(new Stroke("SAOE")), 0, "addressee ", "");
        // endings (with & without queue
        checkResults(translator.translate(new Stroke("AD")), 0, "", "AD ");
        checkResults(translator.translate(new Stroke("ULT")), 0, "adult ", "");
        checkResults(translator.translate(new Stroke("-G")), 1, "ing ", "");
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");
        checkResults(translator.translate(new Stroke("HREU")), 0, "adds ", "ly ");
        checkResults(translator.translate(new Stroke("ADZ")), 1, "ly ", "adds ");
        checkResults(translator.translate(new Stroke("PHEUT")), 0, "", "admit ");
        checkResults(translator.translate(new Stroke("-D")), 0, "admitted ", "");
        // undo (with & without queue)
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");      //admitted adds
        checkResults(translator.translate(new Stroke("*")), 9, "admitted ", "");    //admitted
        checkResults(translator.translate(new Stroke("AEFLD")), 0, "realized ", "");//admitted realized
        checkResults(translator.translate(new Stroke("*")), 18, "admitted ", "");   //admitted
        checkResults(translator.translate(new Stroke("ADZ")), 0, "", "adds ");      //admitted adds
        checkResults(translator.translate(new Stroke("HREU")), 0, "adds ", "ly ");  //admitted addsly
        checkResults(translator.translate(new Stroke("*")), 5, "", "adds ");        //admitted adds
        checkResults(translator.translate(new Stroke("*")), 9, "admitted ", "");   //admitted
        checkResults(translator.translate(new Stroke("EUPL")), 0, "", "im");        //admitted im
        checkResults(translator.translate(new Stroke("PHORT")), 0, "", "im /PHORT "); //admitted im/PHORT
        checkResults(translator.translate(new Stroke("AL")), 0, "immortal ", "");   //admitted immortal
        checkResults(translator.translate(new Stroke("*")), 18, "admitted ", "im /PHORT ");     //admitted im/PHORT
        checkResults(translator.translate(new Stroke("*")), 0, "", "im");           //admitted im
        checkResults(translator.translate(new Stroke("PHORT")), 0, "", "im /PHORT "); //admitted im/PHORT
        checkResults(translator.translate(new Stroke("AL")), 0, "immortal ", "");   //admitted immortal
        checkResults(translator.translate(new Stroke("-PL")), 0, "", ". ");        //admitted immortal.
        checkResults(translator.translate(new Stroke("*")), 9, "immortal ", "");   //admitted immortal

        //Numbers
        checkResults(translator.translate(new Stroke("#S")), 0, "1 ", "");
        checkResults(translator.translate(new Stroke("#S-T")), 1, "19 ", "");
        checkResults(translator.translate(new Stroke("*")), 4, "1 ", "");
        checkResults(translator.translate(new Stroke("#TO")), 1, "20 ", "");
        checkResults(translator.translate(new Stroke("U")), 0, "", "you ");
        checkResults(translator.translate(new Stroke("#H")), 0, "you 4 ", "");
        checkResults(translator.translate(new Stroke("AUG")), 0, "", "August ");
        checkResults(translator.translate(new Stroke("#TO")), 0, "August 20 ", "");
        checkResults(translator.translate(new Stroke("#SH")), 1, "14 ", "");
        //word interrupted by delete
        checkResults(translator.translate(new Stroke("EBGS")), 0, "", "ex-");
        checkResults(translator.translate(new Stroke("KHAEUBG")), 0, "exchange ", "");
        checkResults(translator.translate(new Stroke("EBGS")), 0, "", "ex-");
        checkResults(translator.translate(new Stroke("T")), 0, "ex-", "it ");
        checkResults(translator.translate(new Stroke("*")), 3, "", "ex-");
        checkResults(translator.translate(new Stroke("KHAEUBG")), 0, "exchange ", "");
        //
        checkResults(translator.translate(new Stroke("A")), 0, "", "a");
        checkResults(translator.translate(new Stroke("SRAEUL")), 0, "", "avail ");
        checkResults(translator.translate(new Stroke("-BL")), 0, "available ", "");
    }

    public void estRealDictionary() throws Exception {
        InputStream in = getContext().getAssets().open("dict.json");
        File outFile = new File("/sdcard", "dict.json");
        OutputStream out = new FileOutputStream(outFile);
        copyFile(in, out);
        in.close();
        out.flush();
        out.close();
        final SimpleTranslator translator = new SimpleTranslator(getContext());
        final Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/dict.json"}, null, 10);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        translator.setDictionary(dictionary);
        // How should multi-syllable words appear as you are typing them?
        checkResults(translator.translate(new Stroke("KWRE")), 0, "", "yes "); //KWRE+""
        checkResults(translator.translate(new Stroke("EU")), 0, "", "yes I ");
        checkResults(translator.translate(new Stroke("TKO")), 0, "yes I ", "do ");
        // Test delete spacing
        checkResults(translator.translate(new Stroke("TKPWO*D")), 0, "do ", "God ");
        checkResults(translator.translate(new Stroke("AES")), 0, "God ", "'s ");
        checkResults(translator.translate(new Stroke("TRAO*UT")), 1, "'s ", "truth ");
        checkResults(translator.translate(new Stroke("*")), 3, "", "'s ");
        checkResults(translator.translate(new Stroke("*")), 4, "", "God ");

        checkResults(translator.translate(new Stroke("#H")), 0, "God 4 ", "");
        checkResults(translator.translate(new Stroke("AUG")), 0, "", "August ");
        checkResults(translator.translate(new Stroke("#TO")), 0, "August 20 ", "");
        checkResults(translator.translate(new Stroke("#SH")), 0, "14 ", "");
        outFile.delete();
    }

    public void testSpecialCases() throws Exception {
        final SimpleTranslator translator = new SimpleTranslator(getContext());
        final Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 10);
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
        checkResults(translator.translate(new Stroke("PWEUG")), 0, "", "am big ");
        checkResults(translator.translate(new Stroke("S-P")), 0, "am big ", "");
    }

    private void checkResults(TranslationResult result, int bs, String text, String preview) {
        Log.w("TEST", bs+"="+result.getBackspaces()+" "+text+"="+result.getText()+" "+preview+"="+result.getPreview());
        assertEquals(bs, result.getBackspaces());
        assertEquals(text, result.getText());
        assertEquals(preview, result.getPreview());
    }
}


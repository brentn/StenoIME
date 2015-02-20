package com.brentandjody.stenoime.Translator.Tests;

import android.test.AndroidTestCase;
import android.util.Log;

import com.brentandjody.stenoime.Translator.Dictionary;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;


public class DictionaryTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String[] files = new String[] {"test.json", "test2.json", "test3.json"};
        for (String file : files) {
            try {
                InputStream in = getContext().getAssets().open(file);
                File outFile = new File("/sdcard", file);
                OutputStream out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + file, e);
            }
        }
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
        String[] files = new String[] {"test.json", "test2.json", "test3.json"};
        for (String file : files) {
            try {
                File outFile = new File("/sdcard", file);
                outFile.delete();
            } catch(Exception e) {
                Log.e("tag", "Failed to delete temporary dictionary: " + file, e);
            }
        }
    }

    public void testAPP_HAS_WRITE_PERMISSION() throws Exception {
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(dictionary.size() > 0);
    }

    public void testIsLoading() throws Exception {
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        assertFalse(dictionary.isLoading());
        String[] files = new String[] {"test.json", "test2.json", "test3.json"};
        dictionary.load(files, null, 0);
        assertTrue(dictionary.isLoading());
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        assertFalse(dictionary.isLoading());
    }

    public void testBadFilename() throws Exception {
        Dictionary dictionary = new Dictionary(getContext());
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));
        dictionary.load(new String[] {"booga.json"},null, 0);
        assertEquals("Dictionary File: booga.json could not be found", errContent.toString().trim());
    }

    public void testIllegalFileType() throws Exception{
        Dictionary dictionary = new Dictionary(getContext());
        try {
            dictionary.load(new String[] {"test.rtf"}, null, 0);
            Assert.fail("Illegal file type");
        } catch (Exception e) {
        }
    }

    public void testDefaultDictionary() throws Exception{
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        assertEquals(0, dictionary.size());
        dictionary.load(new String[0], getContext().getAssets(), 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        int size = dictionary.size();
        assertTrue(size > 0);
        assertEquals("mathematic", dictionary.lookup("PHA*T/PHA/TEUBG") );
    }

    public void testLoadAndClear() throws Exception {
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        assertEquals(0, dictionary.size());
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        int size = dictionary.size();
        assertTrue(size > 0);
        dictionary.clear();
        assertEquals(0, dictionary.size());
    }

    public void testOverrideEntries() throws Exception{
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch1 = new CountDownLatch(1);
        assertEquals(0,dictionary.size());
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        final int size = dictionary.size();
        assertTrue(size > 0);
        assertEquals("adjudicator", dictionary.lookup("AD/SKWRAOUD/KAEUT/TOR"));
        dictionary.load((new String[] {"/sdcard/test2.json"}), null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch1.countDown();
            }
        });
        latch1.await();
        assertEquals(size, dictionary.size());
        assertEquals("judge", dictionary.lookup("AD/SKWRAOUD/KAEUT/TOR"));
    }

    public void testLoad2Dictionaries() throws Exception{
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch1 = new CountDownLatch(1);
        assertEquals(0,dictionary.size());
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        int size = dictionary.size();
        assertTrue(size > 0);
        dictionary.load(new String[] {"/sdcard/test3.json"}, null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch1.countDown();
            }
        });
        latch1.await();
        assertTrue(dictionary.size() > size);
    }

    public void testLookupAndForceLookup() throws Exception {
        Dictionary dictionary = new Dictionary(getContext());
        final CountDownLatch latch = new CountDownLatch(1);
        dictionary.load(new String[] {"/sdcard/test.json"}, null, 0);
        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
            @Override
            public void onDictionaryLoaded() {
                latch.countDown();
            }
        });
        latch.await();
        assertNull(dictionary.lookup("AD/R-R"));
        assertNull(dictionary.forceLookup("AD/R-R"));
        assertEquals("adjudication", dictionary.lookup("AD/SKWRAOUD/KAEUGS"));
        assertEquals("adjudication", dictionary.forceLookup("AD/SKWRAOUD/KAEUGS"));
        assertEquals("", dictionary.lookup("AD/SKWRAOUD/KAEUT"));
        assertEquals("adjudicate", dictionary.forceLookup("AD/SKWRAOUD/KAEUT"));
        assertEquals("{\"^}", dictionary.forceLookup("KW-GS"));
    }

//    public void testLongestValidStroke() throws Exception {
//        Dictionary dictionary = new Dictionary(getContext());
//        final CountDownLatch latch = new CountDownLatch(1);
//        dictionary.load(new String[] {"/sdcard/test.json"}, null, 0);
//        dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
//            @Override
//            public void onDictionaryLoaded() {
//                latch.countDown();
//            }
//        });
//        latch.await();
//        assertTrue(Arrays.equals(Stroke.separate("AD/SREPB/KHUR"), dictionary.longestValidStroke("AD/SREPB/KHUR/OU")));
//    }


}

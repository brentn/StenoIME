package com.brentandjody.stenoime.Translator.Tests;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import com.brentandjody.stenoime.Translator.Dictionary;
import com.brentandjody.stenoime.Translator.Optimizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OptimizerTest extends AndroidTestCase {

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

    public void testBetterStroke() throws Exception {
        final Dictionary dictionary = new Dictionary(getContext());
        final Optimizer optimizer = new Optimizer(getContext());
        try {
            dictionary.load(new String[]{"/sdcard/test.json"}, null, 10);
            while (dictionary.isLoading()) {
                SystemClock.sleep(100);
            }
            assertFalse(dictionary.isLoading());
            assertTrue(dictionary.size() > 10);
            optimizer.reloadLookupTable(dictionary);
            while (!optimizer.isRunning()) {
                SystemClock.sleep(100);
            }
            //These two are equal length, and should not optimize
            optimizer.analyze("AD/SRAPB/TAPBLG", 0, "advantage ");
            SystemClock.sleep(2500);
            assertNull(optimizer.getLastBestStroke());
            optimizer.analyze("AD/SRAPBT/APBLG", 0, "advantage ");
            SystemClock.sleep(2500);
            assertNull(optimizer.getLastBestStroke());
            //This one has a shorter stroke
            optimizer.analyze("AD/SRAPB/TAEU/SKWROUS", 0, "advantageous ");
            SystemClock.sleep(3000);
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            //Fingerspelling
            optimizer.analyze("*A", 0, "a ");
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            optimizer.analyze("*TK", 1, "d ");
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            optimizer.analyze("*SR", 1, "v ");
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            optimizer.analyze("*A", 1, "a ");
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            optimizer.analyze("*TPH", 1, "n ");
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            optimizer.analyze("*KR", 1, "c ");
            assertEquals("AD/SRAPBGS", optimizer.getLastBestStroke());
            optimizer.analyze("*-E", 1, "e ");
            SystemClock.sleep(2500);
            assertEquals("AD/SRAPBS", optimizer.getLastBestStroke());
        } finally {
            optimizer.stop();
        }
    }


}

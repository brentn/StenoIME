package com.brentandjody.stenoime.tools;

import android.os.AsyncTask;
import android.util.Log;

import com.brentandjody.stenoime.Translator.TST;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Created by brent on 19/02/14.
 */
public class LookupTable {
    private static final String[] DICTIONARY_TYPES = {".json"};
    private static final String TAG = "StenoIme";

    private TST<Queue<String>> mDictionary = new TST<Queue<String>>();
    private static boolean loaded = false;

    public LookupTable() {
    }

    public void load(String[] filenames) {
        new loader().execute(filenames);
    }

    public Queue<String> lookup(String english) {
        if (!loaded) return null;
        return mDictionary.get(english);
    }

    public Queue<String> possibilities(String partial_word, int limit) {
        if (partial_word.length()<1) return null;
        Queue<String> result = new LinkedList<String>();
        for (String possibility : mDictionary.prefixMatch(partial_word)) {
            result.add(possibility);
            if (result.size() >= limit) break;
        }
        if (result.size() < limit) {
            for (String possibility : mDictionary.prefixMatch("{"+partial_word)) {
                result.add(possibility);
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    public int size() { return mDictionary.size(); }

    public void unload() {
        mDictionary = null;
        mDictionary = new TST<Queue<String>>();
    }

    private boolean validateFilename(String filename) {
        if (filename.contains(".")) {
            String extension = filename.substring(filename.lastIndexOf("."));
            if (Arrays.asList(DICTIONARY_TYPES).contains(extension)) {
                File file = new File(filename);
                if (!file.exists()) {
                    System.err.println("com.brentandjody.BriefTrainer.Dictionary File: "+filename+" could not be found");
                    return false;
                }
            } else {
                System.err.println(extension + " is not an accepted dictionary format.");
                return false;
            }
        } else {
            System.err.println("com.brentandjody.BriefTrainer.Dictionary file does not have the correct extiension");
            return false;
        }
        return true;
    }

    private void addToDictionary(String stroke, String english) {
        StrokeComparator compareByStrokeLength = new StrokeComparator();
        Queue<String> strokes = mDictionary.get(english);
        if (strokes == null)
            strokes = new PriorityQueue<String>(3, compareByStrokeLength);
        strokes.add(stroke);
        mDictionary.put(english, strokes);
    }

    private class StrokeComparator implements Comparator<String> {

        @Override
        public int compare(String a, String b) {
            if (a==null || b==null) return 0;
            int aStrokes = countStrokes(a);
            int bStrokes = countStrokes(b);
            //first compare number of strokes
            if (aStrokes < bStrokes) return -1;
            if (aStrokes > bStrokes) return 1;
            //then compare complexity of strokes
            if (a.length() < b.length()) return -1;
            if (a.length() > b.length()) return 1;
            //otherwise consider them equal
            return 0;
        }

        private int countStrokes(String s) {
            return (s.length()-s.replace("/","").length());
        }
    }

    private class loader extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... filenames) {
            String line, stroke, english;
            String[] fields;
            boolean simple= (filenames.length<=1);
            TST<String> forwardLookup = new TST<String>();
            for (String filename : filenames) {
                if (validateFilename(filename)) {
                    try {
                        File file = new File(filename);
                        FileReader reader = new FileReader(file);
                        BufferedReader lines = new BufferedReader(reader);
                        while ((line = lines.readLine()) != null) {
                            fields = line.split("\"");
                            if ((fields.length > 3) && (fields[3].length() > 0)) {
                                stroke = fields[1];
                                english = fields[3];
                                if (simple) {
                                    addToDictionary(stroke, english);
                                } else {
                                    forwardLookup.put(stroke, english);
                                }
                            }
                        }
                        lines.close();
                        reader.close();
                    } catch (IOException e) {
                        System.err.println("com.brentandjody.BriefTrainer.Dictionary File: " + filename + " could not be found");
                    }
                }
            }
            if (!simple) {
                // Build reverse lookup
                for (String s : forwardLookup.keys()) {
                    english = forwardLookup.get(s);
                    addToDictionary(s, english);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            loaded = true;
            Log.d(TAG, "LookupTable loaded: "+size());
        }
    }
}

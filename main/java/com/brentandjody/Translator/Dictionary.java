package com.brentandjody.Translator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;

import com.brentandjody.StenoIME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;

/* This is the main Steno Dictionary class
 * which stores a dictionary of stroke / translation pairs
 * and can efficiently do forward lookups
 */

public class Dictionary {

    private static final String[] DICTIONARY_TYPES = {".json"};

    private TST<String> dictionary;
    private final Context context;
    private boolean loading = false;
    private SharedPreferences prefs;

    public Dictionary(Context c) {
        context = c;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        dictionary = new TST<String>();
    }

    public boolean isLoading() {
        return loading;
    }

    public void clear() {
        dictionary = new TST<String>();
    }

    public int size() {
        return dictionary.size();
    }


    public void load(String filename, ProgressBar progressBar, int size) {
        String extension = filename.substring(filename.lastIndexOf("."));
        if (Arrays.asList(DICTIONARY_TYPES).contains(extension)) {
            try {
                InputStream stream = context.getAssets().open(filename);
                stream.close();
            } catch (IOException e) {
                System.err.println("Dictionary File: "+filename+" could not be found");
            }
        } else {
            throw new IllegalArgumentException(extension + " is not an accepted dictionary format.");
        }
        loading = true;

        new JsonLoader(progressBar, size).execute(filename);
    }

    private OnDictionaryLoadedListener onDictionaryLoadedListener;
    public interface OnDictionaryLoadedListener {
        public void onDictionaryLoaded();
    }
    public void setOnDictionaryLoadedListener(OnDictionaryLoadedListener listener) {
        onDictionaryLoadedListener = listener;
    }

    public String lookup(String key) {
        // return null if not found
        // and empty string if the result is ambiguous
        if (isLoading()) {
            Log.w("Lookup", "Called while dictionary loading");
        }
        if (key.isEmpty()) return null;
        if (((Collection) dictionary.prefixMatch(key+"/")).size() > 0) return ""; //ambiguous
        return dictionary.get(key);
    }

    public String forceLookup(String key) {
        //return the english translation for this key (even if ambiguous)
        //or null if not found found
        // (this is the same as lookup, except it doesn't return "" for ambiguous entries
        if (key == null || key.isEmpty()) return null;
        return (dictionary.get(key));
    }

    public Stroke[] longestValidStroke(String outline) {
        //returns outline, if it has a valid translation
        //or the longest combination of strokes, starting from the beginning of outline, that has a valid translation
        //or null
        String stroke = dictionary.longestPrefixOf(outline);
        while ((stroke.contains("/")) && (! outlineContainsStroke(outline, stroke))) {
            //remove the last stroke and try again
            String newOutline = stroke.substring(0,stroke.lastIndexOf('/')-1);
            stroke = dictionary.longestPrefixOf(newOutline);
        }
        if (! outlineContainsStroke(outline, stroke)) return null;
        return Stroke.separate(stroke);
    }

    private boolean outlineContainsStroke(String outline, String stroke) {
        //ensures stroke does not contain "partial" strokes  from outline
        return ((outline+"/").contains(stroke+"/"));
    }

    private class JsonLoader extends AsyncTask<String, Integer, Long> {
        private int loaded;
        private int total_size;
        private ProgressBar progressBar;

        public JsonLoader(ProgressBar progress, int size) {
            progressBar = progress;
            total_size = size;
        }

        protected Long doInBackground(String... filenames) {
            loaded = 0;
            int update_interval = total_size/100;
            if (update_interval == 0) update_interval=1;
            String line, stroke, translation;
            String[] fields;
            for (String filename : filenames) {
                if (filename == null || filename.isEmpty())
                    throw new IllegalArgumentException("Dictionary filename not provided");
                try {
                    AssetManager am = context.getAssets();
                    InputStream filestream = am.open(filename);
                    InputStreamReader reader = new InputStreamReader(filestream);
                    BufferedReader lines = new BufferedReader(reader);
                    while ((line = lines.readLine()) != null) {
                        fields = line.split("\"");
                        if ((fields.length >= 3) && (fields[3].length() > 0)) {
                            stroke = fields[1];
                            translation = fields[3];
                            dictionary.put(stroke, translation);
                            loaded++;
                            if (loaded%update_interval==0) {
                                onProgressUpdate(loaded);
                            }
                        }
                    }
                    lines.close();
                    reader.close();
                    filestream.close();
                } catch (IOException e) {
                    System.err.println("Dictionary File: " + filename + " could not be found");
                }
            }
            return (long) loaded;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setMax(total_size);
            progressBar.setProgress(0);
        }

        @Override
        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
            int size = safeLongToInt(result);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(StenoIME.DICTIONARY_SIZE, size);
            editor.commit();
            loading = false;
            if (onDictionaryLoadedListener != null)
                onDictionaryLoadedListener.onDictionaryLoaded();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

    }

    private static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}

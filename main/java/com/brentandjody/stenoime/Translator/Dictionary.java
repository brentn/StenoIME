package com.brentandjody.stenoime.Translator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.brentandjody.stenoime.R;
import com.brentandjody.stenoime.StenoApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    private static final String[] SUPPORTED_DICTIONARY_TYPES = {".json"};
    private static final String TAG = "StenoIME";

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


    public void load(String[] filenames, AssetManager assetManager, int size) {
        // assume filenames is not empty or null
        Log.d(TAG, "loading dictionary");
        for (String filename : filenames) {
            if (filename.contains(".")) {
                String extension = filename.substring(filename.lastIndexOf("."));
                if (Arrays.asList(SUPPORTED_DICTIONARY_TYPES).contains(extension)) {
                    try {
                        File file = new File(filename);
                        if (!file.exists()) {
                            throw new IOException("Dictionary file could not be found.");
                        }
                    } catch (IOException e) {
                        System.err.println("Dictionary File: "+filename+" could not be found");
                    }
                } else {
                    throw new IllegalArgumentException(extension + " is not an accepted dictionary format.");
                }
            }
        }
        loading = true;
        new JsonLoader(assetManager, size).execute(filenames);
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

    public String longestPrefix(String key) {
        String prefix = key;
        if (forceLookup(key)!=null) return key;
        while (prefix.contains("/")) {
            prefix = prefix.substring(0, prefix.indexOf('/'));
            if (forceLookup(prefix) != null) return prefix;
        }
        return "";
    }

    public Stroke[] longestValidStroke(String outline) {
        //returns outline, if it has a valid translation
        //or the longest combination of strokeCount, starting from the beginning of outline, that has a valid translation
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

    public Iterable<String> allKeys() {
        return dictionary.keys();
    }

    private boolean outlineContainsStroke(String outline, String stroke) {
        //ensures stroke does not contain "partial" strokeCount  from outline
        return ((outline+"/").contains(stroke+"/"));
    }

    private class JsonLoader extends AsyncTask<String, Integer, Integer> {
        private int loaded;
        private int total_size;
        private ProgressBar progressBar=null;
        private AssetManager assetManager;

        public JsonLoader(AssetManager am, int size) {
            assetManager = am;
            total_size = size;
            if (context instanceof StenoApp) {
                progressBar = ((StenoApp) context).getProgressBar();
            }
        }

        protected Integer doInBackground(String... filenames) {
            loaded = 0;
            int update_interval = total_size/100;
            if (update_interval == 0) update_interval=1;
            String line, stroke, translation;
            String[] fields;
            //if no personal dictionaries are defined, load the default
            if (filenames.length==0) {
                try {
                    InputStream filestream = assetManager.open("dict.json");
                    InputStreamReader reader = new InputStreamReader(filestream);
                    BufferedReader lines = new BufferedReader(reader);
                    while ((line = lines.readLine()) != null) {
                        fields = line.split("\"");
                        if ((fields.length > 3) && (fields[3].length() > 0)) {
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
                    Log.e(TAG, "Error loading default dictionary asset");
                }
            } else {
                for (String filename : filenames) {
                    if (filename == null || filename.isEmpty())
                        throw new IllegalArgumentException("Dictionary filename not provided");
                    try {
                        File file = new File(filename);
                        FileReader reader = new FileReader(file);
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
                    } catch (IOException e) {
                        System.err.println("Dictionary File: " + filename + " could not be found");
                    }
                }
            }
            return loaded;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (context instanceof StenoApp) {
                progressBar = ((StenoApp) context).getProgressBar();
                if (progressBar != null) {
                    View progress = (View) progressBar.getParent();
                    if (progress != null) progress.setVisibility(View.VISIBLE);
                    progressBar.setMax(total_size);
                    progressBar.setProgress(0);
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            Log.d(TAG, "Dictionary loaded");
            int size = safeLongToInt(result);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(context.getString(R.string.key_dictionary_size), size);
            editor.commit();
            loading = false;
            if (onDictionaryLoadedListener != null)
                onDictionaryLoadedListener.onDictionaryLoaded();
            if (progressBar==null && context instanceof StenoApp)
                progressBar=((StenoApp)context).getProgressBar();
            if (progressBar != null) {
                View progress = (View) progressBar.getParent();
                if (progress != null) progress.setVisibility(View.GONE);
            }

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (progressBar != null) {
                progressBar.setProgress(values[0]);
            } else {
                if (context instanceof StenoApp) {
                    progressBar = ((StenoApp) context).getProgressBar();
                    if (progressBar != null) {
                        progressBar.setMax(total_size);
                        progressBar.setProgress(values[0]);
                    }
                }
            }
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

package com.brentandjody.stenoime.Translator;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.brentandjody.stenoime.R;
import com.brentandjody.stenoime.StenoApp;
import com.brentandjody.stenoime.data.DictionaryDBHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import com.brentandjody.stenoime.data.DictionaryContract.DictionaryEntry;
import com.brentandjody.stenoime.performance.Database;

/* This is the main Steno Dictionary class
 * which stores a dictionary of stroke / translation pairs
 * and can efficiently do forward lookups
 */

public class Dictionary {

    private static final String[] SUPPORTED_DICTIONARY_TYPES = {".json"};
    private static final String TAG = "StenoIME";
    private static final String LOOKUP = "SELECT " + DictionaryEntry.COLUMN_TRANSLATION +
            " FROM " + DictionaryEntry.TABLE_NAME +
            " WHERE " + DictionaryEntry.COLUMN_STROKE ;
    private static final String ALL_STROKES = "SELECT " + DictionaryEntry.COLUMN_STROKE +
            " FROM " + DictionaryEntry.TABLE_NAME;


    private SQLiteDatabase dictionary;
    private final Context context;
    private boolean loading = false;
    private SharedPreferences prefs;

    public Dictionary(Context c) {
        context = c;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        dictionary = new DictionaryDBHelper(context).getReadableDatabase();
    }

    public boolean isLoading() {
        return loading;
    }

    public void clear() {
        SQLiteDatabase db = new DictionaryDBHelper(context).getWritableDatabase();
        db.execSQL("DROP TABLE IF EXSTS " + DictionaryEntry.TABLE_NAME);
        db.close();
    }

    public int size() {
        Cursor cursor = dictionary.rawQuery("SELECT Count(*) FROM " + DictionaryEntry.TABLE_NAME, null);
        int result = cursor.getCount();
        cursor.close();
        return result;
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
        String result = forceLookup(key);
        Cursor cursor = dictionary.rawQuery(LOOKUP+" LIKE ?", new String[] {key+"/%"});
        if (cursor.moveToFirst()) {
            result = "";
        }
        cursor.close();
        return result;
    }

    public String forceLookup(String key) {
        //return the english translation for this key (even if ambiguous)
        //or null if not found found
        // (this is the same as lookup, except it doesn't return "" for ambiguous entries
        if (key.isEmpty()) return null;
        String result=null;
        if (isLoading()) {
            Log.w("Lookup", "Called while dictionary loading");
        }
        if (key.isEmpty()) return null;
        Cursor cursor = dictionary.rawQuery(LOOKUP+"=?", new String[] {key});
        if (cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndex(DictionaryEntry.COLUMN_TRANSLATION));
        }
        cursor.close();
        return result;
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

    public Iterable<String> allKeys() {
        Queue<String> result = new LinkedList<String>();
        Cursor cursor = dictionary.rawQuery(ALL_STROKES, null);
        while(cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndex(DictionaryEntry.COLUMN_STROKE)));
        }
        return result;
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
            SQLiteDatabase db=new DictionaryDBHelper(context).getWritableDatabase();
            ContentValues values = new ContentValues();
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
                            values.put(DictionaryEntry.COLUMN_STROKE, fields[1]);
                            values.put(DictionaryEntry.COLUMN_TRANSLATION, fields[3]);
                            db.insertWithOnConflict(DictionaryEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
                                values.put(DictionaryEntry.COLUMN_STROKE, fields[1]);
                                values.put(DictionaryEntry.COLUMN_TRANSLATION, fields[3]);
                                db.insertWithOnConflict(DictionaryEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
                    } finally {
                        db.close();
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

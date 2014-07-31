package com.brentandjody.stenoime.Translator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.brentandjody.stenoime.R;
import com.brentandjody.stenoime.StenoApp;
import com.brentandjody.stenoime.SuggestionsActivity;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.brentandjody.stenoime.data.DictionaryContract.DictionaryEntry;
import com.brentandjody.stenoime.data.DictionaryDBHelper;

/**
 * Class to report on shorter strokes for what is being typed
 * including fingerspelling
 */
public class Optimizer {
    private static final String SELECT_STROKE = "SELECT " + DictionaryEntry.COLUMN_STROKE +
            " FROM " + DictionaryEntry.TABLE_NAME +
            " WHERE " + DictionaryEntry.COLUMN_TRANSLATION;

    private SQLiteDatabase reverseLookupTable;
    private static final String TAG = Optimizer.class.getSimpleName();
    private String last_optimized_stroke = null;
    private Context context;
    private Dictionary mDictionary;
    private boolean loading=false;
    private boolean loaded=false;
    private Deque<Optimization> optimizations = new ArrayDeque<Optimization>();
    private Analyzer mAnalyzer = new Analyzer();
    private boolean running;

    public Optimizer(Context context) {
        mDictionary = ((StenoApp) context).getDictionary(null);
        initialize(context);
    }

    public Optimizer(Context context, Dictionary dictionary) {
        mDictionary = dictionary;
        initialize(context);
    }

    private void initialize(Context context) {
        this.context = context;
        reverseLookupTable = new DictionaryDBHelper(context).getReadableDatabase();
        loadReverseLookupTable();
        mAnalyzer.execute();
        running=true;
    }

    public void onStop() {
        running=false;
    }

    public void release() {
        SQLiteDatabase db = new DictionaryDBHelper(context).getWritableDatabase();
        db.execSQL("DROP TABLE IF EXSTS " + DictionaryEntry.TABLE_NAME);
        db.close();
        loaded=false;
    }

    public boolean isLoaded() {return loaded;}

    public void analyze (String stroke, int backspaces, String translation) {
        loadReverseLookupTable();
        mAnalyzer.enqueue(new Candidate(stroke, translation, backspaces));
    }

    public String test_analyze(String stroke, int backspaces, String translation) {
        last_optimized_stroke=null;
        analyze(stroke, backspaces, translation);
        SystemClock.sleep(100); //wait for analysis
        return last_optimized_stroke;
    }

    public void loadReverseLookupTable() {
        Log.d(TAG, "loadReverseLookupTable()");
        if (!mDictionary.isLoading()) {
            if (!loaded && !loading) {
                ThesaurusLoader loader = new ThesaurusLoader();
                loader.execute(mDictionary);
            } else {
                if (loading) {
                    Log.d(TAG, "Reverse lookup table load in process");
                }
            }
        } else {
            Log.d(TAG, "Dictionary not loaded.  Not loading reverse lookup table.");
        }
    }


    private void sendNotification(Optimization optimization) {
        Intent suggestionIntent = new Intent(context, SuggestionsActivity.class);
        ArrayList<String> values = new ArrayList<String>();
        for (Optimization opt:optimizations) {
            values.add(opt.toString());
        }
        suggestionIntent.putExtra("optimizations", values);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, suggestionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_stenoime)
                        .setContentTitle("Better Stroke Found")
                        .setContentInfo((optimizations.size()>1?"("+(optimizations.size()-1)+" more)":""))
                        .setContentText(optimization.getStroke() + " : " + optimization.getTranslation() )
                        .setContentIntent(pendingIntent);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Better Strokes:");
        for (Optimization opt : optimizations) {
            inboxStyle.addLine(opt.getStroke() + " : " + opt.getTranslation()
                    + " ("+opt.occurences+(opt.occurences==1?" time)":" times)"));
        }
        mBuilder.setStyle(inboxStyle);
        int mNotificationId = 2;
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void addOptimization(Optimization new_opt) {
        Iterator<Optimization> iter = optimizations.iterator();
        Optimization opt;
        while (iter.hasNext()) {
            opt = iter.next();
            if (opt.equals(new_opt)) {
                new_opt.increment(opt.getOccurences());
                iter.remove();
            }
        }
        optimizations.addFirst(new_opt);
    }

    private void findBetterStroke(Candidate candidate) {
        //Send notification, and add to list if any clearly better stroke is available
        String better_stroke = findBetterStroke(candidate.getStroke(), candidate.getTranslation());
        if (better_stroke != null) {
            int original_strokes = countStrokes(candidate.getStroke());
            int improved_strokes = countStrokes(better_stroke);
            int stroke_savings = original_strokes-improved_strokes;
            if (stroke_savings > 0) {
                last_optimized_stroke = better_stroke;
                Optimization optimization = new Optimization(better_stroke, candidate.getTranslation());
                addOptimization(optimization);
                sendNotification(optimization);
            }

        }
    }

    public String findBetterStroke(String stroke, String translation) {
        if (translation==null || translation.trim().length()==0) return null;
        //return any clearly better stroke, or null
        String bestStroke = get(translation.trim());
        if (bestStroke == null) return null;
        if (countStrokes(bestStroke) < countStrokes(stroke)) {
            return bestStroke;
        }
        return null;
    }

    private String shorterOf(String s1, String s2) {
        //Returns the shorter of s1 and s2, based on number of strokeCount alone,
        //or if equal, returns s1;
        if (s1==null) return s2;
        if (s2==null) return s1;
        int c1, c2;
        c1 = countStrokes(s1);
        c2 = countStrokes(s2);
        if (c1 < c2) return s1;
        if (c2 < c1) return s2;
        return s1;
    }

    private int countStrokes(String s) {
        return s.length()-s.replace("/","").length();
    }

    private String get(String translation) {
        if (translation==null || translation.isEmpty()) return null;
        String result=null;
        Cursor cursor = reverseLookupTable.rawQuery(SELECT_STROKE+" = ?", new String[] { translation });
        if (cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndex(DictionaryEntry.COLUMN_STROKE));
        }
        cursor.close();
        return result;
    }

    private boolean mightExist(String translation) {
        boolean result=false;
        Cursor cursor = reverseLookupTable.rawQuery(SELECT_STROKE+" LIKE ?", new String[] { translation+"%" });
        if (cursor.moveToFirst()) {
            result = true;
        }
        cursor.close();
        return result;
    }

    private class Analyzer extends AsyncTask<Void, Void, Void> {
        private Queue<Candidate> toProcess = new LinkedList<Candidate>();
        private List<Candidate> candidates = new ArrayList<Candidate>();

        public void enqueue(Candidate item) {
            toProcess.add(item);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Candidate candidate;
            Log.d(TAG, "Analyzer starting");
            while (running) {
                while (!toProcess.isEmpty()) {
                    Candidate next_candidate = toProcess.remove();
                    Iterator<Candidate> iterator = candidates.iterator();
                    while (iterator.hasNext()) {
                        candidate = iterator.next();
                        candidate.append(next_candidate);
                        if (mightExist(candidate.getTranslation().trim())) {
                            findBetterStroke(candidate);
                        } else {
                            iterator.remove();
                        }
                    }
                    findBetterStroke(next_candidate);
                    candidates.add(next_candidate);
                }
            }
            Log.d(TAG, "Analyzer stopping");
            return null;
        }
    }

    private class ThesaurusLoader extends AsyncTask<Dictionary, Void, Void>
    {
        private final SQLiteDatabase db = new DictionaryDBHelper(context).getWritableDatabase();
        private ContentValues values = new ContentValues();
        private int dict_size;

        @Override
        protected Void doInBackground(Dictionary... dictionary) {
            Log.d(TAG, "doInBackground()");
            String translation, existing_stroke;
            int progress=0;
            dict_size = dictionary[0].size();
            loaded=false;
            loading=true;
            Log.v(TAG, "Setting lookup table flags");
            db.execSQL("UPDATE " + DictionaryEntry.TABLE_NAME +
                    " SET " + DictionaryEntry.COLUMN_FLAG + " =?", new String[]{"true"});
            Log.d(TAG, "Building reverseLookupTable");
            for (String stroke : dictionary[0].allKeys()) {
                if (!running) {
                    Log.d(TAG, "Interrupting load of reverse lookup table");
                    break;
                }
                translation = dictionary[0].forceLookup(stroke);
                if (translation != null) {
                    if (! translation.contains("{")) {
                        existing_stroke = Optimizer.this.get(translation);
                        values.put(DictionaryEntry.COLUMN_STROKE, shorterOf(existing_stroke, stroke));
                        values.put(DictionaryEntry.COLUMN_TRANSLATION, translation);
                        values.put(DictionaryEntry.COLUMN_FLAG, false);
                        db.insertWithOnConflict(DictionaryEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                        Log.v(TAG, "Translation inserted");
                    } else {
                        Log.v(TAG, "Translation skipped");
                    }
                } else {
                    Log.v(TAG, "Null translation");
                }
                progress++;
                Log.v(TAG, progress/dict_size + " loaded...");
            }
            if (running) {
                Log.d(TAG, "removing flagged lookup table entries");
                    db.execSQL("DELETE FROM " + DictionaryEntry.TABLE_NAME +
                            " WHERE " + DictionaryEntry.COLUMN_FLAG + " =?", new String[]{"true"});
                loaded = true;
            }
            loading=false;
            Log.d(TAG, "ReverseLookupTable build complete");

            return null;
        }

    }

    private class Candidate {
        private String stroke;
        private String translation;
        private int backspaces=0;

        public Candidate(String stroke, String translation, int backspaces) {
            this.stroke = stroke;
            this.translation = translation;
            this.backspaces = backspaces;
        }

        public String getStroke() {return stroke;}
        public String getTranslation() {return translation;}
        public int getBackspaces() {return backspaces;}

        public void append(Candidate c) {
            this.stroke += "/"+c.getStroke();
            int end = this.translation.length();
            if (end > 0 && c.getBackspaces() > 0 && c.getBackspaces()<end)
                this.translation=this.translation.substring(0, (end-c.getBackspaces()));
            this.translation+=c.getTranslation();
        }
    }

    public class Optimization implements Serializable {
        private final String stroke;
        private final String translation;
        private int occurences=1;

        public Optimization(String stroke, String translation) {
            this.stroke = stroke;
            this.translation = translation;
        }

        public String getStroke() {return stroke;}
        public String getTranslation() {return translation;}
        public int getOccurences() {return occurences;}

        public boolean equals(Optimization that) {
            return this.stroke.equals(that.getStroke())
                    && this.translation.equals(that.getTranslation());
        }

        public void increment() {occurences++;}
        public void increment(int initial_occurences) {occurences=initial_occurences+1;}

        @Override
        public String toString() {
            return stroke+" :  "+translation+"  ("+occurences+" times)";
        }
    }

}

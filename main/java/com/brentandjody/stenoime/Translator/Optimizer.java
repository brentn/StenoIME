package com.brentandjody.stenoime.Translator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.brentandjody.stenoime.R;
import com.brentandjody.stenoime.StenoApp;
import com.brentandjody.stenoime.SuggestionsActivity;
import com.brentandjody.stenoime.data.DBContract.DictionaryEntry;
import com.brentandjody.stenoime.data.DBContract.OptimizationEntry;
import com.brentandjody.stenoime.data.LookupTableHelper;
import com.brentandjody.stenoime.data.OptimizerTableHelper;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class to report on shorter strokes for what is being typed
 * including fingerspelling
 */

public class Optimizer {
    private static final String TAG = Optimizer.class.getSimpleName();
    private static final String SELECT_STROKE = "SELECT " + DictionaryEntry.COLUMN_STROKE +
            " FROM " + DictionaryEntry.TABLE_NAME +
            " WHERE " + DictionaryEntry.COLUMN_TRANSLATION ;
    private static final String INSERT = "INSERT OR ABORT INTO " + DictionaryEntry.TABLE_NAME +
            " (" + DictionaryEntry.COLUMN_STROKE + ", " +
            DictionaryEntry.COLUMN_TRANSLATION + ") VALUES ( ?, ?);";
    private static final String UPDATE = "UPDATE " + DictionaryEntry.TABLE_NAME +
            " SET " + DictionaryEntry.COLUMN_STROKE + " = ?" +
            " WHERE " + DictionaryEntry.COLUMN_TRANSLATION + " = ?;";
    private static final String PURGE = "DELETE FROM " + DictionaryEntry.TABLE_NAME + ";";

    private final Context mContext;
    private SQLiteDatabase mLookupTable=null;
    private Runnable mAnalyzer;
    private Queue<Candidate> input = new LinkedList<Candidate>();
    private Queue<Candidate> candidates = new LinkedList<Candidate>();
    private boolean loading, running;
    private String last_optimization=null;

    public Optimizer(Context context) {
        Log.d(TAG, "Optimizer created");
        mContext=context;
        loading=false;
        running=false;
    }

    public void start() {
        Log.d(TAG, "start()");
        candidates = new LinkedBlockingQueue<Candidate>();
        mAnalyzer = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "starting looper");
                while (running) {
                    try {
                        Candidate candidate, next_item;
                        while (!input.isEmpty()) {
                            next_item = input.remove();
                            Log.v(TAG, "optimizing: "+next_item.getTranslation());
                            if (!next_item.getTranslation().trim().isEmpty()) {
                                Iterator<Candidate> iterator = candidates.iterator();
                                while (iterator.hasNext()) {
                                    candidate = iterator.next();
                                    candidate.append(next_item);
                                    if (getPrefixOf(candidate.getTranslation().trim())) {
                                        notifyBetterStroke(candidate);
                                    } else {
                                        iterator.remove();
                                    }

                                }
                                notifyBetterStroke(next_item);
                                candidates.add(next_item);
                            }
                        }
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "analysis interrupted");
                    }
                }
                Log.d(TAG, "...looper stopped");
            }
        };

        //execute the above runnable
        if (!loading) {
            run();
        }
    }

    public void pause() {
        Log.d(TAG, "pause()");
        running=false;
        input.clear();
    }

    public void resume() {
        Log.d(TAG, "resume()");
        run();
    }

    public void stop() {
        Log.d(TAG, "stop()");
        loading=false;
        running=false;
        if (mLookupTable != null) {
            mLookupTable.close();
        }
        mLookupTable=null;
        mAnalyzer=null;
        input.clear();
        Log.d(TAG, "Optimizer destroyed");
    }

    public boolean isRunning() {return running;}

    public void initialize() {
        assert(((StenoApp)mContext).isDictionaryLoaded());
        new BackgroundLoader().execute(((StenoApp) mContext).getDictionary(null));
    }

    public void reloadLookupTable(Dictionary dictionary) {
        // this method is for testing
        if (mLookupTable != null) {
            mLookupTable.close();
        }
        new BackgroundLoader().execute(dictionary);
    }

    public void analyze(String stroke, int backspaces, String translation) {
        Log.v(TAG, "analyzing: "+stroke+":"+translation);
        input.add(new Candidate(stroke, translation, backspaces));
    }

    public String getLastBestStroke() { //only for testing
        return last_optimization;
    }

    //*******PRIVATE

    private void run() {
        if (!loading) {
            running = true;
            Log.d(TAG, "running in background thread");
            new Thread(mAnalyzer).start();
            Log.d(TAG, "background Thread finished");
        }
    }

    private String get(String translation) {
        return get(mLookupTable, translation);
    }

    private String get(SQLiteDatabase db,String translation) {
        //lookup a translation from our lookup table
        if (translation==null || translation.isEmpty()) return null;
        String result=null;
        Cursor cursor = db.rawQuery(SELECT_STROKE+" = ?", new String[] { translation });
        if (cursor.moveToFirst()) {
            result = cursor.getString(cursor.getColumnIndex(DictionaryEntry.COLUMN_STROKE));
        }
        cursor.close();
        return result;
    }

    private boolean getPrefixOf(String translation) {
        boolean result=false;
        Cursor cursor = mLookupTable.rawQuery(SELECT_STROKE+" LIKE ?", new String[] { translation+"%" });
        if (cursor.moveToFirst()) {
            result = true;
        }
        cursor.close();
        return result;
    }

//    private String findBetterStroke(String stroke, String translation) {
//        if (translation==null || translation.trim().length()==0) return null;
//        //return any clearly better stroke, or null
//        String bestStroke = get(translation.trim());
//        if (bestStroke == null) return null;
//        if (countStrokes(bestStroke) < countStrokes(stroke)) {
//            return bestStroke;
//        }
//        return null;
//    }

    private void notifyBetterStroke(Candidate candidate) {
        //Send notification, and add to list if any clearly better stroke is available
        String best_stroke = get(candidate.getTranslation().trim());
        if (best_stroke!=null) {
            int original_num_strokes = countStrokes(candidate.getStroke());
            int improved_num_strokes = countStrokes(best_stroke);
            if (improved_num_strokes < original_num_strokes) {
                Candidate optimization = new Candidate(best_stroke, candidate.getTranslation().trim(), 1);
                addOptimization(optimization);
                sendNotification(optimization);
                last_optimization=optimization.getStroke();
            }
        }
    }

    private void addOptimization(Candidate new_optimization) {
        SQLiteDatabase db = new OptimizerTableHelper(mContext).getWritableDatabase();
        ContentValues values = new ContentValues();
        String stroke = new_optimization.getStroke();
        values.put(OptimizationEntry.COLUMN_STROKE, stroke);
        values.put(OptimizationEntry.COLUMN_TRANSLATION, new_optimization.getTranslation());
        values.put(OptimizationEntry.COLUMN_OCCURRENCES, 1);
        Cursor cursor = db.rawQuery("SELECT " + OptimizationEntry.COLUMN_OCCURRENCES
                + " FROM " + OptimizationEntry.TABLE_NAME
                + " WHERE " + OptimizationEntry.COLUMN_STROKE + " = '" + stroke + "';", null);
        if (cursor.getCount() == 0) {
            db.insert(OptimizationEntry.TABLE_NAME, null, values);
            Log.d(TAG, "New optimization added: " + new_optimization.getStroke());
        } else {
            int occurrences = 1;
            if (cursor.moveToFirst()) {
                occurrences+=cursor.getInt(0);
            }
            values.put(OptimizationEntry.COLUMN_OCCURRENCES, occurrences);
            db.update(OptimizationEntry.TABLE_NAME, values, OptimizationEntry.COLUMN_STROKE + "=?",
                    new String[] {new_optimization.getStroke()});
            Log.d(TAG, "Updating optimization: "+new_optimization.getStroke()+" with "+occurrences);
        }
        cursor.close();
        db.close();
    }

    private void sendNotification(Candidate optimization) {
        Intent suggestionIntent = new Intent(mContext, SuggestionsActivity.class);
        SQLiteDatabase db = new OptimizerTableHelper(mContext).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + OptimizationEntry.COLUMN_STROKE + ", "
                            + OptimizationEntry.COLUMN_TRANSLATION + ", "
                            + OptimizationEntry.COLUMN_OCCURRENCES
                            + " FROM " + OptimizationEntry.TABLE_NAME
                            + " ORDER BY " + OptimizationEntry.COLUMN_OCCURRENCES + " DESC;", null);
        int total = cursor.getCount();
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, suggestionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_stat_stenoime)
                        .setContentTitle("Better Stroke Found")
                        .setContentInfo((total>1?"("+(total-1)+" more)":""))
                        .setContentText(optimization.getStroke() + " : " + optimization.getTranslation() )
                        .setContentIntent(pendingIntent);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Better Strokes:");
        while (cursor.moveToNext()) {
            inboxStyle.addLine(cursor.getString(cursor.getColumnIndex(OptimizationEntry.COLUMN_STROKE)) + ":"
                    + cursor.getString(cursor.getColumnIndex(OptimizationEntry.COLUMN_TRANSLATION)));
        }
        mBuilder.setStyle(inboxStyle);
        int mNotificationId = 2;
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        cursor.close();
        db.close();
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
        int strokes = s.length()-s.replace("/","").length()+1;
        int undos = s.length()-s.replace("**","*").length();
        return strokes-(undos*2);
    }

    private class BackgroundLoader extends AsyncTask<Dictionary, Void, Void> {

        @Override
        protected Void doInBackground(Dictionary... dictionaries) {
            Log.d(TAG, "Loading lookup table");
            running=false;
            loading=true;
            long start=new Date().getTime();
            String translation, existing_stroke, shorter_stroke;
            int dict_size = dictionaries[0].size();
            int count=0;
            SQLiteDatabase db = new LookupTableHelper(mContext).getWritableDatabase();
            try {
                SQLiteStatement insert = db.compileStatement(INSERT);
                SQLiteStatement update = db.compileStatement(UPDATE);
                SQLiteStatement purge = db.compileStatement(PURGE);

                db.beginTransaction();
                purge.execute();

                for (String stroke : dictionaries[0].allKeys()) { //3s to iterate
                    if (!loading) {
                        Log.d(TAG, "Lookup table load interrupted.");
                        break;
                    }
                    if (stroke != null) {
                        translation = dictionaries[0].forceLookup(stroke); //2s to lookup
                        if (translation != null) {
                            existing_stroke = Optimizer.this.get(db, translation);
                            if (existing_stroke == null) {
                                insert.clearBindings();
                                insert.bindString(1, stroke);
                                insert.bindString(2, translation);
                                insert.execute();                           //23s to insert
                            } else {
                                shorter_stroke = shorterOf(existing_stroke, stroke);
                                if (stroke.equals(shorter_stroke)) {
                                    update.clearBindings();
                                    update.bindString(1, stroke);
                                    update.bindString(2, translation);
                                    update.execute();                   //29s to update
                                }
                            }
                        } else {
                            Log.d(TAG, "oops! Translation was null for " + stroke);
                        }
                    }
                    count++;
                    if (count % 10000 == 0) {
                        Log.v(TAG, count + "/" + dict_size + " loaded.");
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                Log.d(TAG, "Lookup table loaded in " + (new Date().getTime() - start) / 1000 + "s");
            } finally {
                db.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                mLookupTable = new LookupTableHelper(mContext).getReadableDatabase();
                if (loading) {
                    loading = false;
                    start();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error opening database: "+e.getMessage());
                Optimizer.this.stop();
            }
        }
    }

    private class Candidate {
        private String stroke;
        private String translation;
        private int counter =0;

        public Candidate(String stroke, String translation, int backspaces) {
            this.stroke = stroke;
            this.translation = translation;
            this.counter = backspaces;
        }

        public String getStroke() {return stroke;}
        public String getTranslation() {return translation;}
        public int getCounter() {return counter;}

        public void append(Candidate c) {
            //appends another stroke/translation pair to this one
            // assumes counter is used to enumerate backspaces
            this.stroke += "/"+c.getStroke();
            int end = this.translation.length();
            if (end > 0 && c.getCounter() > 0 && c.getCounter()<end)
                this.translation=this.translation.substring(0, (end-c.getCounter()));
            this.translation+=c.getTranslation();
        }

        public String toString() {
            return stroke + " : " + translation + "   ("+counter+ " time"+(counter>1?"s.)":".)");
        }
    }

}

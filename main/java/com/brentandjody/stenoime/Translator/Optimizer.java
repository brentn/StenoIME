package com.brentandjody.stenoime.Translator;

import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.brentandjody.stenoime.data.LookupTableHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

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
    private Queue<Candidate> input = new LinkedList<Candidate>();
    private Deque<Candidate> optimizations = new ArrayDeque<Candidate>();
    private boolean loading, running;

    public Optimizer(Context context) {
        Log.d(TAG, "Optimizer created");
        mContext=context;
        running=false;
    }

    public void pause() {
    }

    public void stop() {
        loading=false;
        running=false;
        mLookupTable=null;
        input.clear();
        Log.d(TAG, "Optimizer destroyed");
    }

    public boolean isRunning() {return running;}

    public void refreshLookupTable() {
        assert(((StenoApp)mContext).isDictionaryLoaded());
        new BackgroundLoader().execute(((StenoApp) mContext).getDictionary(null));
    }

    public void analyze(String stroke, int backspaces, String translation) {
        Log.d(TAG, "analyzing: "+translation);
        input.add(new Candidate(stroke, translation, backspaces));
    }

    //*******PRIVATE

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

    private String findBetterStroke(String stroke, String translation) {
        if (translation==null || translation.trim().length()==0) return null;
        //return any clearly better stroke, or null
        String bestStroke = get(translation.trim());
        if (bestStroke == null) return null;
        if (countStrokes(bestStroke) < countStrokes(stroke)) {
            return bestStroke;
        }
        return null;
    }

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
            }

        }
    }

    private void addOptimization(Candidate new_optimization) {
        Iterator<Candidate> iter = optimizations.iterator();
        Candidate optimization;
        while (iter.hasNext()) {
            optimization = iter.next();
            if (optimization.getStroke().equals(new_optimization.getStroke())) {
                new_optimization.increment(optimization.getBackspaces());
                iter.remove();
            }
        }
        optimizations.addFirst(new_optimization);
    }

    private void sendNotification(Candidate optimization) {
        Intent suggestionIntent = new Intent(mContext, SuggestionsActivity.class);
        ArrayList<String> values = new ArrayList<String>();
        for (Candidate opt:optimizations) {
            values.add(opt.toString());
        }
        suggestionIntent.putExtra("optimizations", values);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, suggestionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_stat_stenoime)
                        .setContentTitle("Better Stroke Found")
                        .setContentInfo((optimizations.size()>1?"("+(optimizations.size()-1)+" more)":""))
                        .setContentText(optimization.getStroke() + " : " + optimization.getTranslation() )
                        .setContentIntent(pendingIntent);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("Better Strokes:");
        for (Candidate opt : optimizations) {
            inboxStyle.addLine(opt.getStroke() + " : " + opt.getTranslation()
                    + " ("+opt.backspaces+(opt.backspaces==1?" time)":" times)"));
        }
        mBuilder.setStyle(inboxStyle);
        int mNotificationId = 2;
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
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
                            insert.clearBindings();
                            insert.bindString(1, stroke);
                            insert.bindString(2, translation);
                            try {
                                insert.execute();                           //23s to insert
                            } catch (SQLiteConstraintException e) {
                                existing_stroke = Optimizer.this.get(db, translation);
                                if (existing_stroke != null) {
                                    shorter_stroke = shorterOf(existing_stroke, stroke);
                                    if (stroke.equals(shorter_stroke)) {
                                        update.clearBindings();
                                        update.bindString(1, stroke);
                                        update.bindString(2, translation);
                                        update.execute();                   //29s to update
                                    }
                                } else {
                                    Log.d(TAG, "oops! existing stroke was null for " + translation);
                                }
                            }
                        } else {
                            Log.d(TAG, "oops! Translation was null for " + stroke);
                        }
                    } else {
                        Log.d(TAG, "oops! Stroke was null");
                    }
                    count++;
                    if (count % 1000 == 0) {
                        Log.d(TAG, count + "/" + dict_size + " loaded.");
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
            running=loading;
            loading=false;
            mLookupTable = new LookupTableHelper(mContext).getReadableDatabase();
            new Analyzer().execute();
        }
    }

    private class Analyzer extends AsyncTask<Void, Void, Void> {
        private Queue<Candidate> candidates = new LinkedList<Candidate>();
        @Override
        protected Void doInBackground(Void... voids) {
            Candidate candidate, next_item;
            Log.d(TAG, "Starting Analyzer");
            while (running) {
                while (! input.isEmpty()) {
                    next_item=input.remove();
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
            Log.d(TAG, "Stopping Analyzer");
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

        public void increment(int initial_value) {backspaces=initial_value+1;}

        public void append(Candidate c) {
            this.stroke += "/"+c.getStroke();
            int end = this.translation.length();
            if (end > 0 && c.getBackspaces() > 0 && c.getBackspaces()<end)
                this.translation=this.translation.substring(0, (end-c.getBackspaces()));
            this.translation+=c.getTranslation();
        }
    }

}

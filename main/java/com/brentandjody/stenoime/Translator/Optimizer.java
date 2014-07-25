package com.brentandjody.stenoime.Translator;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.brentandjody.stenoime.R;
import com.brentandjody.stenoime.StenoApp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Class to report on shorter strokes for what is being typed
 * including fingerspelling
 */
public class Optimizer {
    private TST<String> thesaurus = new TST<String>();
    private static final String TAG = Optimizer.class.getSimpleName();
    private List<Candidate> candidates = new ArrayList<Candidate>();
    private String last_optimized_stroke = null;
    private Context context;
    private Dictionary mDictionary;
    private boolean loading=false;
    private boolean loaded=false;
    private Deque<Optimization> optimizations = new ArrayDeque<Optimization>();

    public Optimizer(Context context) {
        this.context = context;
        mDictionary = ((StenoApp) context).getDictionary(null);
    }

    public Optimizer(Context context, Dictionary dictionary) {
        this.context = context;
        mDictionary = dictionary;
    }

    public boolean isLoaded() {return loaded;}

    public void release() {
        Log.d(TAG, "Unloading thesaurus");
        thesaurus = new TST<String>();
        loaded=false;
    }

    public void releaseAndNotify() {
        release();
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_stenoime)
                        .setContentTitle("Out of Memory")
                        .setContentText("Unloading "+context.getResources().getString(R.string.optimizer)+ " table.");
        int mNotificationId = 3;
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
    
    public String analyze (String stroke, int backspaces, String translation) {
        if (!loaded) {
            initiateLoad();
            return null;
        }
        //return value is only for testing
        last_optimized_stroke=null;
        Candidate candidate;
        Iterator<Candidate> iterator = candidates.iterator();
        while (iterator.hasNext()) {
            candidate = iterator.next();
            candidate.append(stroke, backspaces, translation);
            if (thesaurus.nodeExists(candidate.getTranslation().trim())) {
                findBetterStroke(candidate);
            } else {
                iterator.remove();
            }
        }
        candidate = new Candidate(stroke, translation);
        findBetterStroke(candidate);
        candidates.add(candidate);
        return last_optimized_stroke;
    }

    private void initiateLoad() {
        if (!mDictionary.isLoading()) {
            if (!loading) {
                loading = true;
                new BackgroundLoader().execute(mDictionary);
            }
        } else {
            Log.d(TAG, "...waiting for dictionary to load");
        }
    }

    private void findBetterStroke(Candidate candidate) {
        //return any clearly better stroke, or null
        String better_stroke = findBetterStroke(candidate.getStroke(), candidate.getTranslation());
        if (better_stroke != null) {
            int original_strokes = countStrokes(candidate.getStroke());
            int improved_strokes = countStrokes(better_stroke);
            int stroke_savings = original_strokes-improved_strokes;
            if (stroke_savings > 0) {
                last_optimized_stroke = better_stroke;
                Optimization optimization = new Optimization(better_stroke, candidate.getTranslation());
                addOptimization(optimization);
                sendNotification(optimization, stroke_savings);
            }

        }
    }

    private void sendNotification(Optimization optimization, int stroke_savings) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_stenoime)
                        .setContentTitle("Better Stroke Found")
                        .setContentInfo((optimizations.size()>1?"("+(optimizations.size()-1)+" more)":""))
                        .setContentText(optimization.getStroke() + " : " + optimization.getTranslation() );
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

    public String findBetterStroke(String stroke, String translation) {
        if (translation==null || translation.trim().length()==0) return null;
        //return any clearly better stroke, or null
        String bestStroke = thesaurus.get(translation.trim());
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

    private class Candidate {
        private String stroke;
        private String translation;

        public Candidate(String stroke, String translation) {
            this.stroke = stroke;
            this.translation = translation;
        }

        public String getStroke() {return stroke;}
        public String getTranslation() {return translation;}

        public void append(String stroke, int backspaces, String translation) {
            this.stroke += "/"+stroke;
            int end = this.translation.length();
            if (end > 0 && backspaces > 0 && backspaces<end)
                this.translation=this.translation.substring(0, (end-backspaces));
            this.translation+=translation;
        }
    }

    private class BackgroundLoader extends AsyncTask<Dictionary, Void, Void>
    {
        @Override
        protected Void doInBackground(Dictionary... dictionary) {
            String translation, existing_stroke;
            Log.d(TAG, "Building thesaurus");
            for (String stroke : dictionary[0].allKeys()) {
                translation = dictionary[0].forceLookup(stroke);
                if (translation != null) {
                    if (! translation.contains("{")) {
                        existing_stroke = thesaurus.get(translation);
                        thesaurus.put(translation, shorterOf(existing_stroke, stroke));
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "Thesaurus build complete");
            loaded=true;
            loading=false;
        }
    }

    private class Optimization {
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
    }

}

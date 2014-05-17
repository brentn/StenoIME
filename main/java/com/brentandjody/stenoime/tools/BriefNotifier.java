package com.brentandjody.stenoime.tools;

import com.brentandjody.stenoime.StenoApp;
import com.brentandjody.stenoime.StenoIME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/**
 * Created by brent on 19/02/14.
 */
public class BriefNotifier extends BroadcastReceiver {

    private static final String TAG = "StenoIME";

    private static List<Candidate> candidates = new ArrayList<Candidate>();
    private static List<Recommendation> recommendations = new ArrayList<Recommendation>();

    private static LookupTable briefLookupTable; // the main briefLookup

    public BriefNotifier(String[] dictionaries) {
        Log.d(TAG, "BriefNotifier:"+dictionaries);
        briefLookupTable = new LookupTable();
        briefLookupTable.load(dictionaries);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int strokes = intent.getIntExtra(StenoIME.LOG_STROKES, 1);
        String translation = intent.getStringExtra(StenoIME.LOG_TRANSLATION);
        process(context, strokes, translation);
    }

    private static void process(Context context, int strokes, String translation) {
        //add or remove strokes from candidates
        //System.out.println(candidates.size()+" : "+strokes+" : "+translation);
        Candidate candidate;
        for (Iterator<Candidate> iterator = candidates.iterator(); iterator.hasNext();) {
            candidate = iterator.next();
            candidate.addWord(strokes, translation);
            Queue<String> lookup = briefLookupTable.lookup(candidate.translation());
            if (lookup==null || lookup.isEmpty()) {
                iterator.remove();
            } else {
                if (numberOfStrokes(lookup.peek()) < candidate.strokes()) {
                    int savings = candidate.strokes()-numberOfStrokes(lookup.peek());
                    Toast recommendation = Toast.makeText(context, lookup.peek(), Toast.LENGTH_SHORT);
                    //TODO:custom toast layout including candidate.translation()
                    recommendation.show();
                }
            }
        }
        candidates.add(new Candidate(strokes, translation));
    }

    private static int numberOfStrokes(String seriesOfStrokes) {
        return seriesOfStrokes.split("/").length;
    }

    private static Recommendation findRecommendation(String stroke) {
        for (Recommendation r : recommendations) {
            if (r.equals(stroke)) return r;
        }
        return null;
    }

    static class Candidate {
        private int strokes=0;
        private String translation="";

        public Candidate(int strokes, String translation) {
            this.strokes = strokes;
            this.translation = translation;
        }

        public int strokes() {return strokes;}
        public String translation() {return translation;}

        public void addWord(int strokes, String word) {
            this.strokes+= strokes;
            if (strokes < 1) {
                int end = this.translation.lastIndexOf(word);
                if (end >= 0)
                    this.translation = this.translation.substring(0, end).trim();
            } else {
                if (!this.translation.isEmpty())
                    this.translation+=" ";
                this.translation+=word;
            }
        }
    }

    static class Recommendation implements Comparable<Recommendation>{
        private int occurrences=0;
        private int savings=0;
        private String stroke="";
        private String translation="";

        public Recommendation(int occurrences, int savings, String stroke, String translation) {
            this.occurrences = occurrences;
            this.savings = savings;
            this.stroke = stroke;
            this.translation = translation;
        }

        public boolean equals(String stroke) {
            if (stroke == null) return false;
            return (this.stroke().equals(stroke));
        }

        public void addOccurrence() {
            this.occurrences++;
        }

        public String improvement() {
            String s = " stroke";
            if (savings > 1) s=" strokes";
            return "This brief will save "+savings+s+", and could have been used "+occurrences+" times.";}
        public int score() {return occurrences * savings;}
        public String stroke() {return stroke;}
        public String translation() {return translation;}

        @Override
        public int compareTo(Recommendation that) {
            if (that==null) return -1;
            if (this.score()>that.score()) return -1;
            if (this.score()<that.score()) return 1;
            return 0;
        }
    }

}

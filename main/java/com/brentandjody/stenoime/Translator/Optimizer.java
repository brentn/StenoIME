package com.brentandjody.stenoime.Translator;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by brent on 21/07/14.
 */
public class Optimizer {
    private TST<String> thesaurus = new TST<String>();
    private static final String TAG = Optimizer.class.getSimpleName();
    private List<Candidate> candidates = new ArrayList<Candidate>();

    public Optimizer(Dictionary dictionary) {
        String translation, existing_stroke;
        if (dictionary.isLoading()) {
            //wait for the dictionary to be loaded first
            final CountDownLatch latch = new CountDownLatch(1);
            dictionary.setOnDictionaryLoadedListener(new Dictionary.OnDictionaryLoadedListener() {
                @Override
                public void onDictionaryLoaded() {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted before dictonary loaded.");
            }
        }
        for (String stroke : dictionary.allKeys()) {
            translation = dictionary.forceLookup(stroke);
            if (translation != null) {
                existing_stroke = thesaurus.get(translation);
                thesaurus.put(translation, shorterOf(existing_stroke, stroke));
            }
        }
    }

    public String optimize (String stroke, int backspaces, String translation) {
        //return value is only for testing
        String better_stroke;
        Candidate candidate;
        Iterator<Candidate> iterator = candidates.iterator();
        while (iterator.hasNext()) {
            candidate = iterator.next();
            candidate.append(stroke, backspaces, translation);
            if (thesaurus.nodeExists(candidate.getTranslation().trim())) {
                better_stroke = findBetterStroke(candidate);
            } else {
                iterator.remove();
            }
        }
        candidate = new Candidate(stroke, translation);
        better_stroke = findBetterStroke(candidate);
        candidates.add(candidate);
        Log.v(TAG, "CANDIDATES");
        for (Candidate c : candidates) {
            Log.v(TAG, c.getStroke()+": "+c.getTranslation());
        }
        return better_stroke;
    }

    private String findBetterStroke(Candidate candidate) {
        //return any clearly better stroke, or null
        return findBetterStroke(candidate.getStroke(), candidate.getTranslation());
    }

    public String findBetterStroke(String stroke, String translation) {
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
        String stroke;
        String translation;

        public Candidate(String stroke, String translation) {
            this.stroke = stroke;
            this.translation = translation;
        }

        public String getStroke() {return stroke;}
        public String getTranslation() {return translation;}

        public void append(String stroke, int backspaces, String translation) {
            this.stroke += "/"+stroke;
            int end = this.translation.length();
            if (backspaces > 0)
                this.translation=this.translation.substring(0, (end-backspaces));
            this.translation+=translation;
        }
    }
}

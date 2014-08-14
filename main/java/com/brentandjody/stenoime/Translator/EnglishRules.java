package com.brentandjody.stenoime.Translator;

import android.content.Context;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brentn on 16/01/14.
 * Handle English suffix orthography with optional word list
 */
public class EnglishRules {

    private static final Rule RULE1 = new Rule(Pattern.compile("^(.*[aeiou]c) \\^ ly$", Pattern.CASE_INSENSITIVE), "\\1ally");
    private static final Rule RULE2 = new Rule(Pattern.compile("^(.*(?:s|sh|x|z|zh)) \\^ s$", Pattern.CASE_INSENSITIVE), "\\1es");
    private static final Rule RULE3 = new Rule(Pattern.compile("^(.*(?:oa|ea|i|ee|oo|au|ou|l|n|(?<![gin]a)r|t)ch) \\^ s$", Pattern.CASE_INSENSITIVE), "\\1es");
    private static final Rule RULE4 = new Rule(Pattern.compile("^(.+[bcdfghjklmnpqrstvwxz])y \\^ s$", Pattern.CASE_INSENSITIVE), "\\1ies");
    private static final Rule RULE5 = new Rule(Pattern.compile("^(.+)ie \\^ ing$$", Pattern.CASE_INSENSITIVE), "\\1ying");
    private static final Rule RULE6 = new Rule(Pattern.compile("^(.+[cdfghlmnpr])y \\^ ist$", Pattern.CASE_INSENSITIVE), "\\1ist");
    private static final Rule RULE7 = new Rule(Pattern.compile("^(.+[bcdfghjklmnpqrstvwxz])y \\^ ([a-hj-xz].*)", Pattern.CASE_INSENSITIVE), "\\1i\\2");
    private static final Rule RULE8 = new Rule(Pattern.compile("^(.+)([t])e \\^ en$", Pattern.CASE_INSENSITIVE), "\\1\\2\\2en");
    private static final Rule RULE9 = new Rule(Pattern.compile("^(.+[bcdfghjklmnpqrstuvwxz])e \\^ ([aeiouy].*)$", Pattern.CASE_INSENSITIVE), "\\1\\2");
    private static final Rule RULE10 = new Rule(Pattern.compile("^(.*(?:[bcdfghjklmnprstvwxyz]|qu)[aeiou])([bcdfgklmnprtvz]) \\^ ([aeiouy].*)$", Pattern.CASE_INSENSITIVE), "\\1\\2\\2\\3");

    private static final Rule[] RULES = new Rule[]{RULE1, RULE2, RULE3, RULE4, RULE5, RULE6, RULE7, RULE8, RULE9, RULE10};

    private WordList word_list=null;

    public EnglishRules(Context context) {
        word_list = new WordList(context);
    }

    public String bestMatch(String word, String suffix) {
        word = word.trim();
        suffix = suffix.trim();
        String result = word+suffix+" "; //default is a simple join
        Comparator<Word> wordScoreComparator = new WordScoreComparator();
        Queue<Word> candidates = new PriorityQueue<Word>(3, wordScoreComparator);
        //try 'ible' instead of able
        if (suffix.equals("able")) {

            candidates.addAll(in_word_list(make_candidates_from_rules(word, "ible")));
            candidates.add(new Word(word+"ible", word_score(word + "ible")));
        }
        //try a simple join
        if (in_word_list(word + suffix)) {
            candidates.add(new Word(word+suffix, word_score(word + suffix)));
        }
        //try rules
        candidates.addAll(in_word_list(make_candidates_from_rules(word, suffix)));
        if (!candidates.isEmpty()) {
            result = candidates.peek().getText() + " ";
        } else {
            //try candidates without lookup
            candidates = make_candidates_from_rules(word, suffix);
            if (!candidates.isEmpty()) {
                result = candidates.peek().getText() + " ";
            }
        }
        //if all else fails, just join
        return result;
    }

    private boolean in_word_list(String word) {
        return word_list == null || word_list.contains(word);
    }

    private Queue<Word> in_word_list(Queue<Word> candidates) {
        if (word_list==null) return candidates; //if not using word list
        Comparator<Word> wordScoreComparator = new WordScoreComparator();
        Queue<Word> result = new PriorityQueue<Word>(3, wordScoreComparator) {
        };
        int score;
        for (Word word : candidates) {
            // if word is in wordlist, then set the score (otherwise default is 999)
            score = word_list.score(word.getText());
            if (score>-1) {
                word.setScore(score);
            }
            result.add(word);
        }
        return result;
    }

    private int word_score(String word) {
        if (word_list==null) return WordList.NOT_FOUND; //if not using word list
        return word_list.score(word);
    }

    private Queue<Word> make_candidates_from_rules(String word, String suffix) {
        Comparator<Word> wordScoreComparator = new WordScoreComparator();
        Queue<Word> candidates = new PriorityQueue<Word>(3, wordScoreComparator);
        Matcher matcher;
        String result;
        for (Rule rule : RULES) {
            matcher = rule.pattern().matcher(word + " ^ " + suffix);
            if (matcher.find()) {
                result = rule.replace();
                if (matcher.groupCount() > 0) result=result.replace("\\1", matcher.group(1));
                if (matcher.groupCount() > 1) result=result.replace("\\2", matcher.group(2));
                if (matcher.groupCount() > 2) result=result.replace("\\3", matcher.group(3));
                candidates.add(new Word(result));
            }
        }
        return candidates;
    }

    private static class Rule {
        private Pattern pattern;
        private String replace;

        public Rule(Pattern p, String s) {
            pattern = p;
            replace = s;
        }

        public Pattern pattern() { return pattern; }
        public String replace() {return replace; }
    }

    private class Word {
        private String text;
        private int score=999;

        public Word(String text) {
            this.text = text;
        }
        public Word(String text, int score) {
            this.text = text;
            this.score = score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getText() {
            return text;
        }
        public int getScore() {
            return score;
        }

    }

    private static class WordScoreComparator implements Comparator<Word> {
        @Override
        public int compare(Word lhs, Word rhs) {
            if (lhs==null && rhs==null) return 0;
            if (rhs==null) return -1;
            if (lhs==null) return 1;
            if (lhs.getScore() < rhs.getScore()) return -1;
            if (lhs.getScore() > rhs.getScore()) return 1;
            return 0;
        }

    }

}

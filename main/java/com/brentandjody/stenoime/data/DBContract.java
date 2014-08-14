package com.brentandjody.stenoime.data;

import android.provider.BaseColumns;

/**
 * Created by brent on 28/07/14.
 */
public class DBContract {
    public static final class DictionaryEntry implements BaseColumns {
        public static final String TABLE_NAME = "dictionary_entries";
        public static final String COLUMN_STROKE = "stroke";
        public static final String COLUMN_TRANSLATION = "translation";
        public static final String INDEX_TRANSLATION = "translation_idx";
    }

    public static final class OptimizationEntry implements BaseColumns {
        public static final String TABLE_NAME = "optimizations";
        public static final String COLUMN_STROKE = "stroke";
        public static final String COLUMN_TRANSLATION = "translation";
        public static final String COLUMN_OCCURRENCES = "occurrences";
    }

    public static final class StatsEntry implements BaseColumns {
        public static final String TABLE_NAME = "stats";
        public static final String COLUMN_WHEN = "datetime";
        public static final String COLUMN_SESS_DUR = "sessiondur";
        public static final String COLUMN_STROKES = "strokes";
        public static final String COLUMN_LETTERS = "letters";
        public static final String COLUMN_MAX_SPEED = "maxspeed";
        public static final String COLUMN_CORRECTIONS = "corrections";
    }

    public static final class WordEntry {
        public static final String TABLE_NAME = "words";
        public static final String COLUMN_WORD = "key";
        public static final String COLUMN_SCORE = "value";
    }
}

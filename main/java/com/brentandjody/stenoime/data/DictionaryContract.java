package com.brentandjody.stenoime.data;

import android.provider.BaseColumns;

/**
 * Created by brent on 28/07/14.
 */
public class DictionaryContract {
    public static final class DictionaryEntry implements BaseColumns {
        public static final String TABLE_NAME = "dictionary_entries";
        public static final String COLUMN_STROKE = "stroke";
        public static final String COLUMN_TRANSLATION = "translation";
        public static final String INDEX_STROKE = "stroke_idx";
        public static final String INDEX_TRANSLATION = "translation_idx";
    }
}

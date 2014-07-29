package com.brentandjody.stenoime.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.brentandjody.stenoime.Translator.Dictionary;
import com.brentandjody.stenoime.data.DictionaryContract.DictionaryEntry;

/**
 * Created by brent on 28/07/14.
 */
public class DictionaryDBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION=1;
    public static final String DATABASE_NAME="dictionary.db";

    public DictionaryDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_DICTIONARY =
                "CREATE TABLE " + DictionaryEntry.TABLE_NAME + " (" +
                        DictionaryEntry._ID + " INTEGER PRIMARY KEY, " +
                        DictionaryEntry.COLUMN_STROKE + " TEXT NOT NULL, " +
                        DictionaryEntry.COLUMN_TRANSLATION + " TEXT NOT NULL); " +
                        "CREATE UNIQUE INDEX " + DictionaryEntry.INDEX_STROKE +
                        " ON " + DictionaryEntry.TABLE_NAME + " (" + DictionaryEntry.COLUMN_STROKE + "); " +
                        "CREATE INDEX " + DictionaryEntry.INDEX_TRANSLATION +
                        " ON " + DictionaryEntry.TABLE_NAME + " (" + DictionaryEntry.COLUMN_TRANSLATION + ");";
        db.execSQL(SQL_CREATE_DICTIONARY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL("DROP TABLE IF EXISTS " + DictionaryEntry.TABLE_NAME);
        onCreate(db);
    }
}

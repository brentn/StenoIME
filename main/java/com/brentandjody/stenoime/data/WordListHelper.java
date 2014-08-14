package com.brentandjody.stenoime.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import com.brentandjody.stenoime.data.DBContract.WordEntry;

/**
 * Created by brentn on 17/01/14.
 * Manage American English wordlist as database
 */
public class WordListHelper extends SQLiteAssetHelper {


    private static final String DATABASE_NAME = "wordlist.db";
    private static final int DATABASE_VERSION = 1;
    private static final String LOOKUP = "SELECT " + WordEntry.COLUMN_SCORE + " FROM " + WordEntry.TABLE_NAME + " WHERE " + WordEntry.COLUMN_WORD + "=?";
    public static final int NOT_FOUND = 999;

    private SQLiteDatabase db;

    public WordListHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getReadableDatabase();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //this is blank so I don't lose the entire wordlist!
    }

    public boolean contains(String word) {
        String[] args = new String[] {word};
        Cursor c = db.rawQuery(LOOKUP, args);
        boolean result = (c.getCount()>0);
        c.close();
        return result;
    }

    public int score(String word) {
        String[] args = new String[] {word};
        int result=NOT_FOUND;
        Cursor c = db.rawQuery(LOOKUP, args);
        if (c.moveToFirst()) {
            result = (c.getInt(0));
        }
        c.close();
        return result;
    }

}

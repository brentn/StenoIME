package com.brentandjody.stenoime.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.brentandjody.stenoime.data.DBContract.OptimizationEntry;

/**
 * Table for recording suggestions
 */
public class OptimizerTableHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION=2;
    public static final String DATABASE_NAME="optimizations.db";

    public OptimizerTableHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_DICTIONARY =
                "CREATE TABLE " + OptimizationEntry.TABLE_NAME + " (" +
                        OptimizationEntry._ID + " INTEGER PRIMARY KEY, " +
                        OptimizationEntry.COLUMN_STROKE + " TEXT UNIQUE NOT NULL, " +
                        OptimizationEntry.COLUMN_TRANSLATION + " TEXT NOT NULL, " +
                        OptimizationEntry.COLUMN_OCCURRENCES + " INTEGER); ";
        db.execSQL(SQL_CREATE_DICTIONARY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL("DROP TABLE IF EXISTS " + OptimizationEntry.TABLE_NAME);
        onCreate(db);
    }
}

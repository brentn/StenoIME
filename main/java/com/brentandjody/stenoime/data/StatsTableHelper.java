package com.brentandjody.stenoime.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.brentandjody.stenoime.data.DBContract.StatsEntry;

/**
 * Table for recording performance stats
 */
public class StatsTableHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION=1;
    public static final String DATABASE_NAME="performance.db";

    public StatsTableHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE =
                "CREATE TABLE " + StatsEntry.TABLE_NAME + " (" +
                        StatsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        StatsEntry.COLUMN_WHEN + " INTEGER NOT NULL, " +
                        StatsEntry.COLUMN_SESS_DUR + " REAL NOT NULL, " +
                        StatsEntry.COLUMN_STROKES + " INTEGER NOT NULL, " +
                        StatsEntry.COLUMN_LETTERS + " INTEGER NOT NULL, " +
                        StatsEntry.COLUMN_MAX_SPEED + " REAL NOT NULL, " +
                        StatsEntry.COLUMN_CORRECTIONS + " INTEGER NOT NULL);";
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL("DROP TABLE IF EXISTS " + StatsEntry.TABLE_NAME);
        onCreate(db);
    }
}

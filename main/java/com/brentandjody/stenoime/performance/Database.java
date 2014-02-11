package com.brentandjody.stenoime.performance;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by brent on 10/02/14.
 */
public class Database extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "performance";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_STATS = "stats";

    private static final String COL_ID = "id";
    private static final String COL_WHEN = "datetime";
    private static final String COL_SESS_DUR = "session_duration";
    private static final String COL_STROKES = "strokes";
    private static final String COL_LETTERS = "letters";
    private static final String COL_MAX_SPEED = "max_speed";
    private static final String COL_CORRECTIONS = "corrections";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_QUERY = "CREATE TABLE " + TABLE_STATS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + COL_WHEN + " INTEGER, " + COL_SESS_DUR + " INTEGER, "
                + COL_STROKES + " INTEGER, " + COL_LETTERS + " INTEGER, "
                + COL_MAX_SPEED + " INTEGER, " + COL_CORRECTIONS + " INTEGER)";
        db.execSQL(CREATE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_STATS);
        onCreate(db);
    }

    public void insert(PerformanceItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_WHEN, item.when().getTime());
        values.put(COL_SESS_DUR, Math.round(item.minutes() * 100));
        values.put(COL_STROKES, item.strokes());
        values.put(COL_LETTERS, item.letters());
        values.put(COL_MAX_SPEED, Math.round(item.max_speed() * 100));
        values.put(COL_CORRECTIONS, item.corrections());
    }

    public double bestRatio() {
        return 0.0;
    }

    public int bestSpeed() {
        SQLiteDatabase db = getReadableDatabase();
        final SQLiteStatement query = db.compileStatement("SELECT max(" + COL_MAX_SPEED + ") FROM " + TABLE_STATS);
        return (int) query.simpleQueryForLong();
    }
}

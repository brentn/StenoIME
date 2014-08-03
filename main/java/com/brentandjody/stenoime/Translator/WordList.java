package com.brentandjody.stenoime.Translator;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by brentn on 17/01/14.
 * Manage American English wordlist as database
 */
public class WordList extends SQLiteAssetHelper {

    public static final int NOT_FOUND = 999;

    private static final String DATABASE_NAME = "wordlist.db";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase db;

    public WordList(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getReadableDatabase();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public boolean contains(String word) {
        String[] args = new String[] {word};
        Cursor c = db.rawQuery("SELECT value FROM words WHERE key=?", args);
        boolean result = (c.getCount()>0);
        c.close();
        return result;
    }

    public int score(String word) {
        String[] args = new String[] {word};
        int result=NOT_FOUND;
        Cursor c = db.rawQuery("SELECT value FROM words WHERE key=?", args);
        if (c.moveToFirst()) {
            result = (c.getInt(0));
        }
        c.close();
        return result;
    }

//    private void fill(Context context) {
//        String line;
//        SQLiteDatabase db;
//        context.deleteDatabase(DATABASE_NAME);
//        db = getWritableDatabase();
//        String statement = "CREATE TABLE IF NOT EXISTS words (key Text PRIMARY KEY, value Integer)";
//        db.execSQL(statement);
//        Pattern pattern = Pattern.compile("^([a-zA-Z]+)\\s(\\d+)\\s?$");
//        Matcher matcher;
//        ContentValues cv;
//        int size=0;
//        Log.e("WordList","Start!");
//        try {
//            InputStream filestream = context.getAssets().open("american_english_words.txt");
//            InputStreamReader reader = new InputStreamReader(filestream);
//            BufferedReader lines = new BufferedReader(reader);
//            while ((line = lines.readLine()) != null) {
//                matcher = pattern.matcher(line);
//                if (matcher.find()) {
//                    if (matcher.groupCount()==2) {
//                        cv = new ContentValues();
//                        cv.put("key", matcher.group(1));
//                        cv.put("value", matcher.group(2));
//                        db.insertWithOnConflict("words", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
//                        size++;
//                        if (size%1000==0)
//                            Log.e("WordList", "Progress:"+size);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.e("WordList", e.getMessage());
//        }
//        db.close();
//        Log.e("WordList", ""+size);
//    }
}

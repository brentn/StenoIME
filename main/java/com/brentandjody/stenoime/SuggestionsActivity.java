package com.brentandjody.stenoime;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.brentandjody.stenoime.Input.SwipeDismissListViewTouchListener;
import com.brentandjody.stenoime.data.OptimizerTableHelper;
import com.brentandjody.stenoime.data.DBContract.OptimizationEntry;



public class SuggestionsActivity extends ListActivity {

    private static final String TAG = SuggestionsActivity.class.getSimpleName();
    private SimpleCursorAdapter mAdapter = null;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);
        mAdapter = new SimpleCursorAdapter(SuggestionsActivity.this,
                R.layout.optimization_item, null,
                new String[]{OptimizationEntry.COLUMN_STROKE, OptimizationEntry.COLUMN_TRANSLATION, OptimizationEntry.COLUMN_OCCURRENCES},
                new int[]{R.id.stroke, R.id.translation, R.id.occurrences}, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        db = new OptimizerTableHelper(this).getWritableDatabase();
        new Loader().execute();
        setListAdapter(mAdapter);

        // swipe to dismiss
        ListView listView = getListView();
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    //mAdapter.remove(mAdapter.getItem(position));
                                    Cursor item = (Cursor) mAdapter.getItem(position);
                                    int id = item.getInt(item.getColumnIndex(OptimizationEntry._ID));
                                    String stroke = item.getString(item.getColumnIndex(OptimizationEntry.COLUMN_STROKE));
                                    Log.d(TAG, "DELETING: " + stroke);
                                    delete(id);
                                }
                            }
                        });
        listView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        listView.setOnScrollListener(touchListener.makeScrollListener());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    private void delete(int id) {
        db.delete(OptimizationEntry.TABLE_NAME, OptimizationEntry._ID + " = ?", new String[]{Integer.toString(id)});
        new Loader().execute();
    }

    private class Loader extends AsyncTask<Void, Void, Cursor> {

        protected Cursor doInBackground(Void... voids) {
            return db.rawQuery("SELECT " + OptimizationEntry._ID + ", "
                    + OptimizationEntry.COLUMN_STROKE + ", "
                    + OptimizationEntry.COLUMN_TRANSLATION + ", "
                    + OptimizationEntry.COLUMN_OCCURRENCES
                    + " FROM " + OptimizationEntry.TABLE_NAME
                    + " ORDER BY " + OptimizationEntry.COLUMN_OCCURRENCES + " DESC"
                    + " LIMIT 50; ", null);
        }

        protected void onPostExecute(Cursor cursor) {
           if(mAdapter!=null && cursor!=null)
                mAdapter.changeCursor(cursor);
            }

    }
}



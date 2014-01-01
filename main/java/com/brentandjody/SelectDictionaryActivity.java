package com.brentandjody;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copied from StenoDictionary 12/31/13
 */
public class SelectDictionaryActivity extends ListActivity {

    private static final String TAG = "StenoDictionary";
    private static final int FILE_SELECT_CODE = 2;
    private static final List<String> FILE_FORMATS = Arrays.asList(".json");

    private SharedPreferences prefs;
    private StenoApp App;
    private ArrayAdapter<String> adapter;
    private List<String> list = new ArrayList<String>();
    private List<String> pathList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        App = ((StenoApp) getApplication());
        setContentView(R.layout.dictionary_list);
        loadDictionaryList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
        Button addButton = new Button(this);
        addButton.setText(getString(R.string.add_dictionary));
        addButton.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT));
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDictionary();
            }
        });
        getListView().addFooterView(addButton);
        setListAdapter(adapter);
    }

    @Override
    protected void onPause() {
        super.onStop();
        App.unloadDictionary();
    }

    @Override
    protected void onListItemClick(ListView listview, View v, int position, long id) {
        super.onListItemClick(listview, v, position, id);
        AlertDialog.Builder adb=new AlertDialog.Builder(SelectDictionaryActivity.this);
        adb.setTitle("Remove Dictionary?");
        adb.setMessage("Are you sure you want to remove this dictionary?");
        final int positionToRemove = position;
        adb.setNegativeButton("Cancel", null);
        adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeDictionary(positionToRemove);
            }
        });
        adb.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    String path = getPath(this, uri);
                    Log.d(TAG, "File Path: " + path);
                    //update list
                    String extension = path.substring(path.lastIndexOf(".")).toLowerCase();
                    if (FILE_FORMATS.contains(extension)) {
                        String file = path.substring(path.lastIndexOf("/")+1);
                        list.add(file);
                        pathList.add(path);
                        adapter.notifyDataSetChanged();
                        updatePreference();
                    } else {
                        Toast.makeText(this, "Invalid File Format", Toast.LENGTH_SHORT);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void loadDictionaryList() {
        String dictionaries = prefs.getString(StenoApp.KEY_DICTIONARIES, "");
        for (String dictionary : dictionaries.split(StenoApp.DELIMITER)) {
            if (!dictionary.trim().isEmpty()) {
                pathList.add(dictionary);
                list.add(dictionary.substring(dictionary.lastIndexOf("/") + 1));
            }
        }
    }

    private void removeDictionary(int pos) {
        list.remove(pos);
        pathList.remove(pos);
        adapter.notifyDataSetChanged();
        updatePreference();
    }

    private void updatePreference() {
        String dictionaries = "";
        for (String d : pathList) {
            if (!d.trim().isEmpty()) {
                dictionaries += StenoApp.DELIMITER + d;
            }
        }
        if (!dictionaries.isEmpty()) { //list is prefixed with DELIMITER
            dictionaries = dictionaries.substring(1);
        }
        prefs.edit().putString(StenoApp.KEY_DICTIONARIES, dictionaries).commit();
    }

    private void addDictionary() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select your .json dictionary file"),FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager (or DropBox) to access your dictionary.", Toast.LENGTH_LONG).show();
        }
    }

    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

}

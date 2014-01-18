package com.brentandjody.stenoime;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
    private boolean changed = false;
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
        if (changed)
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
                    String path = getPath(getApplicationContext(), uri);

                    Log.d(TAG, "File Path: " + path);
                    //update list
                    String extension = "";
                    if (path.contains(".")) extension = path.substring(path.lastIndexOf(".")).toLowerCase();
                    if (FILE_FORMATS.contains(extension)) {
                        String file = path.substring(path.lastIndexOf("/")+1);
                        list.add(file);
                        pathList.add(path);
                        changed=true;
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
        changed = true;
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

    /**
     * Get a file path from a Uri. This will score the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}

package com.aware.plugin.tracescollector;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

/**
 * Created by researcher on 05/06/15.
 */
public class Provider extends ContentProvider {

    public static final int DATABASE_VERSION = 2;


    public static String AUTHORITY = "com.aware.plugin.tracescollector.provider.tracescollector";

    private static final int TRACESCOLLECTOR = 1;
    private static final int TRACESCOLLECTOR_ID = 2;

    public static final class TracesCollector_Data implements BaseColumns {
        private TracesCollector_Data() {
        };

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + Provider.AUTHORITY + "/plugin_tracescollector");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.tracescollector";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.tracescollector";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String TAG_1 = "tag_1";
        public static final String TAG_2 = "tag_2";
        public static final String TAG_3 = "tag_3";
    }

    public static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_tracescollector.db";

    public static final String[] DATABASE_TABLES = {"plugin_tracescollector"};

    public static final String[] TABLES_FIELDS = {
            TracesCollector_Data._ID + " integer primary key autoincrement," +
                    TracesCollector_Data.TIMESTAMP + " real default 0," +
                    TracesCollector_Data.DEVICE_ID + " text default ''," +
                    TracesCollector_Data.TAG_1 + " text default ''," +
                    TracesCollector_Data.TAG_2 + " text default ''," +
                    TracesCollector_Data.TAG_3 + " text default ''," +
                    "UNIQUE (" + TracesCollector_Data.TIMESTAMP + "," + TracesCollector_Data.DEVICE_ID + ")"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> tableMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen()) ) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.tracescollector"; //make AUTHORITY dynamic
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], TRACESCOLLECTOR); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", TRACESCOLLECTOR_ID); //URI for a single record

        tableMap = new HashMap<String, String>();
        tableMap.put(TracesCollector_Data._ID, TracesCollector_Data._ID);
        tableMap.put(TracesCollector_Data.TIMESTAMP, TracesCollector_Data.TIMESTAMP);
        tableMap.put(TracesCollector_Data.DEVICE_ID, TracesCollector_Data.DEVICE_ID);
        tableMap.put(TracesCollector_Data.TAG_1, TracesCollector_Data.TAG_1);
        tableMap.put(TracesCollector_Data.TAG_2, TracesCollector_Data.TAG_2);
        tableMap.put(TracesCollector_Data.TAG_3, TracesCollector_Data.TAG_3);

        return true; //let Android know that the database is ready to be used.
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case TRACESCOLLECTOR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG) Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TRACESCOLLECTOR:
                return TracesCollector_Data.CONTENT_TYPE;
            case TRACESCOLLECTOR_ID:
                return TracesCollector_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case TRACESCOLLECTOR:
                long _id = database.insert(DATABASE_TABLES[0], TracesCollector_Data.DEVICE_ID, values);
                if (_id > 0) {
                Uri dataUri = ContentUris.withAppendedId(TracesCollector_Data.CONTENT_URI, _id);
                getContext().getContentResolver().notifyChange(dataUri, null);
                return dataUri;
            }
            throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case TRACESCOLLECTOR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case TRACESCOLLECTOR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            default:
                database.close();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}

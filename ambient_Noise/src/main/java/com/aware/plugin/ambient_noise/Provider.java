package com.aware.plugin.ambient_noise;

import java.util.HashMap;

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

public class Provider extends ContentProvider {
	
	public static final int DATABASE_VERSION = 3;
	
	/**
	 * Provider authority: com.aware.plugin.ambient_noise.provider.ambient_noise
	 */
	public static String AUTHORITY = "com.aware.plugin.ambient_noise.provider.ambient_noise";
	
	private static final int AMBIENT_NOISE = 1;
	private static final int AMBIENT_NOISE_ID = 2;
	
	public static final String DATABASE_NAME = "plugin_ambient_noise.db";
	
	public static final String[] DATABASE_TABLES = {
		"plugin_ambient_noise"
	};
	
	public static final String[] TABLES_FIELDS = {
		AmbientNoise_Data._ID + " integer primary key autoincrement," +
		AmbientNoise_Data.TIMESTAMP + " real default 0," +
		AmbientNoise_Data.DEVICE_ID + " text default ''," +
		AmbientNoise_Data.FREQUENCY + " real default 0," +
		AmbientNoise_Data.DECIBELS + " real default 0," +
		AmbientNoise_Data.RMS + " real default 0," +
		AmbientNoise_Data.IS_SILENT + " integer default 0," +
		AmbientNoise_Data.SILENCE_THRESHOLD + " real default 0," +
        AmbientNoise_Data.RAW + " blob default null," +
		"UNIQUE("+AmbientNoise_Data.TIMESTAMP+","+AmbientNoise_Data.DEVICE_ID+")"
	};
	
	public static final class AmbientNoise_Data implements BaseColumns {
		private AmbientNoise_Data(){};
		
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_ambient_noise");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.ambient_noise";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.ambient_noise";
		
		public static final String _ID = "_id";
		public static final String TIMESTAMP = "timestamp";
		public static final String DEVICE_ID = "device_id";
		public static final String FREQUENCY = "double_frequency";
		public static final String DECIBELS = "double_decibels";
		public static final String RMS = "double_rms";
		public static final String IS_SILENT = "is_silent";
        public static final String RAW = "raw";
		public static final String SILENCE_THRESHOLD = "double_silence_threshold";
	}
	
	private static UriMatcher URIMatcher;
	private static HashMap<String, String> databaseMap;
	private static DatabaseHelper databaseHelper;
	private static SQLiteDatabase database;
	
	@Override
	public boolean onCreate() {
		
		AUTHORITY = getContext().getPackageName() + ".provider.ambient_noise";
		
		URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], AMBIENT_NOISE);
		URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", AMBIENT_NOISE_ID);
		
		databaseMap = new HashMap<>();
		databaseMap.put(AmbientNoise_Data._ID, AmbientNoise_Data._ID);
		databaseMap.put(AmbientNoise_Data.TIMESTAMP, AmbientNoise_Data.TIMESTAMP);
		databaseMap.put(AmbientNoise_Data.DEVICE_ID, AmbientNoise_Data.DEVICE_ID);
		databaseMap.put(AmbientNoise_Data.FREQUENCY, AmbientNoise_Data.FREQUENCY);
		databaseMap.put(AmbientNoise_Data.DECIBELS, AmbientNoise_Data.DECIBELS);
		databaseMap.put(AmbientNoise_Data.RMS, AmbientNoise_Data.RMS);
		databaseMap.put(AmbientNoise_Data.IS_SILENT, AmbientNoise_Data.IS_SILENT);
        databaseMap.put(AmbientNoise_Data.RAW, AmbientNoise_Data.RAW);
		databaseMap.put(AmbientNoise_Data.SILENCE_THRESHOLD, AmbientNoise_Data.SILENCE_THRESHOLD);
		
		return true;
	}
	
	private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case AMBIENT_NOISE:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (URIMatcher.match(uri)) {
	        case AMBIENT_NOISE:
	            return AmbientNoise_Data.CONTENT_TYPE;
	        case AMBIENT_NOISE_ID:
	            return AmbientNoise_Data.CONTENT_ITEM_TYPE;
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
	    }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case AMBIENT_NOISE:
                long weather_id = database.insert(DATABASE_TABLES[0], AmbientNoise_Data.DEVICE_ID, values);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            AmbientNoise_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case AMBIENT_NOISE:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }
		
        int count = 0;
        switch (URIMatcher.match(uri)) {
            case AMBIENT_NOISE:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
}

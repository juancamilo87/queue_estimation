package com.aware.plugin.queuetracescollector.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

/**
 * Created by JuanCamilo on 5/7/2015.
 */
public class LocationDataSource {

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;


    private String[] allColumns = {
            MySQLiteHelper.COLUMN_SEARCH_LOCATION_ID,
            MySQLiteHelper.COLUMN_SEARCH_LOCATION_TIMESTAMP,
            MySQLiteHelper.COLUMN_SEARCH_LOCATION_LATITUDE,
            MySQLiteHelper.COLUMN_SEARCH_LOCATION_LONGITUDE
            };

    public LocationDataSource(Context context) {
        dbHelper = MySQLiteHelper.getHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long addSearchLocation(LatLng latlng) {
        if(latlng == null) {
            return -1;
        }
        else {
            ContentValues values = new ContentValues();
            values.put(MySQLiteHelper.COLUMN_SEARCH_LOCATION_TIMESTAMP, System.currentTimeMillis());
            values.put(MySQLiteHelper.COLUMN_SEARCH_LOCATION_LATITUDE, latlng.latitude);
            values.put(MySQLiteHelper.COLUMN_SEARCH_LOCATION_LONGITUDE, latlng.longitude);

            long insertId = database.insert(MySQLiteHelper.TABLE_SEARCH_LOCATION, null,
                    values);
            return insertId;
        }
    }

    public void cleanDB() {

        long min_timestamp = System.currentTimeMillis()-MySQLiteHelper.DAYS_LIMIT_IN_MILLIS_SEARCH;

        Cursor cursor = database.query(MySQLiteHelper.TABLE_SEARCH_LOCATION,
                allColumns, MySQLiteHelper.COLUMN_SEARCH_LOCATION_TIMESTAMP + " < " + min_timestamp, null,
                null, null, null);

        ArrayList<Long> ids = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ids.add(cursor.getLong(cursor.getColumnIndex(MySQLiteHelper.COLUMN_SEARCH_LOCATION_ID)));
            cursor.moveToNext();
        }
        cursor.close();
        for(int i = 0; i< ids.size(); i++)
        {
            database.delete(MySQLiteHelper.TABLE_SEARCH_LOCATION, MySQLiteHelper.COLUMN_SEARCH_LOCATION_ID
                    + " = " + ids.get(i), null);
        }
    }

    public boolean locationSearched(LatLng latLng, double distance)
    {
        double realDistance;
        Cursor cursor = getAllLocations();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            double latitude = cursor.getDouble(cursor.getColumnIndex(MySQLiteHelper.COLUMN_SEARCH_LOCATION_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndex(MySQLiteHelper.COLUMN_SEARCH_LOCATION_LONGITUDE));

            realDistance = SphericalUtil.computeDistanceBetween(new LatLng(latitude,longitude),latLng);
            if(distance>realDistance)
            {
                cursor.close();
                return true;
            }
            cursor.moveToNext();
        }
        cursor.close();
        return false;
    }

    public Cursor getAllLocations() {
        Cursor cursor = database.query(MySQLiteHelper.TABLE_SEARCH_LOCATION,
                allColumns, null, null, null, null, null);

        return cursor;
    }

}

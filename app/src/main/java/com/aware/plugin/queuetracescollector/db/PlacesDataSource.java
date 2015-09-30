package com.aware.plugin.queuetracescollector.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.aware.plugin.queuetracescollector.model.MyDBPlace;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JuanCamilo on 5/7/2015.
 */
public class PlacesDataSource {

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;


    private String[] allColumns = {
            MySQLiteHelper.COLUMN_PLACES_ID,
            MySQLiteHelper.COLUMN_PLACES_PLACE_ID,
            MySQLiteHelper.COLUMN_PLACES_TIMESTAMP,
            MySQLiteHelper.COLUMN_PLACES_LATITUDE,
            MySQLiteHelper.COLUMN_PLACES_LONGITUDE,
            MySQLiteHelper.COLUMN_PLACES_NAME
            };

    public PlacesDataSource(Context context) {
        dbHelper = MySQLiteHelper.getHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long addPlace(MyDBPlace place) {
        if(place == null) {
            return -1;
        }
        else {

            ContentValues values = new ContentValues();
            values.put(MySQLiteHelper.COLUMN_PLACES_PLACE_ID, place.getId());
            values.put(MySQLiteHelper.COLUMN_PLACES_TIMESTAMP, System.currentTimeMillis());
            values.put(MySQLiteHelper.COLUMN_PLACES_LATITUDE, place.getLatLng().latitude);
            values.put(MySQLiteHelper.COLUMN_PLACES_LONGITUDE, place.getLatLng().longitude);
            values.put(MySQLiteHelper.COLUMN_PLACES_NAME, place.getName());

            long insertId = database.insert(MySQLiteHelper.TABLE_PLACES, null,
                    values);

            return insertId;
        }
    }

    public void cleanDB() {

        long min_timestamp = System.currentTimeMillis()-MySQLiteHelper.DAYS_LIMIT_IN_MILLIS;

        Cursor cursor = database.query(MySQLiteHelper.TABLE_PLACES,
                allColumns, MySQLiteHelper.COLUMN_PLACES_TIMESTAMP + " < " + min_timestamp, null,
                null, null, null);

        ArrayList<Long> ids = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ids.add(cursor.getLong(cursor.getColumnIndex(MySQLiteHelper.COLUMN_PLACES_ID)));
            cursor.moveToNext();
        }
        cursor.close();
        for(int i = 0; i< ids.size(); i++)
        {
            database.delete(MySQLiteHelper.TABLE_PLACES, MySQLiteHelper.COLUMN_PLACES_ID
                    + " = " + ids.get(i), null);
        }
    }

    public List<MyDBPlace> getAllPlaces() {
        List<MyDBPlace> places = new ArrayList<>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_PLACES,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            MyDBPlace place = cursorToPlace(cursor);
            places.add(place);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return places;
    }

    private MyDBPlace cursorToPlace(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_PLACES_PLACE_ID));
        String name = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_PLACES_NAME));
        double latitude = cursor.getDouble(cursor.getColumnIndex(MySQLiteHelper.COLUMN_PLACES_LATITUDE));
        double longitude = cursor.getDouble(cursor.getColumnIndex(MySQLiteHelper.COLUMN_PLACES_LONGITUDE));

        LatLng latLng = new LatLng(latitude,longitude);

        MyDBPlace place = new MyDBPlace(name, latLng, id);

        return place;
    }


}

package com.aware.plugin.queuetracescollector.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.aware.plugin.queuetracescollector.app.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JuanCamilo on 5/7/2015.
 */
public class TempTracesDataSource {

    private SQLiteDatabase database;
    private MySQLiteHelper dbHelper;


    private String[] allColumns = {
            MySQLiteHelper.COLUMN_TEMP_TRACES_ID,
            MySQLiteHelper.COLUMN_TEMP_TRACES_DEVICE_ID,
            MySQLiteHelper.COLUMN_TEMP_TRACES_VENUE_ID,
            MySQLiteHelper.COLUMN_TEMP_TRACES_EVENT,
            MySQLiteHelper.COLUMN_TEMP_TRACES_OTHER,
            MySQLiteHelper.COLUMN_TEMP_TRACES_TIMESTAMP
            };

    public TempTracesDataSource(Context context) {
        dbHelper = MySQLiteHelper.getHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long addTrace(Trace trace) {

        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.COLUMN_TEMP_TRACES_DEVICE_ID, trace.getDevice_id());
        values.put(MySQLiteHelper.COLUMN_TEMP_TRACES_VENUE_ID, trace.getVenue_id());
        values.put(MySQLiteHelper.COLUMN_TEMP_TRACES_EVENT, trace.getEvent());
        values.put(MySQLiteHelper.COLUMN_TEMP_TRACES_OTHER, trace.getOther());
        values.put(MySQLiteHelper.COLUMN_TEMP_TRACES_TIMESTAMP, trace.getTimestamp());

        long insertId = database.insert(MySQLiteHelper.TABLE_TEMP_TRACES, null,
                values);

        return insertId;
    }

    public void cleanDB() {
//        database.execSQL("delete from "+ MySQLiteHelper.TABLE_TEMP_TRACES);

        database.delete(MySQLiteHelper.TABLE_TEMP_TRACES, null, null);

    }

    public List<Trace> getTempTraces() {
        List<Trace> traces = new ArrayList<>();

        Cursor cursor = database.query(MySQLiteHelper.TABLE_TEMP_TRACES,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Trace trace = cursorToTrace(cursor);
            traces.add(trace);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return traces;
    }

    private Trace cursorToTrace(Cursor cursor) {
        String device_id = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_TEMP_TRACES_DEVICE_ID));
        String venue_id = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_TEMP_TRACES_VENUE_ID));
        String event = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_TEMP_TRACES_EVENT));
        String other = cursor.getString(cursor.getColumnIndex(MySQLiteHelper.COLUMN_TEMP_TRACES_OTHER));
        long timestamp = cursor.getLong(cursor.getColumnIndex(MySQLiteHelper.COLUMN_TEMP_TRACES_TIMESTAMP));
        Trace trace = new Trace(device_id, venue_id, event, other, timestamp);

        return trace;
    }


}

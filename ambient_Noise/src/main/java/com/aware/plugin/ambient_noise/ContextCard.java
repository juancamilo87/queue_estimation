package com.aware.plugin.ambient_noise;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.plugin.ambient_noise.Provider.AmbientNoise_Data;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;

public class ContextCard implements IContextCard {

    /**
     * Constructor for Stream reflection
     */
    public ContextCard(){}

    public View getContextCard(Context context) {

		LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View card = sInflater.inflate(R.layout.ambient_layout, null);
        LinearLayout ambient_container = (LinearLayout) card.findViewById(R.id.ambient_plot);

		TextView frequency = (TextView) card.findViewById(R.id.frequency);
		TextView decibels = (TextView) card.findViewById(R.id.decibels);
		TextView ambient_noise = (TextView) card.findViewById(R.id.ambient_noise);
		TextView rms = (TextView) card.findViewById(R.id.rms);
		TextView rms_threshold = (TextView) card.findViewById(R.id.rms_threshold);

        Cursor latest = context.getContentResolver().query(AmbientNoise_Data.CONTENT_URI, null, AmbientNoise_Data.FREQUENCY + ">0 AND " + AmbientNoise_Data.RMS+">0", null, AmbientNoise_Data.TIMESTAMP + " DESC LIMIT 1");
        if( latest != null && latest.moveToFirst() ) {
            frequency.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.FREQUENCY))) + " Hz");
            decibels.setText(String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.DECIBELS))) + " dB");
            ambient_noise.setText(latest.getInt(latest.getColumnIndex(AmbientNoise_Data.IS_SILENT)) == 0 ? "Noisy" : "Silent");
            rms.setText("RMS: " + String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.RMS))));
            rms_threshold.setText("Threshold: "+String.format("%.1f", latest.getDouble(latest.getColumnIndex(AmbientNoise_Data.SILENCE_THRESHOLD))));
        }
        if( latest != null && ! latest.isClosed() ) latest.close();

        ambient_container.removeAllViews();
        ambient_container.addView(drawGraph(context));
        ambient_container.invalidate();

		return card;
	}
	
	private BarChart drawGraph( Context context ) {

        ArrayList<String> x_hours = new ArrayList<>();
        for(int i=0; i<24; i++) {
            x_hours.add(String.valueOf(i));
        }

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        ArrayList<BarEntry> dbEntries = new ArrayList<>();
        ArrayList<BarEntry> hzEntries = new ArrayList<>();

        Cursor latest = context.getContentResolver().query(
                AmbientNoise_Data.CONTENT_URI,
                new String[]{ "AVG(" + AmbientNoise_Data.DECIBELS +") as average_db",
                              "AVG(" + AmbientNoise_Data.FREQUENCY +") as average_hz",
                              "strftime('%H'," + AmbientNoise_Data.TIMESTAMP +"/1000, 'unixepoch','localtime')+0 as time_of_day" },
                AmbientNoise_Data.FREQUENCY + ">0 AND " + AmbientNoise_Data.RMS + ">0 AND " + AmbientNoise_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ",
                null,
                "time_of_day ASC");

        if( latest != null && latest.moveToFirst() ) {
            do {
                dbEntries.add(new BarEntry(latest.getFloat(0), latest.getInt(2)));
                hzEntries.add(new BarEntry(latest.getFloat(1), latest.getInt(2)));
            } while(latest.moveToNext());
        }
        if( latest != null && ! latest.isClosed() ) latest.close();

        BarDataSet dbData = new BarDataSet(dbEntries, "Average dB");
        dbData.setColor(Color.parseColor("#33B5E5"));
        dbData.setDrawValues(false);

        BarDataSet hzData = new BarDataSet(hzEntries, "Average Hz");
        hzData.setColor(Color.parseColor("#009688"));
        hzData.setDrawValues(false);

        ArrayList<BarDataSet> datasets = new ArrayList<>();
        datasets.add(dbData);
        datasets.add(hzData);

        BarData data = new BarData(x_hours, datasets);

        BarChart mChart = new BarChart(context);
        mChart.setContentDescription("");
        mChart.setDescription("");
        mChart.setMinimumHeight(200);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBorders(false);

        YAxis left = mChart.getAxisLeft();
        left.setDrawLabels(true);
        left.setDrawGridLines(true);
        left.setDrawAxisLine(true);

        YAxis right = mChart.getAxisRight();
        right.setDrawAxisLine(false);
        right.setDrawLabels(false);
        right.setDrawGridLines(false);

        XAxis bottom = mChart.getXAxis();
        bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        bottom.setSpaceBetweenLabels(0);
        bottom.setDrawGridLines(false);

        mChart.setData(data);
        mChart.invalidate();

        mChart.animateX(1000);

		return mChart;
	}
}

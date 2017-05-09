package com.udacity.stockhawk.ui;

import android.database.Cursor;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import timber.log.Timber;

public class StockChartActivity extends AppCompatActivity {

    public static final String SYMBOL_EXTRA = "SYMBOL";

    private String mSymbol;
    List<Entry> mEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_chart);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mSymbol = extras.getString(SYMBOL_EXTRA);
            if (mSymbol == null) {
                Timber.e("Symbol extra is null");
                finish();
                return;
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(mSymbol);

        Cursor result = getContentResolver().query(
                Contract.Quote.URI,
                new String[] { Contract.Quote.COLUMN_HISTORY }, // projection
                Contract.Quote.COLUMN_SYMBOL + " = ?", new String[] { mSymbol }, // selection
                null); // sortOrder

        if (result == null) {
            Timber.e("History query for symbol " + mSymbol + " returned null");
            finish();
            return;
        }

        if (result.getCount() == 0) {
            Timber.e("History query for symbol " + mSymbol + " returned no rows");
            result.close();
            finish();
            return;
        }

        result.moveToFirst();
        String historyData = result.getString(result.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
        String[] lines = TextUtils.split(historyData, "\n");
        ArrayList<String> dataPoints = new ArrayList<>();
        dataPoints.addAll(Arrays.asList(lines).subList(0, lines.length - 1));
        Collections.reverse(dataPoints);

        long referenceDate = -1;
        TreeMap<Float, String> dates = new TreeMap<>();
        for (String dataPoint : dataPoints) {
            String[] xy = TextUtils.split(dataPoint, ", ");
            if (xy.length != 2) {
                Timber.d("Invalid history data entry: " + dataPoint);
                continue;
            }
            try {
                long dateInMillis = Long.parseLong(xy[0]);
                long xLong = dateInMillis / 1000;
                if (referenceDate == -1) {
                    referenceDate = xLong;
                }
                float x = xLong - referenceDate;
                float y = Float.parseFloat(xy[1]);
                mEntries.add(new Entry(x, y));
                dates.put(x, DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(dateInMillis)));
            } catch (NumberFormatException e) {
                Timber.d("Invalid history data x or y value: " + Arrays.toString(xy));
            }
        }

        LineChart chart = (LineChart) findViewById(R.id.chart);
        LineDataSet dataSet = new LineDataSet(mEntries, mSymbol); // entries, label
        //dataSet.setColor(getResources().getColor(R.color.material_blue_500));
        //dataSet.setValueTextColor(R.color.material_blue_500);
        dataSet.setFillColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setCircleSize(2f);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new XAxisValueFormatter(dates));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.chart_axis_text));
        YAxis leftYAxis = chart.getAxisLeft();
        YAxis rightYAxis = chart.getAxisRight();
        leftYAxis.setTextColor(getResources().getColor(R.color.chart_axis_text));
        rightYAxis.setTextColor(getResources().getColor(R.color.chart_axis_text));
        chart.getLegend().setEnabled(false);
        chart.setDescription(null);
        chart.invalidate();

        result.close();

        Timber.d("Created stock chart activity for " + mSymbol);
    }

    private class XAxisValueFormatter implements IAxisValueFormatter {

        private TreeMap<Float, String> mValues;

        XAxisValueFormatter(TreeMap<Float, String> values) {
            this.mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            // "value" represents the position of the label on the axis (x or y)

            Map.Entry<Float, String> floorEntry = mValues.floorEntry(value);
            Map.Entry<Float, String> ceilEntry = mValues.ceilingEntry(value);

            float floorKeyDiff = (floorEntry != null) ? value - floorEntry.getKey() : Float.POSITIVE_INFINITY;
            float ceilKeyDiff = (ceilEntry != null) ? ceilEntry.getKey() - value : Float.POSITIVE_INFINITY;

            try {
                return (floorKeyDiff < ceilKeyDiff) ? floorEntry.getValue() : ceilEntry.getValue();
            } catch (NullPointerException e) {
                Timber.e("NullPointerException when trying to return formatted axis value");
                return "";
            }
        }
    }

}

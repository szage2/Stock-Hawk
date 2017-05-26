package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>{

    @BindView(R.id.chart)
    LineChart chart;

    List<String> stockDate = new ArrayList<>();
    List<Float> stockPrice = new ArrayList<>();

    Uri mStockUri;
    String mSymbol;
    String mHistoricalData;

    /** Identifier for the product data loader */
    private static final int STOCK_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        Intent intentThatStartedThisActivity = getIntent();
        Bundle bundle = intentThatStartedThisActivity.getExtras();

        if (bundle != null) {
            mSymbol = (String) bundle.get("SYMBOL");
        }
        mStockUri = Contract.Quote.makeUriForStock(mSymbol);

        getLoaderManager().initLoader(STOCK_LOADER, null, this);
    }

    private String formatDate(String dateInMilliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.valueOf(dateInMilliseconds));
        int date = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(calendar.YEAR);
        String fullDate = String.valueOf(date) + '/' + String.valueOf(month) + '/' + String.valueOf(year);
        return fullDate;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        String[] projection = Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{});

        String selection = Contract.Quote.COLUMN_SYMBOL + "='" + mSymbol + "'";

        String[] selectionArgs = new String[] {mSymbol};

        return new CursorLoader(this,
                mStockUri,
                projection,
                selection,
                selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            mHistoricalData = cursor.getString(cursor.getColumnIndexOrThrow(Contract.Quote.COLUMN_HISTORY));

            String[] historyData = mHistoricalData.split("\n");

            for (String data : historyData) {
                String[] splitHistoryData = data.split(",");
                stockDate.add(String.valueOf(formatDate(splitHistoryData[0])));
                stockPrice.add(Float.valueOf(splitHistoryData[1]));
            }
            setChart(historyData);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void setChart(String[] stringData) {

        List<Entry>  entries = new ArrayList<>();

        int numElements = stringData.length;

        for (int i = 0; i < numElements -1; i++) {
            entries.add(new Entry(i, stockPrice.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, mSymbol);
        dataSet.setCircleColor(Color.CYAN);
        dataSet.setValueTextColor(Color.CYAN);
        dataSet.setColor(Color.CYAN);
        dataSet.setCircleColor(Color.CYAN);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.CYAN);

        LineData lineData = new LineData(dataSet);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setData(lineData);
        chart.invalidate(); // refresh

        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setLabelRotationAngle(90);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return stockDate.get((int) value);
            }
        });

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextColor(Color.WHITE);
    }
}

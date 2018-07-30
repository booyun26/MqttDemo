package com.holux.iot.mqttdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.holux.iot.mqttdemo.db.ItemHR;
import com.holux.iot.mqttdemo.db.LocalDBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HrActivity extends Activity {
    static final public String EXTRA_DEVICE_ID = "device_id";
    static final public String ACTION_UPDATE = "com.holux.iot.mqttdemo.HrActivity.ACTION_UPDATE";

    static final private int CHART_Y_MAX = 200;
    static final private int CHART_Y_MIN= 40;

    private final int [] lineColor = {Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.MAGENTA};

    private LineChart lineChart;
    private String deviceID;
    private int idxColor = 0;
    private long curTime, minTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr);

        Intent it = getIntent();
        deviceID = it.getStringExtra(EXTRA_DEVICE_ID);

        findViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateChart();

        IntentFilter filter = new IntentFilter(ACTION_UPDATE);
        registerReceiver(receiverHrUpdate, filter);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(receiverHrUpdate);
        super.onPause();
    }

    private void findViews() {
        lineChart = findViewById(R.id.lineChart);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMaximum(CHART_Y_MAX);
        leftAxis.setAxisMinimum(CHART_Y_MIN);
        leftAxis.setLabelCount(16);
        leftAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if(value > (CHART_Y_MAX - 1))
                    return "bpm";
                else if(value < (CHART_Y_MIN + 1))
                    return "";
                else
                    return Integer.toString((int)value);
            }
        });

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setDrawAxisLine(false);
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawLabels(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(900);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
                return format.format(new Date((long)(value*1000 + minTime)/900000*900000));
            }
        });
        //xAxis.setAxisMinimum(0);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        lineChart.setDrawMarkers(false);
        Description description = new Description();
        description.setEnabled(false);
        lineChart.setDescription(description);
        //lineChart[i].setBackgroundColor(Color.WHITE);
        lineChart.setBackground(getResources().getDrawable(R.drawable.chart_border));
        lineChart.setHighlightPerTapEnabled(true);
        //lineChart.setNoDataText(getString(R.string.graph_no_data));
        //lineChart[i].setNoDataTextColor(Color.RED);
        Legend legend = lineChart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setFormToTextSpace(1);
        lineChart.setExtraBottomOffset(8);
    }

    private void updateChart() {
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();

        curTime = Calendar.getInstance().getTimeInMillis();
        long startTime = curTime - 3600000;

        // query log
        LocalDBHelper db = new LocalDBHelper(HrActivity.this, MainActivity.DB_NAME);
        List<ItemHR> listLog = db.queryHrByDevicePeriod(deviceID, startTime, curTime);
        db.close();

        List<Entry> entries = new ArrayList<Entry>();

        if(!listLog.isEmpty()) {
            int num = listLog.size();
            minTime = listLog.get(num - 1).getTime();

            if(curTime - minTime < 900000)
                minTime = curTime - 900000;

            for (int j = 0; j < num; j++) {
                ItemHR item = listLog.get(j);
                entries.add(new Entry((item.getTime() - minTime) / 1000, item.getBpm()));
            }

            lineChart.getXAxis().setAxisMinimum(-120);
            lineChart.getXAxis().setAxisMaximum((curTime - minTime)/1000 + 120);
        }
        else {
            entries.add(new Entry(0, 0));
        }

        LineDataSet dataSet = new LineDataSet(entries, deviceID);
        dataSet.setDrawCircles(true);
        dataSet.setDrawCircleHole(false);
        dataSet.setColor(lineColor[idxColor++]);
        if(idxColor == lineColor.length)
            idxColor = 0;
        dataSet.setLineWidth(2f);

        dataSets.add(dataSet);

        if(!dataSets.isEmpty()) {
            LineData lineData = new LineData(dataSets);
            lineData.setDrawValues(false);
            lineChart.setData(lineData);
        } else {
            if(lineChart.getLineData() != null) {
                lineChart.getLineData().removeDataSet(0);
            }
        }

        lineChart.invalidate(); // refresh
    }

    private BroadcastReceiver receiverHrUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateChart();
                }
            }, 1000);
        }
    };
}

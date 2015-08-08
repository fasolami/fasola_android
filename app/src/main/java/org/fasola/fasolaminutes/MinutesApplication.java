package org.fasola.fasolaminutes;

import android.app.Application;
import android.content.Context;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.utils.ValueFormatter;

/**
 * Application Override
 * Application-wide initialization code (e.g. Database)
 * Static getContext() for the application context
 */
public class MinutesApplication extends Application {
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        // Open the database
        MinutesDb.getInstance(mContext);
    }

    public static void applyDefaultChartStyle(BarLineChartBase chart) {
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.setDrawGridBackground(false);
        if (chart instanceof BarChart)
            ((BarChart) chart).setDrawBarShadow(false);
        // Avoid decimals
        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                return String.format("%d", (long) v);
            }
        };
        int labelCount = (int)(chart.getYChartMax() - chart.getYChartMin());
        if (labelCount < chart.getAxisLeft().getLabelCount())
            chart.getAxisLeft().setLabelCount(labelCount, true);
        if (labelCount < chart.getAxisRight().getLabelCount())
            chart.getAxisRight().setLabelCount(labelCount, true);
        chart.getAxisRight().setValueFormatter(formatter);
        chart.getAxisLeft().setValueFormatter(formatter);
        chart.getData().setValueFormatter(formatter);
        // Data color
        DataSet data = chart.getData().getDataSetByIndex(0);
        if (data != null)
            data.setColor(getContext().getResources().getColor(R.color.fasola_foreground));
        data = chart.getData().getDataSetByIndex(1);
        if (data != null)
            data.setColor(getContext().getResources().getColor(R.color.tab_background));
    }
}

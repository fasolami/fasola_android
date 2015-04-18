package org.fasola.fasolaminutes;

import android.app.Application;
import android.content.Context;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.utils.ValueFormatter;
import com.github.mikephil.charting.utils.XLabels;

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
        chart.setDrawLegend(false);
        chart.getXLabels().setCenterXLabelText(true);
        chart.getXLabels().setPosition(XLabels.XLabelPosition.BOTTOM);
        chart.setDrawHorizontalGrid(false);
        chart.setDrawVerticalGrid(false);
        chart.setDrawGridBackground(false);
        if (chart instanceof BarChart)
            ((BarChart) chart).setDrawBarShadow(false);
        // Avoid decimals
        int labelCount = (int)(chart.getYChartMax() - chart.getYChartMin());
        if (labelCount < chart.getYLabels().getLabelCount())
            chart.getYLabels().setLabelCount(labelCount);
        chart.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                return String.format("%d", (long) v);
            }
        });
        // Data color
        DataSet data = chart.getData().getDataSetByIndex(0);
        if (data != null)
            data.setColor(getContext().getResources().getColor(R.color.fasola_foreground));
    }
}

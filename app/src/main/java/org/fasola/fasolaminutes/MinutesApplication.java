package org.fasola.fasolaminutes;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Application Override
 * Application-wide initialization code (e.g. Database)
 * Static getContext() for the application context
 */
public class MinutesApplication extends Application
                                implements Application.ActivityLifecycleCallbacks {
    private static Context mContext;
    private static Activity mTopActivity;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        // Open the database
        MinutesDb.getInstance(mContext);
        registerActivityLifecycleCallbacks(this);
    }

    public final static int MIN_Y_AXIS = 4;
    public final static int MIN_X_AXIS_RANGE = 2; // must be even

    public static void applyDefaultChartStyle(BarLineChartBase chart) {
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.getAxisRight().setDrawZeroLine(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getAxisLeft().setDrawZeroLine(false);
        chart.setDrawGridBackground(false);
        if (chart instanceof BarChart)
            ((BarChart) chart).setDrawBarShadow(false);
        // Fix label count
        int labelCount = (int)(chart.getYChartMax() - chart.getYChartMin());
        if (labelCount < chart.getAxisLeft().getLabelCount())
            chart.getAxisLeft().setLabelCount(labelCount, true);
        if (labelCount < chart.getAxisRight().getLabelCount())
            chart.getAxisRight().setLabelCount(labelCount, true);
        // Avoid decimals
        chart.getData().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.format("%d", (long) value);
            }
        });
        // Datasets
        float maxLeft = 0;
        float maxRight = 0;
        for (int i = 0; i < chart.getData().getDataSetCount(); ++i) {
            DataSet data = (DataSet)chart.getData().getDataSetByIndex(i);
            // Data color
            if (i == 0)
                data.setColor(getContext().getResources().getColor(R.color.fasola_foreground));
            else if (i == 1)
                data.setColor(getContext().getResources().getColor(R.color.tab_background));
            if (data.getAxisDependency() == YAxis.AxisDependency.LEFT)
                maxLeft = Math.max(maxLeft, data.getYMax());
            else
                maxRight = Math.max(maxRight, data.getYMax());
        }
        // Set max y axis value
        if (maxLeft > 0 && maxLeft < MIN_Y_AXIS)
            chart.getAxisLeft().setAxisMaxValue(MIN_Y_AXIS);
        if (maxRight > 0 && maxRight < MIN_Y_AXIS)
            chart.getAxisRight().setAxisMaxValue(MIN_Y_AXIS);
        chart.getAxisRight().setAxisMinValue(0);
        chart.getAxisLeft().setAxisMinValue(0);
        // No zoom
        chart.setScaleEnabled(false);
        // Recalculate everything since we've changed min/max, etc.
        chart.notifyDataSetChanged();
    }

    public static Activity getTopActivity() {
        return mTopActivity;
    }

    //region Activity Lifecycle Callbacks
    // Keep track of activity stack
    //---------------------------------------------------------------------------------------------
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mTopActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mTopActivity == activity)
            mTopActivity = null;
    }
    //---------------------------------------------------------------------------------------------
    //endregion Activity Lifecycle Callbacks
}

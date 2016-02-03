package org.fasola.fasolaminutes;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

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

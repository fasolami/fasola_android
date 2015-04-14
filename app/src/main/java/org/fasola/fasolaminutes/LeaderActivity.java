package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ValueFormatter;
import com.github.mikephil.charting.utils.XLabels;

import java.util.ArrayList;

public class LeaderActivity extends SimpleTabActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_leader;
    }

    @Override
    public void onCreateTabs() {
        addTab("Stats", LeaderStatsFragment.class);
        addTab("Songs", LeaderSongFragment.class);
        addTab("Singings", LeaderSingingFragment.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Query for main data
        long id = getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
        SQL.Query query = SQL.select(C.Leader.fullName, C.Leader.aka).whereEq(C.Leader.id);
        getSupportLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
            @Override
            public void onLoadFinished(Cursor cursor) {
                MinutesContract.LeaderDAO leader = MinutesContract.Leader.fromCursor(cursor);
                if (leader != null) {
                    setTitle(leader.fullName.getString());
                    // Assemble the aka text
                    TextView akaText = (TextView) findViewById(R.id.leader_aka);
                    if (!leader.aka.isNull()) {
                        String aka = leader.aka.getString().replace(",", ", ").replaceAll(", ([^,]+)$", " and $1");
                        akaText.setText("also known as: " + aka);
                    } else
                        findViewById(R.id.leader_aka).setVisibility(View.GONE);
                }
            }
        });
    }

    static public class LeaderStatsFragment extends CursorFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_leader_stats, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            // Query for stats
            SQL.Query query = SQL.select(C.Leader.songCount, C.Leader.leadCount, C.Leader.singingCount)
                                 .whereEq(C.Leader.id);
            getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    MinutesContract.LeaderDAO leader = MinutesContract.Leader.fromCursor(cursor);
                    if (leader != null) {
                        int nSongs = leader.songCount.getInt();
                        int nTimes = leader.leadCount.getInt();
                        int nSingings = leader.singingCount.getInt();
                        String songsLed = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
                        String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
                        String singings = getResources().getQuantityString(R.plurals.singingsAttended, nSingings, nSingings);
                        View root = getView();
                        ((TextView) root.findViewById(R.id.songs)).setText(songsLed + ", " + timesLed);
                        ((TextView) root.findViewById(R.id.singings)).setText(singings);
                    }
                }
            });
            // Query for BarChart
            SQL.Query chartQuery = SQL.select(C.Leader.singingCount, C.Singing.year)
                                        .whereEq(C.Leader.id)
                                        .group(C.Singing.year)
                                        .orderAsc(C.Singing.year);
            getLoaderManager().initLoader(2, null, new MinutesLoader(chartQuery, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    // Chart entries
                    ArrayList<BarEntry> entries = new ArrayList<>();
                    int minYear = Integer.MAX_VALUE;
                    int maxYear = Integer.MIN_VALUE;
                    while (cursor.moveToNext()) {
                        int count = cursor.getInt(0);
                        int year = cursor.getInt(1);
                        if (year < minYear)
                            minYear = year;
                        if (year > maxYear)
                            maxYear = year;
                        entries.add(new BarEntry(count, year-minYear));
                    }
                    // Chart labels
                    ArrayList<String> labels = new ArrayList<>();
                    for(int i = minYear; i <= maxYear; ++i)
                        labels.add(String.valueOf(i));
                    // Set data
                    BarDataSet dataset = new BarDataSet(entries, "Singings Attended");
                    dataset.setColor(FasolaTabView.SELECTED_COLOR);
                    BarChart chart = (BarChart)getView().findViewById(R.id.chart);
                    chart.setDescription("");
                    chart.setData(new BarData(labels, dataset));
                    // Style the chart
                    chart.setDrawLegend(false);
                    chart.getXLabels().setCenterXLabelText(true);
                    chart.getXLabels().setPosition(XLabels.XLabelPosition.BOTTOM);
                    chart.setDrawHorizontalGrid(false);
                    chart.setDrawVerticalGrid(false);
                    chart.setDrawGridBackground(false);
                    chart.setDrawBarShadow(false);
                    int labelCount = (int)(chart.getYChartMax() - chart.getYChartMin());
                    if (labelCount < 6)
                        chart.getYLabels().setLabelCount(labelCount);
                    chart.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float v) {
                            return String.format("%d", (long) v);
                        }
                    });
                }
            });
        }
    }

    static public class LeaderSongFragment extends CursorListFragment {
        public LeaderSongFragment() {
            mItemLayoutId = R.layout.leader_list_item;
            mIntentClass = SongActivity.class;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            setQuery(C.Song.selectList(C.Song.fullName,
                                       C.LeaderStats.leadCount.format("'(' || {column} || ')'"))
                            .where(C.LeaderStats.leaderId, "=", id)
                            .order(C.LeaderStats.leadCount, "DESC", C.Song.pageSort, "ASC"));
        }
    }

    static public class LeaderSingingFragment extends CursorStickyListFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setItemLayout(android.R.layout.simple_list_item_2);
            setIntentActivity(SingingActivity.class);
            setRangeIndexer();
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            setQuery(C.Singing.selectList(C.Singing.name, C.Singing.location).distinct()
                        .sectionIndex(C.Singing.year)
                        .where(C.SongLeader.leaderId, "=", id));
        }
    }
}

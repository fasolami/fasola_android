/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;


public class SongActivity extends SimpleTabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
        setHelpResource(R.string.help_song_activity);
        // Query for main data
        long id = getIntent().getLongExtra(CursorListFragment.EXTRA_ID, -1);
        SQL.Query query = C.Song.select(C.Song.fullName).whereEq(C.Song.id);
        getSupportLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
            @Override
            public void onLoadFinished(Cursor cursor) {
                C.SongDAO song = C.Song.fromCursor(cursor);
                if (song != null) {
                    setTitle(song.fullName.getString());
                }
            }
        });
    }

    public interface SongFragment {
        void setSongId(long id);
    }

    public static class SongLeaderListFragment extends CursorListFragment implements SongFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(R.layout.list_item_leader);
            setIntentActivity(LeaderActivity.class);
            setSongId(getArguments().getLong(EXTRA_ID, -1));
        }

        public void setSongId(long songId) {
            getArguments().putLong(EXTRA_ID, songId);
            SQL.Query query = C.Leader.selectList(C.Leader.fullName,
                                C.LeaderStats.leadCount)
                            .whereEq(C.LeaderStats.songId)
                            .order(C.LeaderStats.leadCount, "DESC", C.Leader.lastName, "ASC")
                    .limit(20);
            setQuery(query, String.valueOf(songId));
        }
    }

    public static class SongWordsFragment extends Fragment implements SongFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_song_words, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setSongId(getArguments().getLong(CursorListFragment.EXTRA_ID, -1));
        }

        public void setSongId(long songId) {
            getArguments().putLong(CursorListFragment.EXTRA_ID, songId);
            SQL.Query query = C.Song.select(C.Song.lyrics, C.Song.poet, C.Song.composer,
                                            C.Song.key, C.Song.time, C.Song.meter)
                                    .whereEq(C.Song.id);
            getLoaderManager().restartLoader(1, null, new MinutesLoader(query, String.valueOf(songId)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    View view = getView();
                    C.SongDAO song = C.Song.fromCursor(cursor);
                    if (song != null) {
                        ((TextView) view.findViewById(R.id.words)).setText(Html.fromHtml(song.poet.getString()));
                        ((TextView) view.findViewById(R.id.tune)).setText(Html.fromHtml(song.composer.getString()));
                        ((TextView) view.findViewById(R.id.key)).setText(song.key.getString());
                        ((TextView) view.findViewById(R.id.time)).setText(song.time.getString());
                        ((TextView) view.findViewById(R.id.meter)).setText(song.meter.getString());
                        CharSequence lyrics = createIndentedText(song.lyrics.getString(), 0, 20);
                        ((TextView) view.findViewById(R.id.lyrics)).setText(lyrics);
                    }
                }
            });
        }

        // Add a hanging indent to a string
        protected SpannableString createIndentedText(String text, int marginFirstDp, int marginRestDp) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            marginFirstDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) marginFirstDp, metrics);
            marginRestDp = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float)marginRestDp, metrics);
            String[] lines = TextUtils.split(text, "\n");
            SpannableString result = new SpannableString(text);
            // Run through each line and add a LeadingMarginSpan
            int start = 0;
            for (String line : lines) {
                int end = start + line.length();
                result.setSpan(new LeadingMarginSpan.Standard(marginFirstDp, marginRestDp), start, end, 0);
                start = end + 1;
            }
            return result;
        }
    }

    public static class SongStatsFragment extends Fragment {
        private static final String BUNDLE_GRAPH_SETTING = "GRAPH_SETTING";
        private static final String BUNDLE_GRAPH_TITLE = "GRAPH_TITLE";
        int mGraphSettingId = -1;
        String mGraphTitle = "";

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // load saved data
            if (savedInstanceState != null) {
                mGraphSettingId = savedInstanceState.getInt(BUNDLE_GRAPH_SETTING);
                mGraphTitle = savedInstanceState.getString(BUNDLE_GRAPH_TITLE);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(BUNDLE_GRAPH_SETTING, mGraphSettingId);
            outState.putString(BUNDLE_GRAPH_TITLE, mGraphTitle);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setHasOptionsMenu(true);
            return inflater.inflate(R.layout.fragment_song_stats, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            long id = getSongId();
            // Stats summary
            SQL.Query query = C.Song.select(C.Song.leaderCount, C.Song.leadCount, C.Song.coleadCount)
                                    .whereEq(C.Song.id);
            getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    C.SongDAO song = C.Song.fromCursor(cursor);
                    if (song != null) {
                        int nLeaders = song.leaderCount.getInt();
                        int nTimes = song.leadCount.getInt();
                        int nColeads = song.coleadCount.getInt();
                        String leaders = getResources().getQuantityString(R.plurals.leaders, nLeaders, nLeaders);
                        String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
                        String coleads = getResources().getQuantityString(R.plurals.coleads, nColeads, nColeads);
                        ((TextView) view.findViewById(R.id.stats)).setText(
                                "Led " + timesLed + ", by " + leaders + " (" + coleads + ")");
                    }
                }
            });
            // Chart
            updateChart();
        }

        private long getSongId() {
            return getArguments().getLong(CursorListFragment.EXTRA_ID, -1);
        }

        private SQL.Query getChartQuery() {
            switch (mGraphSettingId) {
                case R.id.menu_graph_percent_leads:
                    return C.SongStats.select(C.SongStats.year, C.SongStats.leadPercent)
                        .whereEq(C.SongStats.songId)
                        .order(C.SongStats.year, "ASC");
                case R.id.menu_graph_song_rank:
                    return C.SongStats.select(C.SongStats.year, C.SongStats.rank)
                        .whereEq(C.SongStats.songId)
                        .order(C.SongStats.year, "ASC");
                case R.id.menu_graph_leads_per_year:
                default:
                    return C.SongStats.select(C.SongStats.year, C.SongStats.leadCount)
                        .whereEq(C.SongStats.songId)
                        .order(C.SongStats.year, "ASC");
            }
        }

        private void updateChart() {
            long id = getSongId();
            final CombinedChart chart = (CombinedChart)getView().findViewById(R.id.chart);
            chart.setNoDataText("");
            chart.setDescription("");
            final TextView chartTitle = (TextView)getView().findViewById(R.id.song_chart_title);
            getLoaderManager().initLoader(mGraphSettingId, null, new MinutesLoader(getChartQuery(), String.valueOf(id)) {

                private CombinedData getChartData(Cursor cursor) {
                    // Common
                    ArrayList<String> xVals = new ArrayList<>();
                    ArrayList<Entry> entries = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        entries.add(new Entry(cursor.getFloat(1), cursor.getPosition()));
                        xVals.add(cursor.getString(0));
                    }
                    CombinedData data = new CombinedData(xVals);
                    // by graph
                    switch (mGraphSettingId) {
                        case R.id.menu_graph_song_rank:
                            LineDataSet lineDataSet = new LineDataSet(entries, "");
                            lineDataSet.setDrawCircles(false);
                            lineDataSet.setLineWidth(2);
                            data.setData(new LineData(xVals, lineDataSet));
                            return data;
                        case R.id.menu_graph_percent_leads:
                        case R.id.menu_graph_leads_per_year:
                        default:
                            // Bar graphs need a BarEntry array
                            ArrayList<BarEntry> barEntries = new ArrayList<>();
                            for (Entry entry : entries)
                                barEntries.add(new BarEntry(entry.getVal(), entry.getXIndex()));
                            data.setData(new BarData(xVals, new BarDataSet(barEntries, "")));
                            return data;
                    }
                }

                private void styleChart() {
                    // Reset
                    chart.getAxisLeft().resetAxisMaxValue();
                    chart.getAxisLeft().resetAxisMinValue();
                    chart.getAxisLeft().setInverted(false);
                    // Graph-specific styles
                    switch (mGraphSettingId) {
                        case R.id.menu_graph_song_rank:
                            chart.getAxisLeft().setAxisMaxValue(C.SONG_COUNT);
                            chart.getAxisLeft().setAxisMinValue(1);
                            chart.getAxisLeft().setInverted(true);
                            break;
                    }
                    // Global styles
                    MinutesApplication.applyDefaultChartStyle(chart);
                }

                @Override
                public void onLoadFinished(Cursor cursor) {
                    chart.setData(getChartData(cursor));
                    styleChart();
                    chart.invalidate(); // redraw
                    // Update chart title
                    chartTitle.setText(mGraphTitle);
                }
            });
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_song_stats_fragment, menu);
            // Initial graph setting
            MenuItem item = menu.findItem(
                mGraphSettingId != -1 ? mGraphSettingId : R.id.menu_graph_leads_per_year);
            if (item != null) {
                item.setChecked(true);
                mGraphTitle = item.getTitle().toString();
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getGroupId() == R.id.menu_group_graph_settings) {
                item.setChecked(true);
                mGraphTitle = item.getTitle().toString();
                mGraphSettingId = item.getItemId();
                updateChart();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }

    public static class SongNeighborsFragment extends CursorListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(R.layout.list_item_song);
            setIntentActivity(SongActivity.class);
            long id = getArguments().getLong(EXTRA_ID, -1);
            setQuery(SQL.select(C.Song.id, C.Song.number, C.Song.fullTitle)
                        .join(C.SongNeighbor, C.Song.id, C.SongNeighbor.toId)
                        .where(C.SongNeighbor.fromId, "=", id));
        }
    }

    public static class SongRecordingsFragment extends CursorListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(R.layout.list_item_singing);
            setRangeIndexer();
            long id = getArguments().getLong(EXTRA_ID, -1);
            setQuery(SQL.select(C.SongLeader.id,
                    C.Leader.allNames,
                    C.Singing.year + " || ' ' || " + C.Singing.name,
                    C.Singing.location)
                    .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                    .sectionIndex(C.Singing.year)
                    .group(C.SongLeader.leadId)
                    .where(C.SongLeader.songId, "=", id)
                    .and(C.SongLeader.audioUrl, "IS NOT", "NULL"));
        }
    }
}

package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;

public class LeaderActivity extends SimpleTabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader);
        // Query for main data
        long id = getIntent().getLongExtra(CursorListFragment.EXTRA_ID, -1);
        SQL.Query query = SQL.select(C.Leader.fullName, C.Leader.aka).whereEq(C.Leader.id);
        getSupportLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
            @Override
            public void onLoadFinished(Cursor cursor) {
                MinutesContract.LeaderDAO leader = MinutesContract.Leader.fromCursor(cursor);
                if (leader != null) {
                    setTitle(leader.fullName.getString());
                }
            }
        });
        // Check for a lead id and switch to All Leads tab
        if (getIntent().getLongExtra(SingingActivity.EXTRA_LEAD_ID, -1) > -1) {
            int position = mSimplePagerAdapter.getFragmentIndex(LeaderLeadsFragment.class);
            mViewPager.setCurrentItem(position);
        }
    }

    static public class LeaderStatsFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_leader_stats, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            long id = getActivity().getIntent().getLongExtra(CursorListFragment.EXTRA_ID, -1);
            // Query for stats
            SQL.Query query = SQL.select(C.Leader.aka, C.Leader.songCount,
                                         C.Leader.leadCount, C.Leader.singingCount,
                                         C.Leader.entropyDisplay, C.Leader.majorPercent)
                                 .whereEq(C.Leader.id);
            getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    MinutesContract.LeaderDAO leader = MinutesContract.Leader.fromCursor(cursor);
                    if (leader != null) {
                        // AKA text
                        TextView akaText = (TextView) view.findViewById(R.id.aka);
                        if (!leader.aka.isNull()) {
                            String aka = leader.aka.getString().replace(",", ", ").replaceAll(", ([^,]+)$", " and $1");
                            akaText.setText("also known as: " + aka);
                        }
                        else
                            akaText.setVisibility(View.GONE);
                        // Stats
                        int nSongs = leader.songCount.getInt();
                        int nTimes = leader.leadCount.getInt();
                        int nSingings = leader.singingCount.getInt();
                        String songsLed = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
                        String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
                        String singings = getResources().getQuantityString(R.plurals.singingsAttended, nSingings, nSingings);
                        float major = leader.majorPercent.getFloat();
                        String majorText = major >= 0.5 ?
                                Math.round(100 * major) + "% Major" :
                                Math.round(100 * (1-major)) + "% Minor";
                        String entropy = "Entropy: " + leader.entropyDisplay.getString();
                        ((TextView) view.findViewById(R.id.songs)).setText(songsLed + ", " + timesLed);
                        ((TextView) view.findViewById(R.id.singings)).setText(singings);
                        ((TextView) view.findViewById(R.id.major_pct)).setText(majorText);
                        ((TextView) view.findViewById(R.id.entropy)).setText(entropy);
                    }
                }
            });
            // Query for BarChart
            final BarChart chart = (BarChart)view.findViewById(R.id.chart);
            chart.setNoDataText("");
            SQL.Query chartQuery = SQL.select(C.Leader.singingCount, C.Singing.year)
                                        .whereEq(C.Leader.id)
                                        .group(C.Singing.year)
                                        .orderAsc(C.Singing.year);
            getLoaderManager().initLoader(2, null, new MinutesLoader(chartQuery, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    // Chart data
                    ArrayList<BarEntry> entries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    int year = -1;
                    while (cursor.moveToNext()) {
                        int thisYear = cursor.getInt(1);
                        if (year == -1)
                            year = thisYear;
                        // Add x labels up to the current year (in cas of gaps between years)
                        for (; year <= thisYear; ++year)
                            labels.add(String.valueOf(year));
                        // Add y value for the current year
                        entries.add(new BarEntry(cursor.getInt(0), labels.size()-1));
                    }
                    // Set data
                    BarDataSet dataset = new BarDataSet(entries, "Singings Attended");
                    chart.setDescription("");
                    chart.setData(new BarData(labels, dataset));
                    // Style the chart
                    MinutesApplication.applyDefaultChartStyle(chart);
                    // Update -- if the query took a little while, sometimes the chart doesn't
                    // want to redraw, so we force it here.
                    chart.invalidate();
                }
            });
        }
    }

    static public class LeaderSongFragment extends CursorListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            setItemLayout(R.layout.leader_list_item);
            setIntentActivity(SongActivity.class);
            long id = getActivity().getIntent().getLongExtra(EXTRA_ID, -1);
            setQuery(C.Song.selectList(C.Song.fullName,
                                       C.LeaderStats.leadCount.format("'(' || {column} || ')'"))
                            .where(C.LeaderStats.leaderId, "=", id)
                            .order(C.LeaderStats.leadCount, "DESC", C.Song.pageSort, "ASC"));
            super.onViewCreated(view, savedInstanceState);
        }
    }

    static public class LeaderSingingFragment extends CursorStickyListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            setItemLayout(R.layout.singing_list_item);
            setIntentActivity(SingingActivity.class);
            setRangeIndexer();
            long id = getActivity().getIntent().getLongExtra(EXTRA_ID, -1);
            setQuery(C.Singing.selectList(C.Singing.name, C.Singing.startDate, C.Singing.location).distinct()
                        .sectionIndex(C.Singing.year)
                        .where(C.SongLeader.leaderId, "=", id));
            super.onViewCreated(view, savedInstanceState);
        }
    }

    static public class LeaderLeadsFragment extends CursorStickyListFragment {
        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            setItemLayout(R.layout.singing_list_item);
            setIntentActivity(SingingActivity.class);
            setRangeIndexer();
            long id = getActivity().getIntent().getLongExtra(EXTRA_ID, -1);
            setQuery(SQL.select(C.Singing.id, C.Song.fullName, C.Singing.name, C.Singing.startDate)
                    .select(C.SongLeader.leadId).as(SingingActivity.EXTRA_LEAD_ID)
                    .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                    .sectionIndex(C.Singing.year)
                    .where(C.SongLeader.leaderId, "=", id));
            super.onViewCreated(view, savedInstanceState);
        }

        @Override
        public void onLoadFinished(Cursor cursor) {
            // Highlight the Intent's lead id
            long leadId = getActivity().getIntent().getLongExtra(SingingActivity.EXTRA_LEAD_ID, -1);
            if (leadId > -1)
                setHighlight(cursor, SingingActivity.EXTRA_LEAD_ID, leadId);
            super.onLoadFinished(cursor);
        }

        @Override
        protected void setIntentData(Intent intent, int position, long id) {
            super.setIntentData(intent, position, id);
            Cursor cursor = getListAdapter().getCursor();
            if (cursor.moveToPosition(position)) {
                long leadId = cursor.getLong(cursor.getColumnIndex(SingingActivity.EXTRA_LEAD_ID));
                intent.putExtra(SingingActivity.EXTRA_LEAD_ID, leadId);
            }
        }
    }
}

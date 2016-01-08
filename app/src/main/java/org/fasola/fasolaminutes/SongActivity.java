package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;


public class SongActivity extends SimpleTabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);
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
            setItemLayout(R.layout.leader_list_item);
            setIntentActivity(LeaderActivity.class);
            long songId = getArguments().getLong(EXTRA_ID, -1);
            if (songId != -1)
                setSongId(songId);
        }

        public void setSongId(long id) {
            SQL.Query query = C.Leader.selectList(C.Leader.fullName,
                                C.LeaderStats.leadCount.format("'(' || {column} || ')'"))
                            .whereEq(C.LeaderStats.songId)
                            .order(C.LeaderStats.leadCount, "DESC", C.Leader.lastName, "ASC")
                    .limit(20);
            setQuery(query, String.valueOf(id));
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
            long id = getArguments().getLong(CursorListFragment.EXTRA_ID, -1);
            if (id != -1)
                setSongId(id);
        }

        public void setSongId(long id) {
            SQL.Query query = C.Song.select(C.Song.lyrics, C.Song.poet, C.Song.composer,
                                            C.Song.key, C.Song.time)
                                    .whereEq(C.Song.id);
            getLoaderManager().restartLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    View view = getView();
                    C.SongDAO song = C.Song.fromCursor(cursor);
                    if (song != null) {
                        ((TextView) view.findViewById(R.id.words)).setText(song.poet.getString());
                        ((TextView) view.findViewById(R.id.tune)).setText(song.composer.getString());
                        ((TextView) view.findViewById(R.id.key)).setText(song.key.getString());
                        ((TextView) view.findViewById(R.id.time)).setText(song.time.getString());
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
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_song_stats, container, false);
        }

        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            long id = getArguments().getLong(CursorListFragment.EXTRA_ID, -1);
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
            // Chart data
            final BarChart chart = (BarChart)view.findViewById(R.id.chart);
            chart.setNoDataText("");
            query = C.SongStats.select(C.SongStats.year, C.SongStats.leadCount)
                                           .whereEq(C.SongStats.songId)
                                           .order(C.SongStats.year, "ASC");
            getLoaderManager().initLoader(2, null, new MinutesLoader(query, String.valueOf(id)) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    // Get data
                    ArrayList<String> xVals = new ArrayList<>();
                    ArrayList<BarEntry> countVals = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        C.SongStatsDAO stats = C.SongStats.fromCursor(cursor);
                        countVals.add(new BarEntry(stats.leadCount.getInt(), xVals.size()));
                        xVals.add(stats.year.getString());
                    }
                    // Make chart data
                    BarDataSet countSet = new BarDataSet(countVals, "Times Led");
                    ArrayList<BarDataSet> dataSets = new ArrayList<>();
                    dataSets.add(countSet);
                    // Set chart data
                    BarData data = new BarData(xVals, dataSets);
                    chart.setDescription("");
                    chart.setData(data);
                    // Style chart
                    MinutesApplication.applyDefaultChartStyle(chart);
                    // Update -- if the query took a little while, sometimes the chart doesn't
                    // want to redraw, so we force it here.
                    chart.invalidate();
                }
            });
        }
    }

    public static class SongNeighborsFragment extends CursorListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(R.layout.song_list_item);
            setIntentActivity(SongActivity.class);
            long id = getArguments().getLong(EXTRA_ID, -1);
            setQuery(SQL.select(C.Song.id, C.Song.number, C.Song.title)
                        .join(C.SongNeighbor, C.Song.id, C.SongNeighbor.toId)
                        .where(C.SongNeighbor.fromId, "=", id));
        }
    }

    public static class SongRecordingsFragment extends CursorListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setItemLayout(R.layout.singing_list_item);
            setRangeIndexer();
            long id = getArguments().getLong(EXTRA_ID, -1);
            setQuery(SQL.select(C.SongLeader.id, C.Singing.name, C.Singing.startDate, C.Singing.location)
                        .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                        .sectionIndex(C.Singing.year)
                        .group(C.SongLeader.leadId)
                        .where(C.SongLeader.songId, "=", id)
                            .and(C.SongLeader.audioUrl, "IS NOT", "NULL"));
        }
    }
}

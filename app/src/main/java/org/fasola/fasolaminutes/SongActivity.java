package org.fasola.fasolaminutes;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;


public class SongActivity extends SimpleTabActivity {
    MinutesContract.SongDAO mSong;
    @Override
    protected int getLayoutId() {
        return R.layout.activity_song;
    }

    @Override
    protected void onCreateTabs() {
        addTab("Leaders", SongLeaderListFragment.class);
        addTab("Lyrics", LyricsFragment.class);
        addTab("Chart", ChartFragment.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long id = getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
        super.onCreate(savedInstanceState);
        // Query
        mSong = C.Song.get(id);
        if (mSong != null) {
            String songName = mSong.fullName.getString();
            setTitle(songName);
            String words = "PLACEHOLDER"; //mSong.POET.getString();
            String tune = "PLACEHOLDER"; //mSong.COMPOSER.getString();
            int nLeaders = mSong.leaderCount.getInt();
            int nTimes = mSong.leadCount.getInt();
            if (words.endsWith(", "))
                words = words.substring(0, words.length() - 2);
            if (tune.endsWith(", "))
                tune = tune.substring(0, tune.length() - 2);
            String leaders = getResources().getQuantityString(R.plurals.leaders, nLeaders, nLeaders);
            String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
            ((TextView) findViewById(R.id.song_title)).setText(songName);
            ((TextView) findViewById(R.id.words)).setText(words);
            ((TextView) findViewById(R.id.tune)).setText(tune);
            ((TextView) findViewById(R.id.stats)).setText("Led " + timesLed + ", by " + leaders);
        }
    }

    public static class SongLeaderListFragment extends CursorListFragment {
        public SongLeaderListFragment() {
            mItemLayoutId = R.layout.leader_list_item;
            mIntentClass = LeaderActivity.class;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            long songId = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            SQL.Query query = C.Leader.selectList(C.Leader.fullName,
                                C.LeaderStats.leadCount.format("'(' || {column} || ')'"))
                            .whereEq(C.LeaderStats.songId)
                            .order(C.LeaderStats.leadCount, "DESC", C.Leader.lastName, "ASC")
                            .limit(20);
            setQuery(query, String.valueOf(songId));
        }
    }

    public static class LyricsFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_song_lyrics, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // Query
            String lyrics = ((SongActivity) getActivity()).mSong.lyrics.getString();
            ((TextView) view.findViewById(R.id.lyrics)).setText(createIndentedText(lyrics, 0, 20));
            // Done
            super.onViewCreated(view, savedInstanceState);
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

    public static class ChartFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_song_chart, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // Query
            long songId = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            MinutesDb db = MinutesDb.getInstance(getActivity());
            String query = SQL.select(C.SongStats.year, C.SongStats.leadCount, C.SongStats.rank)
                                .from(C.SongStats)
                                .whereEq(C.SongStats.songId)
                                .order(C.SongStats.year, "ASC").toString();
            Cursor cursor = db.query(query, String.valueOf(songId));
            // Get data
            ArrayList<String> xVals = new ArrayList<>();
            ArrayList<Entry> rankVals = new ArrayList<>();
            ArrayList<Entry> countVals = new ArrayList<>();
            while (cursor.moveToNext()) {
                xVals.add(cursor.getString(0));
                countVals.add(new Entry(cursor.getInt(1), xVals.size()));
                rankVals.add(new Entry(cursor.getInt(2), xVals.size()));
            }
            cursor.close();
            // Make chart data
            LineDataSet countSet = new LineDataSet(countVals, "Times Led");
            countSet.setColor(Color.BLUE);
            LineDataSet rankSet = new LineDataSet(rankVals, "Rank");
            rankSet.setColor(Color.GRAY);
            ArrayList<LineDataSet> dataSets = new ArrayList<>();
            dataSets.add(countSet);
            dataSets.add(rankSet);
            // Set chart data
            LineData data = new LineData(xVals, dataSets);
            LineChart chart = (LineChart) view.findViewById(R.id.chart);
            chart.setData(data);
            super.onViewCreated(view, savedInstanceState);
        }
    }
}

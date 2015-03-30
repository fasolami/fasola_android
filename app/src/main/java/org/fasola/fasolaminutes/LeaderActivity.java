package org.fasola.fasolaminutes;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


public class LeaderActivity extends SimpleTabActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_leader;
    }

    @Override
    public void onCreateTabs() {
        addTab("Songs", LeaderSongFragment.class);
        addTab("Singings", LeaderSingingFragment.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Query for main data
        long id = getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
        MinutesContract.LeaderDAO leader = MinutesContract.Leader.get(id);
        if (leader != null) {
            // Assemble the aka text
            StringBuilder akaText = new StringBuilder();
            if (! leader.aka.isNull()) {
                String[] akaList = leader.aka.getString().split(",");
                if (akaList.length > 0) {
                    akaText.append("aka");
                    for (int i = 0; i < akaList.length; i++) {
                        akaText.append(" ").append(akaList[i]);
                        if (i < akaList.length - 2)
                            akaText.append(",");
                        else if (i == akaList.length - 2)
                            akaText.append(" and");
                    }
                }
            }
            setTitle(leader.fullName.getString());
            int nSongs = leader.songCount.getInt();
            int nTimes = leader.leadCount.getInt();
            int nSingings = leader.singingCount.getInt();
            String songsLed = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
            String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
            String singings = getResources().getQuantityString(R.plurals.singingsAttended, nSingings, nSingings);
            ((TextView) findViewById(R.id.leader_name)).setText(akaText.toString());
            if (leader.aka.isNull())
                findViewById(R.id.leader_name).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.songs)).setText(songsLed + ", " + timesLed);
            ((TextView) findViewById(R.id.singings)).setText(singings);
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
            SQL.Query query = C.Song.selectList(C.Song.fullName,
                                                C.LeaderStats.leadCount.format("'(' || {column} || ')'"))
                                    .whereEq(C.LeaderStats.leaderId)
                                    .order(C.LeaderStats.leadCount, "DESC", C.Song.pageSort, "ASC");

            setQuery(query, String.valueOf(id));
        }
    }

    static public class LeaderSingingFragment extends CursorStickyListFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mIntentClass = SingingActivity.class;
            SQL.Query query =
                SQL.select(C.Singing.id).distinct()
                    .select(C.Singing.name)
                    .select(C.Singing.location)
                    .sectionIndex(C.Singing.year)
                    .from(C.SongLeader)
                    .join(C.SongLeader, C.Singing)
                    .whereEq(C.SongLeader.leaderId);
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            setRangeIndexer();
            setQuery(query, String.valueOf(id));
        }
    }
}

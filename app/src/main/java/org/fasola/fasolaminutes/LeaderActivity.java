package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.support.v4.content.Loader;
import android.os.Bundle;
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
            String leaderName = leader.fullName.getString();
            setTitle(leaderName);
            int nSongs = leader.songCount.getInt();
            int nTimes = leader.leadCount.getInt();
            int nSingings = leader.singingCount.getInt();
            String songsLed = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
            String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
            String singings = getResources().getQuantityString(R.plurals.singingsAttended, nSingings, nSingings);
            ((TextView) findViewById(R.id.leader_name)).setText(leaderName);
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
        public LeaderSingingFragment() {
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mIntentClass = SingingActivity.class;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            SQL.Query query =
                SQL.select(C.Singing.id).distinct()
                    .select(C.Singing.name)
                    .select(C.Singing.location)
                    .sectionIndex(C.Singing.year)
                    .from(C.SongLeader)
                    .join(C.SongLeader, C.Singing)
                    .whereEq(C.SongLeader.leaderId);
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            setQuery(query, String.valueOf(id));
        }

        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            super.onLoadFinished(loader, cursor);
            // Find the index column
            IndexedCursorAdapter adapter = getListAdapter();
            int indexColumn = adapter.getIndexColumn();
            // Find the min and max values (assume sorted)
            cursor.moveToFirst();
            int min = cursor.getInt(indexColumn);
            cursor.moveToLast();
            int max = cursor.getInt(indexColumn);
            cursor.moveToFirst();
            // Create sections
            String[] sections = new String[max-min+1];
            for (int i = min; i <= max; i++)
                sections[i-min] = Integer.toString(i);
            // Set the indexer
            setIndexer(new StringIndexer(cursor, indexColumn, sections));
        }

    }
}

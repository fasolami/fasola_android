package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
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
        MinutesDb db = MinutesDb.getInstance();
        Cursor cursor = db.query(MinutesDb.LEADER_ACTIVITY_QUERY, new String[] {String.valueOf(id)});
        if (cursor.moveToFirst()) {
            String songName = cursor.getString(0);
            int nSongs = cursor.getInt(1);
            int nTimes = cursor.getInt(2);
            int nSingings = cursor.getInt(3);
            String songsLed = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
            String timesLed = getResources().getQuantityString(R.plurals.timesLed, nTimes, nTimes);
            String singings = getResources().getQuantityString(R.plurals.singingsAttended, nSingings, nSingings);
            ((TextView) findViewById(R.id.leader_name)).setText(songName);
            ((TextView) findViewById(R.id.songs)).setText(songsLed + ", " + timesLed);
            ((TextView) findViewById(R.id.singings)).setText(singings);
        }
        cursor.close();
    }

    static public class LeaderSongFragment extends CursorListFragment {
        public LeaderSongFragment() {
            mItemLayoutId = R.layout.leader_list_item;
            mIntentClass = SongActivity.class;
        }

        @Override
        public Cursor getCursor() {
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            return getDb().query(MinutesDb.LEADER_SONG_LIST_QUERY, new String[] {String.valueOf(id)});
        }
    }

    static public class LeaderSingingFragment extends CursorListFragment {
        public LeaderSingingFragment() {
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mIntentClass = SingingActivity.class;
        }

        @Override
        public Cursor getCursor() {
            String query =
                SQL.select("DISTINCT " + C.Singing._ID, C.Singing.NAME, C.Singing.LOCATION)
                    .from(C.SongLeader)
                    .join(C.SongLeader, C.Singing)
                    .whereEq(C.SongLeader.LEADER_ID).toString();
            long id = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            return getDb().query(query, new String[] {String.valueOf(id)});
        }
    }
}

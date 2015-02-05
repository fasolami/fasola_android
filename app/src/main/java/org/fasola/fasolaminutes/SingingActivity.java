package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class SingingActivity extends SimpleTabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long id = getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
        super.onCreate(savedInstanceState);
        // Query
        MinutesDb db = MinutesDb.getInstance();
        Cursor cursor = db.query(MinutesDb.SINGING_ACTIVITY_QUERY, new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String location = cursor.getString(1);
            int nSongs = cursor.getInt(2);
            int nLeaders = cursor.getInt(3);
            String songs = getResources().getQuantityString(R.plurals.songsLed, nSongs, nSongs);
            String leaders = getResources().getQuantityString(R.plurals.leaders, nLeaders, nLeaders);
            ((TextView) findViewById(R.id.title)).setText(name);
            ((TextView) findViewById(R.id.location)).setText(location);
            ((TextView) findViewById(R.id.songs)).setText(songs);
            ((TextView) findViewById(R.id.leaders)).setText(leaders);
        }
        cursor.close();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_singing;
    }

    @Override
    protected void onCreateTabs() {
        addTab("Songs", SingingSongListFragment.class);
        addTab("Full Text", FullTextFragment.class);
    }

    public static class SingingSongListFragment extends CursorListFragment {
        public SingingSongListFragment() {
            mItemLayoutId = android.R.layout.simple_list_item_2;
            mIntentClass = SongActivity.class;
        }
        @Override
        public Cursor getCursor() {
            long singingId = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            return getDb().query(MinutesDb.SINGING_LEADER_LIST_QUERY, new String[]{String.valueOf(singingId)});
        }
    }

    public static class FullTextFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_singing_text, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // Query
            long singingId = getActivity().getIntent().getLongExtra(MainActivity.EXTRA_ID, -1);
            MinutesDb db = MinutesDb.getInstance(getActivity());
            String query = SQL.select(C.Singing.FULL_TEXT).from(C.Singing).whereEq(C.Singing._ID).toString();
            String fullText = db.queryString(query, new String[]{String.valueOf(singingId)});
            ((TextView) view.findViewById(R.id.full_text)).setText(fullText.replace("\n", "\n\n"));
            // Done
            super.onViewCreated(view, savedInstanceState);
        }
    }
}

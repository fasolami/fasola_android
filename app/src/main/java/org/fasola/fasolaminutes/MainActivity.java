package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends SimpleTabActivity {
    public final static String ACTIVITY_POSITION = "org.fasola.fasolaminutes.POSITION";
    DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PlaybackService.isRunning())
            PlaybackService.getInstance().setMainTaskRunning(true);
        setContentView(R.layout.activity_main);
        // Save all the pages since the queries may take some time to run
        mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        // Set page change listener and initial settings
        setOnPageChangeListener(mPageChangeListener);
        // Setup drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // Drawer toggle callbacks
        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Close drawers first
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT))
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        else if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
        else
            super.onBackPressed();
    }

    // Change title and FaSoLa tabs when the page changes
    ViewPager.SimpleOnPageChangeListener mPageChangeListener =
        new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                setTitle(mPagerAdapter.getPageTitle(position));
                ((FasolaTabView) findViewById(R.id.fasola_tabs)).setSelection(position);
            }
        };

    @Override
    protected void onResume() {
        mPageChangeListener.onPageSelected(mViewPager.getCurrentItem());
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (PlaybackService.isRunning())
            PlaybackService.getInstance().setMainTaskRunning(false);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Change to the requested fragment (by position)
        int position = intent.getIntExtra(ACTIVITY_POSITION, -1);
        if (position != -1) {
            // Change page, or at least force the Activity to think the page was
            // changed so it updates fasola tabs and title
            if (mViewPager.getCurrentItem() != position)
                mViewPager.setCurrentItem(position, true);
            else
                mPageChangeListener.onPageSelected(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.v("MainActivity", "onPrepareOptionsMenu called");
        Log.v("MainActivity", String.valueOf(menu.hasVisibleItems()));
        return super.onPrepareOptionsMenu(menu);
    }

    public static class LeaderListFragment extends CursorStickyListFragment {
        protected int mSortId = R.id.menu_leader_sort_name;
        protected final static String BUNDLE_SORT = "SORT_ID";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null)
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, mSortId);
            setHasOptionsMenu(true);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setIntentActivity(LeaderActivity.class);
            setItemLayout(R.layout.leader_list_item);
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
            super.onSaveInstanceState(saveInstanceState);
            saveInstanceState.putSerializable(BUNDLE_SORT, mSortId);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_leader_fragment, menu);
            // Check the initial sort
            MenuItem item = menu.findItem(mSortId);
            if (item != null)
                item.setChecked(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Sort
            if (item.getGroupId() == R.id.menu_group_leader_sort) {
                item.setChecked(true);
                mSortId = item.getItemId();
                updateQuery();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        // Change query/index based on the selected sort column

        @Override
        public SQL.Query onUpdateQuery() {
            switch(mSortId) {
                case R.id.menu_leader_sort_count:
                    setBins(0, 10, 50, 100, 500, 1000);
                    showHeaders(false);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                   .sectionIndex(C.Leader.leadCount, "DESC")
                                   .order(C.Leader.lastName, "ASC", C.Leader.fullName, "ASC");
                case R.id.menu_leader_sort_entropy:
                    setBins(0, 10, 20, 30, 40, 50, 60, 70, 80, 90);
                    setSectionLabels("0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9");
                    showHeaders(false);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.entropyDisplay.format("'(' || {column} || ')'"))
                                   .sectionIndex(C.Leader.entropy.format("CAST({column} * 100 AS INT)"), "DESC")
                                   .order(C.Leader.entropy, "DESC",
                                           C.Leader.lastName, "ASC",
                                           C.Leader.fullName, "ASC");
                case R.id.menu_leader_sort_first_name:
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                   .sectionIndex(C.Leader.fullName, "ASC");
                case R.id.menu_leader_sort_name:
                default:
                    setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                    showHeaders(true);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount.format("'(' || {column} || ')'"))
                                   .sectionIndex(C.Leader.lastName, "ASC")
                                   .order(C.Leader.fullName, "ASC");
            }
        }

        @Override
        public SQL.Query onUpdateSearch(SQL.Query query, String searchTerm) {
            return query.where(C.Leader.fullName, "LIKE", "%" + searchTerm + "%")
                        .or(C.LeaderAlias.alias, "LIKE", "%" + searchTerm + "%");
        }
    }

    public static class SongListFragment extends CursorStickyListFragment {
        protected int mSortId = R.id.menu_song_sort_page;
        protected final static String BUNDLE_SORT = "SORT_ID";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, mSortId);
            }
            setHasOptionsMenu(true);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setIntentActivity(SongActivity.class);
            setItemLayout(R.layout.song_list_item);
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
            super.onSaveInstanceState(saveInstanceState);
            saveInstanceState.putSerializable(BUNDLE_SORT, mSortId);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_song_fragment, menu);
            // Check the initial sort
            MenuItem item = menu.findItem(mSortId);
            if (item != null)
                item.setChecked(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Sort
            if (item.getGroupId() == R.id.menu_group_song_sort) {
                item.setChecked(true);
                mSortId = item.getItemId();
                updateQuery();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        /**
         * Get the default query for filtering or sorting
         * @return {SQL.Query} Default song query
         */
        private SQL.Query songQuery() {
            return C.Song.selectList(C.Song.number, C.Song.title,
                                     C.SongStats.leadCount.sum().format("'(' || {column} || ')'"));
        }

        // Code common to onUpdateQuery and onUpdateSearch
        private SQL.Query setQueryOrder(SQL.Query query, boolean setSectionIndex) {
            switch(mSortId) {
                case R.id.menu_song_sort_title:
                    if (setSectionIndex) {
                        setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                        showHeaders(true);
                        return query.sectionIndex(C.Song.title, "ASC");
                    }
                    else
                        return query.order(C.Song.title, "ASC");
                case R.id.menu_song_sort_leads:
                    if (setSectionIndex) {
                        setBins(100, 500, 1000, 1500, 2000, 2500, 3000);
                        showHeaders(false);
                        return query.sectionIndex(C.SongStats.leadCount.sum(), "DESC");
                    }
                    else
                        return query.order(C.SongStats.leadCount.sum(), "DESC");
                case R.id.menu_song_sort_key:
                    if (setSectionIndex) {
                        setStringIndexer();
                        showHeaders(true);
                        return query.sectionIndex(C.Song.key, "ASC");
                    }
                    else
                        return query.order(C.Song.key, "ASC");
                case R.id.menu_song_sort_time:
                    if (setSectionIndex) {
                        setStringIndexer();
                        showHeaders(true);
                        return query.sectionIndex(C.Song.time, "ASC");
                    }
                    else
                        return query.order(C.Song.time, "ASC");
                case R.id.menu_song_sort_meter:
                    if (setSectionIndex) {
                        setStringIndexer();
                        showHeaders(true);
                        return query.sectionIndex(C.Song.meter)
                                    .orderAsc(C.Song.meter.cast("INT"))
                                    .orderAsc(C.Song.meter);
                    }
                    else
                        return query.orderAsc(C.Song.meter.cast("INT"))
                                    .orderAsc(C.Song.meter);
                case R.id.menu_song_sort_page:
                default:
                    if (setSectionIndex) {
                        setBins(0, 100, 200, 300, 400, 500);
                        showHeaders(false);
                        return query.sectionIndex(C.Song.pageSort);
                    }
                    else
                        return query.order(C.Song.pageSort, "ASC");
            }
        }

        // Change query/index based on the selected sort column
        public SQL.Query onUpdateQuery() {
            return setQueryOrder(songQuery(), true);
        }

        @Override
        public SQL.Query onUpdateSearch(SQL.Query query, String searchTerm) {
            searchTerm = DatabaseUtils.sqlEscapeString("%" + searchTerm + "%");
            showHeaders(true);
            setAlphabet("0123");
            setSectionLabels("Title", "Composer", "Poet", "Words");
            query = songQuery().sectionIndex(
                    new SQL.QueryColumn(
                            "CASE ",
                            C.Song.fullName.format("WHEN {column} LIKE %s THEN 0 ", searchTerm),
                            C.Song.composer.format("WHEN {column} LIKE %s THEN 1 ", searchTerm),
                            C.Song.poet.format("WHEN {column} LIKE %s THEN 2  ", searchTerm),
                            C.Song.lyrics.format("WHEN {column} LIKE %s THEN 3 ", searchTerm),
                            "END"
                    ), "ASC")
                .where(SQL.INDEX_COLUMN, "IS NOT", "NULL");
            // Add the additional order clause
            return setQueryOrder(query, false);
        }
    }

    public static class SingingListFragment extends CursorStickyListFragment {
        protected int mSortId = R.id.menu_singing_sort_year;
        protected final static String BUNDLE_SORT = "SORT_ID";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null)
                mSortId = savedInstanceState.getInt(BUNDLE_SORT, mSortId);
            setHasOptionsMenu(true);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setIntentActivity(SingingActivity.class);
            setItemLayout(R.layout.singing_list_item);
            updateQuery();
        }

        @Override
        public void onSaveInstanceState(final Bundle saveInstanceState) {
            super.onSaveInstanceState(saveInstanceState);
            saveInstanceState.putSerializable(BUNDLE_SORT, mSortId);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_singing_fragment, menu);
            // Check the initial sort
            MenuItem item = menu.findItem(mSortId);
            if (item != null)
                item.setChecked(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Sort
            if (item.getGroupId() == R.id.menu_group_singing_sort) {
                item.setChecked(true);
                mSortId = item.getItemId();
                updateQuery();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        /**
         * Get the default query for filtering or sorting
         * @return {SQL.Query} Default singing query
         */
        private SQL.Query singingQuery() {
            return C.Singing.selectList(C.Singing.name, C.Singing.startDate, C.Singing.location)
                            .select(C.Singing.recordingCount).as(CursorListFragment.AUDIO_COLUMN);
        }

        public SQL.Query onUpdateQuery() {
            switch(mSortId) {
                case R.id.menu_singing_sort_recordings:
                    showHeaders(false);
                    return singingQuery().where(C.Singing.recordingCount, ">", "0")
                                         .orderDesc(C.Singing.recordingCount)
                                         .orderAsc(C.Singing.year);
                case R.id.menu_singing_sort_year:
                default:
                    setRangeIndexer();
                    showHeaders(true);
                    return singingQuery().sectionIndex(C.Singing.year);
            }
        }

        @Override
        public SQL.Query onUpdateSearch(SQL.Query query, String searchTerm) {
            return query.where(C.Singing.name, "LIKE", "%" + searchTerm + "%")
                        .or(C.Singing.location, "LIKE", "%" + searchTerm + "%");
        }

        @Override
        public void onPlayClick(View v, int position) {
            // Get singing id
            Cursor cursor = getListAdapter().getCursor();
            cursor.moveToPosition(position);
            int singingId = cursor.getInt(0);
            // Query for songs
            SQL.Query query = C.SongLeader.select(C.SongLeader.leadId)
                                .select(C.SongLeader.audioUrl).as(CursorListFragment.AUDIO_COLUMN)
                                .where(C.SongLeader.singingId, "=", singingId)
                                    .and(C.SongLeader.audioUrl, "IS NOT", "NULL")
                                .group(C.SongLeader.leadId)
                                .order(C.SongLeader.singingOrder, "ASC");
            // Start query and play when finished
            getLoaderManager().restartLoader(100, null, new MinutesLoader(query) {
                @Override
                public void onLoadFinished(Cursor cursor) {
                    playSongs(cursor, 0);
                }
            });
        }
    }
}
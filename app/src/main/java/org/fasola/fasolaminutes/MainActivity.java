/*
 * This file is part of FaSoLa Minutes for Android.
 * Copyright (c) 2016 Mike Richards. All rights reserved.
 */

package org.fasola.fasolaminutes;

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends SimpleTabActivity {
    public final static String ACTIVITY_POSITION = "org.fasola.fasolaminutes.POSITION";

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
    protected void handleIntent(Intent intent, boolean isFirst) {
        super.handleIntent(intent, isFirst);
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
        if (! isFirst)
            return;
        // Recordings deep link:
        // fasola://recordings?singing=singingId
        Uri data = intent.getData();
        if (data != null &&
                "fasola".equals(data.getScheme()) &&
                "recordings".equals(data.getHost())) {
            // Got a recordings url
            if (data.getLastPathSegment().equals("random")) {
                // Start a random singing
                SQL.Query query = SQL.select(C.Singing.id)
                                     .where(C.Singing.recordingCount, ">", "10")
                                     .order("RANDOM()")
                                     .limit(1);
                long singingId = MinutesDb.getInstance().queryLong(query.toString());
                if (singingId > -1)
                    PlaybackService.playSinging(this, PlaybackService.ACTION_PLAY_MEDIA, singingId);
            }
            else {
                try {
                    // Start the specified singing
                    long singingId = Long.parseLong(data.getQueryParameter("singing"));
                    PlaybackService.playSinging(this, PlaybackService.ACTION_PLAY_MEDIA, singingId);
                } catch (NumberFormatException | UnsupportedOperationException ex) {
                    Log.w("MainActivity", "Bad url: " + data.toString());
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public static class LeaderListFragment extends CursorStickyListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setMenuResource(R.menu.menu_leader_list_fragment);
            setDefaultSortId(R.id.menu_leader_sort_name);
            setIntentActivity(LeaderActivity.class);
            setItemLayout(R.layout.list_item_leader);
            ((SimpleTabActivity)getActivity()).setHelpResource(this, R.string.help_leader_list);
            updateQuery();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item.getItemId() == R.id.menu_leader_correction) {
                startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(LeaderActivity.CORRECTIONS_URL)));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public SQL.Query onUpdateQuery() {
            switch(mSortId) {
                case R.id.menu_leader_sort_count:
                    setBinCount(7);
                    showHeaders(false);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount)
                                   .sectionIndex(C.Leader.leadCount, "DESC")
                                   .order(C.Leader.lastName, "ASC", C.Leader.fullName, "ASC");
                case R.id.menu_leader_sort_entropy:
                    setBins(0, 10, 20, 30, 40, 50, 60, 70, 80, 90);
                    setSectionLabels("0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9");
                    showHeaders(false);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.entropyDisplay)
                                   .sectionIndex(C.Leader.entropy.format("CAST({column} * 100 AS INT)"), "DESC")
                                   .order(C.Leader.entropy, "DESC",
                                           C.Leader.lastName, "ASC",
                                           C.Leader.fullName, "ASC");
                case R.id.menu_leader_sort_first_name:
                    setAlphabetIndexer();
                    showHeaders(true);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount)
                                   .sectionIndex(C.Leader.fullName, "ASC");
                case R.id.menu_leader_sort_name:
                default:
                    setAlphabetIndexer();
                    showHeaders(true);
                    return C.Leader.selectList(C.Leader.fullName, C.Leader.leadCount)
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
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setMenuResource(R.menu.menu_song_list_fragment);
            setDefaultSortId(R.id.menu_song_sort_page);
            setIntentActivity(SongActivity.class);
            setItemLayout(R.layout.list_item_song);
            ((SimpleTabActivity)getActivity()).setHelpResource(this, R.string.help_song_list);
            updateQuery();
        }

        /**
         * Get the default query for filtering or sorting
         * @return {SQL.Query} Default song query
         */
        private SQL.Query songQuery() {
            return C.Song.selectList(C.Song.number, C.Song.fullTitle,
                                     C.SongStats.leadCount.sum());
        }

        /** Get the column for the search query **/
        private SQL.Column searchColumn(String searchTerm, Object val1, Object val2, Object val3, Object val4) {
            return new SQL.QueryColumn(
                "CASE ",
                    C.Song.fullName.format("WHEN {column} LIKE %s THEN %s ", searchTerm, searchColumn_fixValue(val1)),
                    C.Song.composer.format("WHEN {column} LIKE %s THEN %s ", searchTerm, searchColumn_fixValue(val2)),
                    C.Song.poet.format("WHEN {column} LIKE %s THEN %s ", searchTerm, searchColumn_fixValue(val3)),
                    C.Song.lyrics.format("WHEN {column} LIKE %s THEN %s ", searchTerm, searchColumn_fixValue(val4)),
                "END"
            );
        }

        // Add quotes around unquoted strings
        private String searchColumn_fixValue(Object value) {
            String strVal = value.toString();
            if (value instanceof CharSequence && !strVal.contains("'"))
                return "'" + strVal + "'";
            else
                return strVal;
        }

        // Change query/index based on the selected sort column
        public SQL.Query onUpdateQuery() {
            SQL.Query query = songQuery();
            switch(getSortId()) {
                default:
                case R.id.menu_song_sort_page:
                    setBins(0, 100, 200, 300, 400, 500);
                    showHeaders(false);
                    return query.sectionIndex(C.Song.pageSort);
                case R.id.menu_song_sort_title:
                    setAlphabetIndexer();
                    showHeaders(true);
                    return query.sectionIndex(C.Song.title, "ASC");
                case R.id.menu_song_sort_leads:
                    setBinCount(7);
                    showHeaders(false);
                    return query.sectionIndex(C.SongStats.leadCount.sum(), "DESC");
                case R.id.menu_song_sort_key:
                    setStringIndexer();
                    showHeaders(true);
                    return query.sectionIndex(C.Song.key, "ASC");
                case R.id.menu_song_sort_time:
                    setStringIndexer();
                    showHeaders(true);
                    return query.sectionIndex(C.Song.time, "ASC");
                case R.id.menu_song_sort_meter:
                    setStringIndexer();
                    showHeaders(true);
                    return query.sectionIndex(C.Song.meter)
                                .orderAsc(C.Song.meter.cast("INT"))
                                .orderAsc(C.Song.meter);

            }
        }

        @Override
        public SQL.Query onUpdateSearch(SQL.Query query, String searchTerm) {
            searchTerm = DatabaseUtils.sqlEscapeString("%" + searchTerm + "%");
            // Start with standard query ordered by search sections
            query = songQuery().orderAsc(searchColumn(searchTerm, 0, 1, 2, 3));
            showHeaders(true);
            setStringIndexer();
            // Add custom sorting options
            switch(getSortId()) {
                default:
                case R.id.menu_song_sort_page:
                    query.order(C.Song.pageSort, "ASC");
                    break;
                case R.id.menu_song_sort_title:
                    query.order(C.Song.title, "ASC");
                    break;
                case R.id.menu_song_sort_leads:
                    query.order(C.SongStats.leadCount.sum(), "DESC");
                    break;
                case R.id.menu_song_sort_key:
                    query.order(C.Song.key, "ASC")
                         .sectionIndex(searchColumn(searchTerm, C.Song.key, "Composer", "Poet", "Words"));
                    break;
                case R.id.menu_song_sort_time:
                    query.order(C.Song.time, "ASC")
                         .sectionIndex(searchColumn(searchTerm, C.Song.time, "Composer", "Poet", "Words"));
                    break;
                case R.id.menu_song_sort_meter:
                    setStringIndexer();
                    query.orderAsc(C.Song.meter.cast("INT"))
                         .orderAsc(C.Song.meter)
                         .sectionIndex(searchColumn(searchTerm, C.Song.meter, "Composer", "Poet", "Words"));
                    break;
            }
            // Add the standard search index if none has been added
            if (!query.hasSectionIndex())
                query.sectionIndex(searchColumn(searchTerm, "Title", "Composer", "Poet", "Words"));
            // Last sort should always be by page
            query.orderAsc(C.Song.pageSort);
            // CASE WHEN can exclude rows, and NULL will make indexer break
            return query.where(SQL.INDEX_COLUMN, "IS NOT", "NULL");
        }
    }

    public static class SingingListFragment extends CursorStickyListFragment {
        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setMenuResource(R.menu.menu_singing_list_fragment);
            setDefaultSortId(R.id.menu_singing_sort_year);
            setIntentActivity(SingingActivity.class);
            setItemLayout(R.layout.list_item_singing);
            ((SimpleTabActivity)getActivity()).setHelpResource(this, R.string.help_singing_list);
            updateQuery();
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
            Cursor cursor = getListAdapter().getCursor();
            cursor.moveToPosition(position);
            int singingId = cursor.getInt(0);
            PlaybackService.playSinging(getActivity(), PlaybackService.ACTION_PLAY_MEDIA, singingId);
        }

        @Override
        public boolean onPlayLongClick(View v, int position) {
            Cursor cursor = getListAdapter().getCursor();
            cursor.moveToPosition(position);
            int singingId = cursor.getInt(0);
            PlaybackService.playSinging(getActivity(), PlaybackService.ACTION_ENQUEUE_MEDIA, singingId);
            return true;
        }
    }
}
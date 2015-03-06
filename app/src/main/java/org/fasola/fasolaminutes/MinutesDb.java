package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * The database class
 */
public class MinutesDb {
    // Singleton
    private static MinutesDb instance = null;

    private MinutesDb() {
    }

    public static MinutesDb getInstance() {
        if (instance == null)
            instance = new MinutesDb();
        return instance;
    }

    public static MinutesDb getInstance(Context context) {
        getInstance();
        instance.open(context);
        return instance;
    }

    // Raw database operations
    private SQLiteDatabase db = null;
    private MinutesDbHelper mHelper = null; // See below

    public SQLiteDatabase open(Context context) {
        if (db == null) {
            if (mHelper == null)
                mHelper = new MinutesDbHelper(context);
            db = mHelper.getReadableDatabase();
            // Change to true when data is changed -- takes a while to copy the db
            if (false) {
                db.setVersion(-1);
                db.close();
                db = mHelper.getReadableDatabase();
            }
        }
        return db;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    // Don't just use db.close(), close through the helper class.
    public void close() {
        if (db != null) {
            mHelper.close();
            db = null;
        }
    }

    // Queries
    // ------------------------------------------------------------------------------------------
    public static final String TEXT1 = "text1";
    public static final String TEXT2 = "text2";
    public static final String TEXT3 = "text3";

    public static final String SONG_ID_QUERY =
        SQL.select(C.Song._ID)
            .from(C.Song)
            .whereEq(C.Song.NUMBER).toString();
/*
    public static final String LEADER_ID_QUERY =
        SQL.select(C.Leader._ID)
            .from(C.Leader)
            .whereEq(C.Leader.FIRST_NAME, C.Leader.LAST_NAME).toString();
*/
    public static final SQL.Query LEADER_LIST_QUERY =
        SQL.select(C.Leader._ID)
                .select(C.Leader.LAST_NAME).as(IndexedCursorAdapter.INDEX_COLUMN)
                .select(C.Leader.FULL_NAME)
                .select("'(' || " + C.Leader.LEAD_COUNT + " || ')'")
            .from(C.Leader)
            .order(C.Leader.LAST_NAME, "ASC");

    public static final SQL.Query LEADER_LIST_QUERY_LEAD_COUNT =
        SQL.select(C.Leader._ID)
                .select(C.Leader.LEAD_COUNT + " / 100").as(IndexedCursorAdapter.INDEX_COLUMN)
                .select(C.Leader.FULL_NAME)
                .select("'(' || " + C.Leader.LEAD_COUNT + " || ')'")
            .from(C.Leader)
            .order(C.Leader.LEAD_COUNT, "DESC");

    public static final SQL.Query LEADER_LIST_QUERY_ENTROPY =
        SQL.select(C.Leader._ID)
                .select(C.Leader.ENTROPY + " * 10").as(IndexedCursorAdapter.INDEX_COLUMN)
                .select(C.Leader.FULL_NAME)
                .select("'(' || " + C.Leader.ENTROPY.func("ROUND", "4") + " || ')'")
            .from(C.Leader)
            .order(C.Leader.ENTROPY, "DESC");

    public static final String LEADER_ACTIVITY_QUERY =
        SQL.select(C.Leader.FULL_NAME,
                   C.SongLeader.SONG_ID.countDistinct(),
                   C.SongLeader.count(),
                   C.SongLeader.SINGING_ID.countDistinct())
            .from(C.Leader)
            .join(C.Leader, C.SongLeader)
            .whereEq(C.Leader._ID).toString();

    public static final String LEADER_SONG_LIST_QUERY =
        SQL.select(C.Song._ID)
                .select(C.Song.FULL_NAME)
                .select("'(' || " + C.LeaderStats.LEAD_COUNT + " || ')'")
            .from(C.LeaderStats)
            .join(C.LeaderStats, C.Song)
            .whereEq(C.LeaderStats.LEADER_ID)
            .order(C.LeaderStats.LEAD_COUNT, "DESC", C.Song.SORT, "ASC").toString();

    public static final String SONG_LIST_QUERY =
        SQL.select(C.Song._ID)
                .select(C.Song.NUMBER)
                .select(C.Song.TITLE)
                .select(C.Song.NUMBER + "*1").as(IndexedCursorAdapter.INDEX_COLUMN)
                .select("'(' || " + C.SongStats.LEAD_COUNT.func("SUM") + " || ')'")
            .from(C.Song)
            .left_outer_join(C.Song, C.SongStats)
            .group(C.Song._ID)
            .order(C.Song.SORT, "ASC").toString();

    public static final String SONG_ACTIVITY_QUERY =
        SQL.select(C.Song.FULL_NAME,
                   "'PLACEHOLDER'",
                   "'PLACEHOLDER'",
                   C.SongLeader.LEADER_ID.countDistinct(),
                   C.SongLeader.SINGING_ID.countDistinct())
            .from(C.Song)
            .join(C.Song, C.SongLeader)
            .whereEq(C.Song._ID).toString();

    public static final String SONG_LYRICS_QUERY =
        SQL.select(C.Song.LYRICS)
            .from(C.Song)
            .whereEq(C.Song._ID).toString();

    public static final String SONG_LEADER_LIST_QUERY =
        SQL.select(C.Leader._ID)
            .select(C.Leader.FULL_NAME)
            .select("'(' || " + C.LeaderStats.LEAD_COUNT + " || ')'")
        .from(C.LeaderStats)
        .join(C.LeaderStats, C.Leader)
        .whereEq(C.LeaderStats.SONG_ID)
        .order(C.LeaderStats.LEAD_COUNT, "DESC", C.Leader.SORT, "ASC").toString();

    public static final String SINGING_LIST_QUERY =
        SQL.select(C.Singing._ID)
                .select(C.Singing.NAME)
                .select(C.Singing.LOCATION)
                .select(C.Singing.YEAR).as(IndexedCursorAdapter.INDEX_COLUMN)
            .from(C.Singing).toString();

    public static final String SINGING_ACTIVITY_QUERY =
        SQL.select(C.Singing.NAME, C.Singing.LOCATION,
                   C.SongLeader.SONG_ID.countDistinct(), C.SongLeader.LEADER_ID.countDistinct())
            .from(C.Singing)
            .join(C.Singing, C.SongLeader)
            .whereEq(C.Singing._ID).toString();

    public static final String SINGING_LEADER_LIST_QUERY =
        SQL.select(C.Song._ID,
                   C.Song.FULL_NAME,
                   C.Leader.FULL_NAME.func("group_concat", "', '"))
            .from(C.SongLeader)
            .join(C.SongLeader, C.Song)
            .join(C.SongLeader, C.Leader)
            .whereEq(C.SongLeader.SINGING_ID)
            .group(C.SongLeader.SONG_ID)
            .order(C.SongLeader.SINGING_ORDER, "ASC").toString();

    public Cursor query(Object sql, String[] args) {
        return db.rawQuery(sql.toString(), args);
    }
    public Cursor query(Object sql) {
        return query(sql, new String[] {});
    }

    // Return a single long value given an sql string and positional arguments
    public static final long NOT_FOUND = -1;
    public long queryLong(String sql, String[] args) {
        Cursor cursor = query(sql, args);
        long result = NOT_FOUND;
        if (cursor.moveToFirst())
            result = cursor.getLong(0);
        cursor.close();
        return result;
    }

    public String queryString(String sql, String[] args) {
        Cursor cursor = query(sql, args);
        String result = null;
        if (cursor.moveToFirst())
            result = cursor.getString(0);
        cursor.close();
        return result;
    }

    // Shortcut methods
    public long getSongId(String number) {
        return queryLong(SONG_ID_QUERY, new String[] {number});
    }
/*
    public long getLeaderId(String firstName, String lastName) {
        return queryLong(LEADER_ID_QUERY, new String[] {firstName, lastName});
    }
*/
    // The OpenHelper class
    // ------------------------------------------------------------------------------------------
    private static class MinutesDbHelper extends SQLiteAssetHelper {
        public MinutesDbHelper(Context context) {
            super(context, C.DB_NAME, null, C.DB_VERSION);
            setForcedUpgrade();
        }
    }
/*
    // The OpenHelper class
    // ------------------------------------------------------------------------------------------
    private static class MinutesDbHelper extends SQLiteOpenHelper {
        private Context mContext;
        public MinutesDbHelper(Context context) {
            super(context, C.DB_NAME, null, C.DB_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(C.Song.sqlCreate());
            db.execSQL(C.Leader.sqlCreate());
            db.execSQL(C.Singing.sqlCreate());
            db.execSQL(C.SingingSong.sqlCreate());
            db.execSQL(C.LeaderSong.sqlCreate());
            insertData(db);
            SQLiteAssetHelper
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(C.Song.sqlDelete());
            db.execSQL(C.Leader.sqlDelete());
            db.execSQL(C.Singing.sqlDelete());
            db.execSQL(C.SingingSong.sqlDelete());
            db.execSQL(C.LeaderSong.sqlDelete());
            onCreate(db);
        }

        // Insert initial data
        private void insertData(final SQLiteDatabase db) {
            new Thread(new Runnable() {
                public void run() {
                    showToast("Rebuilding database...");
                    try {
                        loadLyrics(db);
                        showToast("Updated lyrics");
                        loadMinutes(db);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        // Show a toast from a thread
        private void showToast(final String msg) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void loadLyrics(SQLiteDatabase db) throws IOException {
            AssetManager assets = mContext.getAssets();
            for (String fileName : assets.list("lyrics")) {
                String filePath = new File("lyrics", fileName).getPath();
                BufferedReader f = new BufferedReader(new InputStreamReader(assets.open(filePath), "utf8"));
                try {
                    // Read each line into a ContentValues object
                    ContentValues values = new ContentValues();
                    String line;
                    StringBuilder lyrics = new StringBuilder(); // Lyrics are multiple
                    while ((line = f.readLine()) != null) {
                        String[] pair = TextUtils.split(line, "=");
                        String label = pair.length > 0 ? pair[0] : line;
                        String text = pair.length > 1 ? pair[1] : label;
                        // Find the column
                        SQL.Column column = null;
                        switch(label) {
                            case "page":
                                column = C.Song.NUMBER;
                                break;
                            case "title": column = C.Song.TITLE; break;
                            case "tune": column = C.Song.COMPOSER; break;
                            case "tune_year": column = C.Song.TUNE_YEAR; break;
                            case "words": column = C.Song.POET; break;
                            case "words_year": column = C.Song.WORDS_YEAR; break;
                            case "lyrics":
                            default:
                                // add to lyrics if no column, or if column is lyrics
                                lyrics.append(text.trim()).append("\n");
                        }
                        // Add to values
                        if (column != null)
                            values.put(column.name, text.trim());
                        // Add the sort value
                        if (label.equals("page")) {
                            int sort = 0;
                            if (text.endsWith("b")) { // Bottom
                                sort = 1 + 10 * Integer.parseInt(text.substring(0, text.length() - 1));
                            } else if (text.endsWith("t")) { // Top
                                sort = 10 * Integer.parseInt(text.substring(0, text.length() - 1));
                            } else
                            sort = 10 * Integer.parseInt(text);
                            values.put(C.Song.SORT.name, sort);
                        }
                    }
                    values.put(C.Song.LYRICS.name, lyrics.toString());
                    long id = db.insert(C.Song.TABLE_NAME, "null", values);
                } finally {
                    f.close();
                }
            }
        }

        private long insertSingingSong(SQLiteDatabase db, long singingId, int songIdx, String number) {
            // Get the song id
            long songId = -1;
            Cursor cursor = db.rawQuery(
                SQL.select(C.Song._ID).from(C.Song).whereEq(C.Song.NUMBER).toString(),
                new String[]{number}
            );
            if (cursor.moveToFirst())
                songId = cursor.getLong(0);
            else
                Log.v("getSongId", "Missing song: " + number);
            cursor.close();
            if (songId == -1)
                return -1;
            // Insert the SingingSong
            ContentValues values = new ContentValues();
            values.put(C.SingingSong.SINGING_ID.name, singingId);
            values.put(C.SingingSong.SONG_ID.name, songId);
            values.put(C.SingingSong.SINGING_ORDER.name, songIdx);
            return db.insert(C.SingingSong.TABLE_NAME, "null", values);
        }

        private long getOrInsertLeader(SQLiteDatabase db, String firstName, String lastName) {
            long leaderId = 0;
            Cursor cursor = db.rawQuery(
                SQL.select(C.Leader._ID).from(C.Leader).whereEq(C.Leader.FIRST_NAME, C.Leader.LAST_NAME).toString(),
                new String[]{firstName, lastName}
            );
            if (cursor.moveToFirst()) {
                leaderId = cursor.getLong(0);
            } else {
                ContentValues values = new ContentValues();
                values.put(C.Leader.FIRST_NAME.name, firstName);
                values.put(C.Leader.LAST_NAME.name, lastName);
                leaderId = db.insert(C.Leader.TABLE_NAME, "null", values);
            }
            cursor.close();
            return leaderId;
        }

        private void loadMinutes(SQLiteDatabase db) throws IOException {
            int totalSongs = 0;
            int totalSingings = 0;
            AssetManager assets = mContext.getAssets();
            for (String fileName : assets.list("minutes")) {
                String filePath = new File("minutes", fileName).getPath();
                BufferedReader f = new BufferedReader(new InputStreamReader(assets.open(filePath), "utf8"));
                try {
                    ContentValues values;
                    Cursor cursor;
                    // Add the singing
                    String singing = f.readLine();
                    String location = f.readLine();
                    String date = f.readLine();
                    if (singing == null || location == null || date == null)
                        throw new IOException();
                    values = new ContentValues();
                    values.put(C.Singing.NAME.name, singing);
                    values.put(C.Singing.LOCATION.name, location);
                    values.put(C.Singing.START_DATE.name, date);
                    long singingId = db.insert(C.Singing.TABLE_NAME, "null", values);
                    ++totalSingings;
                    if (totalSingings % 5 == 0)
                        showToast(String.format("Adding: %s (%d total)", singing, totalSingings));
                    // Add songs and leaders
                    String line;
                    int songIdx = 0;
                    while ((line = f.readLine()) != null) {
                        String[] data = TextUtils.split(line, ", ");
                        if (data.length < 3)
                            continue;
                        // Add the entry to the singing's minutes
                        long singingSongId = insertSingingSong(db, singingId, songIdx++, data[0]);
                        if (singingSongId == -1)
                            continue;
                        ++totalSongs;
                        // Add leaders to the song
                        for (int i = 2; i < data.length; i+=2) {
                            long leaderId = getOrInsertLeader(db, data[i-1], data[i]);
                            values = new ContentValues();
                            values.put(C.LeaderSong.LEADER_ID.name, leaderId);
                            values.put(C.LeaderSong.SINGING_SONG_ID.name, singingSongId);
                            db.insert(C.LeaderSong.TABLE_NAME, "null", values);
                        }
                    }
                } finally {
                    f.close();
                }
            }
            showToast(String.format("Done: %d singings, %d songs", totalSingings, totalSongs));
        }
    };
*/
}
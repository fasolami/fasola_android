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
    public Cursor query(Object sql, String... args) {
        return db.rawQuery(sql.toString(), args);
    }

    // Query shortcuts
    public static final long NOT_FOUND = -1;
    public long queryLong(String sql, String... args) {
        Cursor cursor = query(sql, args);
        long result = NOT_FOUND;
        if (cursor.moveToFirst())
            result = cursor.getLong(0);
        cursor.close();
        return result;
    }

    public String queryString(String sql, String... args) {
        Cursor cursor = query(sql, args);
        String result = null;
        if (cursor.moveToFirst())
            result = cursor.getString(0);
        cursor.close();
        return result;
    }

    public String[] queryStringArray(String sql, String... args) {
        Cursor cursor = query(sql, args);
        String[] result = new String[cursor.getCount()];
        while (cursor.moveToNext())
            result[cursor.getPosition()] = cursor.getString(0);
        cursor.close();
        return result;
    }

    // The OpenHelper class
    // ------------------------------------------------------------------------------------------
    private static class MinutesDbHelper extends SQLiteAssetHelper {
        public MinutesDbHelper(Context context) {
            super(context, C.DB_NAME, null, C.DB_VERSION);
            setForcedUpgrade();
        }
    }
}
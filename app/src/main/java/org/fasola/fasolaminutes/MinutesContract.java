package org.fasola.fasolaminutes;

import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * FaSoLa database
 */

public class MinutesContract {
    protected MinutesContract() {
    }

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "minutes.db";

    /* Song table */
    public static final class SongContract extends SQL.BaseTable {
        private SongContract() {
            super("songs");
            TITLE = column("Title", "TEXT");
            NUMBER = column("PageNum", "TEXT");
            SORT = _ID.fullName;
/*
            SORT = column("sort", "INTEGER");
            COMPOSER = column("composer", "TEXT");
            TUNE_YEAR = column("tune_year", "TEXT");
            POET = column("poet", "TEXT");
            WORDS_YEAR = column("words_year", "TEXT");
*/
            LYRICS = column("SongText", "TEXT");

            FULL_NAME = NUMBER + " || ' ' || " + TITLE;
        }
        public SQL.Column TITLE, NUMBER, COMPOSER, TUNE_YEAR, POET, WORDS_YEAR, LYRICS;
        public String FULL_NAME, SORT; // Query to return the full tune name with page number
    }
    public static SongContract Song = new SongContract();

    /* SongStats table */
    public static final class SongStatsContract extends SQL.BaseTable {
        private SongStatsContract() {
            super("song_stats");
            SONG_ID = joinColumn(Song, "song_id");
            YEAR = column("year", "TEXT");
            TIMES_LED = column("lead_count", "INTEGER");
            RANK = column("rank", "INTEGER");
        }
        public SQL.Column SONG_ID, YEAR, TIMES_LED, RANK;
    }
    public static SongStatsContract SongStats = new SongStatsContract();

    /* Leader table */
    public static final class LeaderContract extends SQL.BaseTable {
        private LeaderContract() {
            super("leaders");
/*
            // instr doesn't exist in the SQLite version android uses
            FIRST_NAME = "substr(name, 1, instr(name, ' '))";
            LAST_NAME = "substr(substr(name, instr(name, ' ') + 1), instr(substr(name, instr(name, ' ') + 1), ' ')+1)";
*/
            FULL_NAME = column("name", "TEXT");
            TIMES_LED = column("lead_count", "INTEGER");
            SORT = FULL_NAME.fullName;
        }
        public SQL.Column FULL_NAME, TIMES_LED;
        public String LAST_NAME, FIRST_NAME; // Query to return the full name
        public String SORT; // Query to use for ordering
    }
    public static LeaderContract Leader = new LeaderContract();


    /* LeaderStats table */
    public static final class LeaderStatsContract extends SQL.BaseTable {
        private LeaderStatsContract() {
            super("leader_song_stats");
            LEADER_ID = joinColumn(Leader, "leader_id");
            SONG_ID = joinColumn(Song, "song_id");
            TIMES_LED = column("lead_count", "INTEGER");
        }
        public SQL.Column SONG_ID, LEADER_ID, TIMES_LED;
    }
    public static LeaderStatsContract LeaderStats = new LeaderStatsContract();

    /* Singing table */
    public static final class SingingContract extends SQL.BaseTable {
        private SingingContract() {
            super("minutes");
            NAME = column("Name", "TEXT");
            START_DATE = column("Date", "TEXT");
            LOCATION = column("Location", "TEXT");
            FULL_TEXT = column("Minutes", "TEXT");
            YEAR = column("Year", "TEXT");
        }
        public SQL.Column NAME, START_DATE, LOCATION, FULL_TEXT, YEAR;
    }
    public static SingingContract Singing = new SingingContract();

    /* Song-Singing-Leader join table table */
    public static final class SongLeaderContract extends SQL.BaseTable {
        private SongLeaderContract() {
            super("song_leader_joins");
            SONG_ID = joinColumn(Song, "song_id");
            SINGING_ID = joinColumn(Singing, "minutes_id");
            LEADER_ID = joinColumn(Leader, "leader_id");
            SINGING_ORDER = _ID.fullName;
        }
        public SQL.Column SONG_ID, SINGING_ID, LEADER_ID;
        public String SINGING_ORDER;
    }
    public static SongLeaderContract SongLeader = new SongLeaderContract();
}



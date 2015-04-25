package org.fasola.fasolaminutes;

import android.database.sqlite.SQLiteDatabase;

/**
 * FaSoLa database
 */

public class MinutesContract {
    protected MinutesContract() {
    }

    public static final int DB_VERSION = 2;
    public static final String DB_NAME = "minutes.db";

    // Contract classes (see below for definitions
    public static SongDAO Song = new SongDAO();
    public static SongStatsDAO SongStats = new SongStatsDAO();
    public static LeaderDAO Leader = new LeaderDAO();
    public static LeaderAliasDAO LeaderAlias = new LeaderAliasDAO();
    public static LeaderStatsDAO LeaderStats = new LeaderStatsDAO();
    public static SingingDAO Singing = new SingingDAO();
    public static SongLeaderDAO SongLeader = new SongLeaderDAO();

    // Initialize joins and calculated columns
    static {
        SQL.BaseTable.join(Song.id, SongStats.songId);
        SQL.BaseTable.join(Song.id, LeaderStats.songId);
        SQL.BaseTable.join(Song.id, SongLeader.songId);
        SQL.BaseTable.join(Leader.id, LeaderStats.leaderId);
        SQL.BaseTable.join(Leader.id, SongLeader.leaderId);
        SQL.BaseTable.join(Singing.id, SongLeader.singingId);
        SQL.BaseTable.leftJoin(Leader, LeaderAlias,
            Leader.id + " = " + LeaderAlias.leaderId + " AND " +
            LeaderAlias.type + " = 'Alternate Spelling'"
        );
        Song.onCreate();
        SongStats.onCreate();
        Leader.onCreate();
        LeaderAlias.onCreate();
        LeaderStats.onCreate();
        SongLeader.onCreate();
        Singing.onCreate();
    }

    // Use as a base table class to provide a database for SQL.BaseTable
    private static class MinutesBaseTable extends SQL.BaseTable {
        protected MinutesBaseTable(String tableName) {
            super(tableName);
        }

        @Override
        protected SQLiteDatabase getDb() {
            return MinutesDb.getInstance().getDb();
        }
    }

    /* Song table */
    public static final class SongDAO extends MinutesBaseTable {
        protected SongDAO() {
            super("songs");
            title = column("Title");
            number = column("PageNum");
            lyrics = column("SongText");
        }

        @Override
        protected void onCreate() {
            pageSort = column(number.format("{column} * 1"));
            fullName = concat(number, "' '", title);
            leaderCount = column(LeaderStats.leaderId.countDistinct());
            leadCount = column(LeaderStats.leadCount.sum());
        }

        public SQL.Column title, number, COMPOSER, TUNE_YEAR, POET, WORDS_YEAR, lyrics;
        public SQL.Column fullName, leaderCount, leadCount, pageSort;
    }

    /* SongStats table */
    public static final class SongStatsDAO extends MinutesBaseTable {
        protected SongStatsDAO() {
            super("song_stats");
            year = column("year");
            leadCount = column("lead_count");
            rank = column("rank");
            songId = column("song_id");
        }

        public SQL.Column songId, year, leadCount, rank;
    }

    /* Leader table */
    public static final class LeaderDAO extends MinutesBaseTable {
        protected LeaderDAO() {
            super("leaders");
            fullName = column("name");
            lastName = column("last_name"); // NB must use db altered from iPhone version
            leadCount = column("lead_count");
            // Raw entropy
            entropy = column("song_entropy");
            // Rounded entropy
            entropyDisplay = column(entropy.format("ROUND({column}, 4)"));
        }

        @Override
        protected void onCreate() {
            songCount = column(SongLeader.songId.countDistinct());
            singingCount = column(SongLeader.singingId.countDistinct());
            aka = column(LeaderAlias.alias.func("group_concat", true));
        }

        public SQL.Column fullName, lastName, leadCount, entropy, entropyDisplay, singingCount, songCount, aka;
    }

    /* LeaderNameAliases table */
    public static final class LeaderAliasDAO extends MinutesBaseTable {
        protected LeaderAliasDAO() {
            super("leader_name_aliases");
            leaderId = column("leader_id");
            name = column("name");
            alias = column("alias");
            type = column("type");
        }

        public SQL.Column leaderId, name, alias, type;
    }

    /* LeaderStats table */
    public static final class LeaderStatsDAO extends MinutesBaseTable {
        protected LeaderStatsDAO() {
            super("leader_song_stats");
            leadCount = column("lead_count");
            leaderId = column("leader_id");
            songId = column("song_id");
        }

        public SQL.Column songId, leaderId, leadCount;
    }

    /* Singing table */
    public static final class SingingDAO extends MinutesBaseTable {
        protected SingingDAO() {
            super("minutes");
            name = column("Name");
            startDate = column("Date");
            location = column("Location");
            fullText = column("Minutes");
            year = column("Year");
        }

        @Override
        protected void onCreate() {
            songCount = column(SongLeader.songId.countDistinct());
            leaderCount = column(SongLeader.leaderId.countDistinct());
        }

        public SQL.Column name, startDate, location, fullText, year, songCount, leaderCount;
    }

    /* Song-Singing-Leader join table table */
    public static final class SongLeaderDAO extends MinutesBaseTable {
        protected SongLeaderDAO() {
            super("song_leader_joins");
            singingOrder = id.toString();
            songId = column("song_id");
            singingId = column("minutes_id");
            leaderId = column("leader_id");
        }

        public SQL.Column songId, singingId, leaderId;
        public String singingOrder;
    }
}



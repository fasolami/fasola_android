package org.fasola.fasolaminutes;

import android.database.sqlite.SQLiteDatabase;

/**
 * FaSoLa database
 */

public class MinutesContract {
    protected MinutesContract() {
    }

    public static final int DB_VERSION = 10;
    public static final String DB_NAME = "minutes.db";

    // Contract classes (see below for definitions
    public static SongDAO Song = new SongDAO();
    public static SongStatsDAO SongStats = new SongStatsDAO();
    public static LeaderDAO Leader = new LeaderDAO();
    public static LeaderAliasDAO LeaderAlias = new LeaderAliasDAO();
    public static LeaderStatsDAO LeaderStats = new LeaderStatsDAO();
    public static SingingDAO Singing = new SingingDAO();
    public static SongLeaderDAO SongLeader = new SongLeaderDAO();
    public static SongNeighborDAO SongNeighbor = new SongNeighborDAO();

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
            titleOrdinal = column("TitleOrdinal");
            number = column("PageNum");
            lyrics = column("SongText");
            composer = column("composer");
            poet = column("poet");
            time = column("Times");
            meter = column("MeterName");
            // Format keys (which are in the db as "D" or "A min")
            // 1. Some keys have extraneous spaces at the end, hence trim()
            // 2. Minor keys are written "min"; major keys are blank; key changes list "min" first
            //    The replace function catches all occurrences of "min", while a check that a key
            //    does not end in "min" catches major keys.
            rawKey = column("Keys");
            key = column(rawKey.format(
                    "replace(" +
                        "CASE WHEN substr({column}, length({column})-2) <> 'min' " +
                            "THEN trim({column}) || ' Major' " +
                            "ELSE trim({column}) " +
                        "END, " +
                        "'min', 'Minor'" +
                    ")"
            ));
        }

        @Override
        protected void onCreate() {
            pageSort = column(number.format("{column} * 1"));
            leaderCount = subQuery(LeaderStats.leaderId.countDistinct());
            leadCount = column(SongStats.leadCount.sum());
            coleadCount = subQuery(
                "SELECT COUNT(*) FROM (" +
                    SQL.select("1")
                       .from(SongLeader)
                       .group(SongLeader.leadId)
                       .where(SongLeader.songId, "=", Song.id)
                       .having(SongLeader.leaderId.count(), ">" , 1) +
                ")");
            fullName = concat(number, "' '", title, titleOrdinal.format(
                "(CASE WHEN {column} <> '' " +
                    "THEN ' (' || {column} || ')' " +
                    "ELSE '' " +
                "END)"
            ));
        }

        public SQL.Column title, titleOrdinal, number, composer, poet, lyrics, rawKey, key, time, meter;
        public SQL.Column fullName, leaderCount, leadCount, coleadCount, pageSort;
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
            entropyDisplay = column(entropy.format("SUBSTR(ROUND({column}, 4) || '000', 0, 7)"));
        }

        @Override
        protected void onCreate() {
            songCount = column(SongLeader.songId.countDistinct());
            singingCount = column(SongLeader.singingId.countDistinct());
            aka = subQuery(LeaderAlias.alias.func("group_concat", true));
            majorPercent = column(
                Song.rawKey.format(
                    "CASE " +
                        // Key changes = 0.5
                        "WHEN {} LIKE ',' THEN 0.5 " +
                        // Minor = 0
                        "WHEN {} LIKE '%%min' THEN 0 " +
                        // Major = 1
                        "ELSE 1.0 " +
                    "END")
                // Turn into percent of total leads
                .format("SUM({}) / %s", leadCount)
            );
        }

        public SQL.Column fullName, lastName, leadCount, entropy, entropyDisplay,
                          singingCount, songCount, aka, majorPercent;
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
            recordingCount = column("RecordingCt");
            year = column("Year");
        }

        @Override
        protected void onCreate() {
            songCount = column(SongLeader.leadId.countDistinct());
            leaderCount = column(SongLeader.leaderId.countDistinct());
        }

        public SQL.Column name, startDate, location, fullText, year, recordingCount, songCount, leaderCount;
    }

    /* Song-Singing-Leader join table */
    public static final class SongLeaderDAO extends MinutesBaseTable {
        protected SongLeaderDAO() {
            super("song_leader_joins");
            songId = column("song_id");
            singingId = column("minutes_id");
            leaderId = column("leader_id");
            leadId = column("lead_id");
            singingOrder = leadId.toString();
            audioUrl = column("audio_url");
        }

        public SQL.Column songId, singingId, leaderId, leadId, audioUrl;
        public String singingOrder;
    }

    /* Song Neighbor join table */
    public static final class SongNeighborDAO extends MinutesBaseTable {
        protected SongNeighborDAO() {
            super("song_neighbors");
            fromId = column("from_song_id");
            toId = column("to_song_id");
            rank = column("rank");
        }

        public SQL.Column fromId, toId, rank;
    }
}



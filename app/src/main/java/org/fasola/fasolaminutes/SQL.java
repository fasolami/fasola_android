package org.fasola.fasolaminutes;

import android.text.TextUtils;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * A namespace class with SQL helpers
 */

public class SQL {
    /**
     * Base class for SQL table contracts
     * Provides automatic TABLE_NAME and _ID fields
     * Easy JOINs using the Select class
     */
    public static class BaseTable {
        public String TABLE_NAME;
        public Column _ID;
        protected StringBuilder SQL_COLUMNS;

        public static Map<Pair<BaseTable, BaseTable>, Pair<Column, Column>> joinMap =
                new HashMap<>();

        protected BaseTable(String tableName) {
            TABLE_NAME = tableName;
            _ID = new Column(TABLE_NAME, "id");
            SQL_COLUMNS = new StringBuilder("CREATE TABLE ").append(TABLE_NAME).append(" (")
                    .append("_id INTEGER PRIMARY KEY");
        }

        // Create a column with type
        public Column column(String name, String type) {
            SQL_COLUMNS.append(", ").append(name).append(" ").append(type);
            return new Column(TABLE_NAME, name);
        }

        // Create a column named [other table]_id to be used as a join column
        public Column joinColumn(BaseTable joinTable) {
            return joinColumn(joinTable, joinTable.TABLE_NAME + "_id");
        }

        public Column joinColumn(BaseTable joinTable, String joinColumn) {
            Column column = column(joinColumn, "INTEGER");
            join(this, column, joinTable, joinTable._ID);
            return column;
        }

        // Add an entry to the joinMap, joining two tables by their columns
        // Called from joinColumn,
        public static void join(BaseTable t1, Column col1, BaseTable t2, Column col2) {
            joinMap.put(Pair.create(t1, t2), Pair.create(col1, col2));
            joinMap.put(Pair.create(t2, t1), Pair.create(col2, col1));
        }

        // Create and delete statements
        public String sqlCreate() {
            return SQL_COLUMNS.toString() + ")";
        }

        public String sqlDelete() {
            return "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
        }

        // Default count for the table is on _id
        public String count() {
            return count(false);
        }

        public String countDistinct() {
            return count(true);
        }

        public String count(boolean distinct) {
            return _ID.count(distinct);
        }

        // Default string
        public String toString() {
            return TABLE_NAME;
        }
    }

    /**
     * Single Column in a BaseTable
     */
    public static class Column {
        public String name;
        public String fullName;
        public Column(String tableName, String columnName) {
            name = columnName;
            fullName = tableName + "." + columnName;
        }

        public String toString() {
            return fullName;
        }

        // Count functions
        public String count() {
            return count(false);
        }

        public String countDistinct() {
            return count(true);
        }

        public String count(boolean distinct) {
            return func("COUNT", distinct);
        }

        // Generic functions
        public String func(String funcName) {
            return func(funcName, false);
        }

        public String func(String funcName, boolean distinct) {
            return funcName + "(" + (distinct ? "DISTINCT " : "") + fullName + ")";
        }

        // Extra argument aggregates cannot use DISTINCT
        public String func(String funcName, String... args) {
            return funcName + "(" + fullName + ", " + TextUtils.join(", ", args) + ")";
        }
    }

    /**
     *  Easy SQL queries using Column and BaseTable classes
     */
    public static Query select(Object... args) {
        return new Query("SELECT", args);
    }

    public static class Query {
        protected StringBuilder q;
        int lastAlias = -1; // Start of the last auto-generated "AS"
        int columnIdx = 0; // Increment with each column to use as an auto alias

        protected Query(String queryType, Object... args) {
            q = new StringBuilder(queryType + " ");
            doSelect(args);
        }

        // Adds an automatic alias for every column ("column[0-n]")
        // Tracks the last added alias in case as() is called
        private Query doSelect(Object... cols) {
            String delim = "";
            for (Object col : cols) {
                append(delim).append(col.toString());
                // Save position in case of manual alias
                lastAlias = q.length();
                 // Auto alias
                if (col.toString().endsWith("._id") || col.toString().endsWith(".id"))
                    append(" AS _id"); // Preserve _id column for CursorAdapters
                else
                    append(" AS column").append(columnIdx++); // Auto alias
                delim = ", ";
            }
            return this;
        }

        // Basic queries
        public Query select(Object... cols) {
            return append(", ").doSelect(cols);
        }

        public Query as(String alias) {
            // Remove the previous alias
            if (lastAlias > -1)
                q.delete(lastAlias, q.length());
            lastAlias = -1;
            // Add the new alias
            return append(" AS ").append(alias);
        }

        public Query from(BaseTable table) {
            lastAlias = -1; // Prevent accidental destruction of this query (see as() above)
            return append(" FROM ").append(table.TABLE_NAME);
        }

        public Query from(BaseTable table, String alias) {
            return from(table).append(" ").append(alias);
        }

        public Query where(Object... args) {
            append(" WHERE ").append(args);
            if (args.length < 2)
                append(" = ?");
            else if (args.length < 3)
                append(" ?");
            return this;
        }

        public Query and(Object... args) {
            append(" AND ").append(args);
            if (args.length < 2)
                append(" = ?");
            else if (args.length < 3)
                append(" ?");
            return this;
        }

        public Query or(Object... args) {
            append(" OR ").append(args);
            if (args.length < 2)
                append(" = ?");
            else if (args.length < 3)
                append(" ?");
            return this;
        }
        // Better semantics for WHERE column = ? AND column = ? ...
        public Query whereEq(Object firstColumn, Object... columns) {
            where(firstColumn, "=", "?");
            for (Object columnName : columns)
                and(columnName, "=", "?");
            return this;
        }


        // Joins -- see BaseTable for the joinMap data structure
        public Query join(BaseTable t1, BaseTable t2) {
            Pair<Column, Column> joinColumns = BaseTable.joinMap.get(Pair.create(t1, t2));
            return join(t2, joinColumns.first, joinColumns.second);
        }

        public Query join(BaseTable table, Column on1, Column on2) {
            return append(" JOIN ").append(table.TABLE_NAME)
                    .append(" ON ").append(on1).append(" = ").append(on2);
        }

        public Query left_outer_join(BaseTable table, Column on1, Column on2) {
            return append(" LEFT OUTER").join(table, on1, on2);
        }

        public Query left_outer_join(BaseTable t1, BaseTable t2) {
            return append(" LEFT OUTER").join(t1, t2);
        }

        // Group, order, having
        public Query group(Object... cols) {
            return append(" GROUP BY ").append(", ", cols);
        }

        public Query having(Object... args) {
            return append(" HAVING ").append(args);
        }

        protected StringBuilder qOrder; // ORDER BY is kept separate
        // NB: Each call will replace the previous ORDER BY clause
        public Query order(Object... args) {
            qOrder = new StringBuilder("");
            for (int i = 0; i < args.length; i+=2) {
                if (i != 0)
                    qOrder.append(", ");
                // Column
                qOrder.append(args[i].toString()).append(" ");
                // ASC/DESC.  Assume ASC if there are an odd number of args
                if (args.length > i+1)
                    qOrder.append(args[i+1].toString());
                else
                    qOrder.append("ASC");
            }
            return this;
        }

        // append overloads
        public Query append(Object str) {
            q.append(str.toString());
            return this;
        }

        public Query append(Object[] args) {
            return append(" ", args);
        }

        public Query append(String delim, Object[] args) {
            append(args[0]);
            for (int i = 1; i < args.length; i++)
                append(delim).append(args[i]);
            return this;
        }

        // Data access
        public String toString() {
            if (qOrder != null)
                return q.toString() + " ORDER BY " + qOrder.toString();
            else
                return q.toString();
        }
    }
}
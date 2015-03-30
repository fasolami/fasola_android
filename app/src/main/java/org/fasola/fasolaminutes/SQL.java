package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A namespace class with SQL helpers
 */

public class SQL {
    public static final String INDEX_COLUMN = "sql_section_index";

    public static class JoinException extends RuntimeException {
        public JoinException() {
            super();
        }

        public JoinException(BaseTable table1, BaseTable table2) {
            super("No join path found between " + table1 + " and " + table2);
        }
    }

    private static class JoinEntry {
        JoinEntry(String text, boolean isLeft) {
            this.text = text;
            this.isLeft = isLeft;
        }
        String text;
        boolean isLeft;
    }

    private static class JoinMap extends HashMap<String, Map<String, JoinEntry>> {
        public JoinEntry put(Object t1, Object t2, String text, boolean isLeft) {
            String key1 = t1.toString();
            String key2 = t2.toString();
            Map<String, JoinEntry> map = get(key1);
            if (map == null) {
                map = new HashMap<>();
                super.put(key1, map);
            }
            return map.put(key2, new JoinEntry(text, isLeft));
        }
    }

    /**
     * Base class for SQL table contracts
     * Provides automatic TABLE_NAME and id fields
     * Easy JOINs using the Query class
     */
    public static class BaseTable {
        public String TABLE_NAME;
        public Column id;
        protected Map<String, Column> _columns;
        protected static JoinMap joinMap = new JoinMap();

        protected BaseTable(String tableName) {
            TABLE_NAME = tableName;
            _columns = new HashMap<>();
            id = column("id");
        }

        // Override this to initialize calculated columns
        // Essentially a two-part constructor, this is required to reliably use columns
        // from other tables in calculations
        protected void onCreate() {
        }

        // Create a column
        public Column column(String name) {
            return column(new Column(this, name));
        }

        // Add an existing column
        public Column column(Column col) {
            _columns.put(col.name, col);
            return col;
        }

        // Create a stored query as a column
        public Column queryColumn(Object... args) {
            return column(new QueryColumn(args));
        }

        public Column concat(Object... args) {
            // Insert concat operators (" || ") between each argument
            Object[] concatArgs = new Object[args.length + args.length - 1];
            for (int i = 0; i < args.length; i++) {
                concatArgs[i*2] = args[i];
                if (i != args.length-1)
                    concatArgs[i*2+1] = " || ";
            }
            return column(new QueryColumn(concatArgs));
        }

        // Add an entry to the joinMap, joining two tables by their columns
        public static void join(Column col1, Column col2) {
            join(col1.table, col2.table, col1 + " = " + col2, false);
        }

        public static void leftJoin(Column col1, Column col2) {
            join(col1.table, col2.table, col1 + " = " + col2, true);
        }

        public static void join(BaseTable t1, BaseTable t2, String text) {
            join(t1, t2, text, false);
        }

        public static void leftJoin(BaseTable t1, BaseTable t2, String text) {
            join(t1, t2, text, true);
        }

        public static void join(BaseTable t1, BaseTable t2, String text, boolean isLeft) {
            joinMap.put(t1, t2, text, isLeft);
            joinMap.put(t2, t1, text, isLeft);
        }

        // Get the JoinEntry used to join two tables
        protected static JoinEntry getJoin(Object t1, Object t2) {
            Map<String, JoinEntry> map = joinMap.get(t1.toString());
            if (map == null)
                return null;
            return map.get(t2.toString());
        }

        // Default count for the table is on _id
        public Column count() {
            return count(false);
        }

        public Column countDistinct() {
            return count(true);
        }

        public Column count(boolean distinct) {
            return id.count(distinct);
        }

        // Default string
        public String toString() {
            return TABLE_NAME;
        }

        // Query wrappers
        //---------------

        protected SQLiteDatabase getDb() {
            // Override to provide an SQLiteDatabase for queries
            return null;
        }

        protected void doQuery(Column field, Object value) {
            // Make a column list
            Column[] cols = new Column[_columns.size()];
            int idx = 0;
            for (Column col : _columns.values())
                cols[idx++] = col;
            // Make the query
            Query query = select();
            for (Column col : cols)
                query.select(col);
            query.from(this).whereEq(field).group(this.id);
            // Query
            Cursor cursor = getDb().rawQuery(query.toString(), new String[] {value.toString()});
            // Fill column values (already in order)
            if (cursor.moveToFirst())
                for (int i = 0; i < cols.length; i++)
                    _columns.get(cols[i].name).value = cursor.getString(i);
            cursor.close();
        }

        // Return a new BaseTable object that functions as a DAO
        protected <T extends BaseTable> T get(long id) {
            return get(this.id, id);
        }

        @SuppressWarnings("unchecked")
        protected <T extends BaseTable> T get(Column field, Object value) {
            try {
                T obj = (T) (this.getClass().newInstance());
                obj.onCreate(); // Setup secondary fields
                obj.doQuery(field, value);
                return obj;
            } catch(InstantiationException|IllegalAccessException e) {
                return null;
            }
        }

        // Return a Query object for this table
        public Query select(Object... args) {
            return SQL.select(args).from(this);
        }

        // Return a Query object for a CursorAdapter (i.e. it already has the ID column)
        public Query selectList(Object... args) {
            return SQL.select(id).select(args).group(id).from(this);
        }
    }

    /**
     * Single Column in a BaseTable
     * See QueryColumn for more complex queries
     */
    public static class Column {
        public String name;
        public BaseTable table;

        protected Column() {
            name = "";
        }

        public Column(BaseTable table, String columnName) {
            this.table = table;
            name = table.toString() + "." + columnName;
        }

        public String toString() {
            return name;
        }

        // Column formatting: Use {column} for the column
        public Column format(String fmt, Object... args) {
            return QueryColumn.fromFormat(fmt, this, args);
        }

        // Aggregate functions
        public Column count() {
            return count(false);
        }

        public Column countDistinct() {
            return count(true);
        }

        public Column count(boolean distinct) {
            return func("COUNT", distinct);
        }

        public Column sum() {
            return func("SUM");
        }

        // Generic functions
        public Column func(String funcName) {
            return func(funcName, false);
        }

        public Column func(String funcName, boolean distinct) {
            return format("%s(%s{column})", funcName, distinct ? "DISTINCT " : "");
        }

        // Extra argument aggregates, cannot use DISTINCT
        public Column func(String funcName, String... args) {
            return format("%s({column}, %s)", funcName, TextUtils.join(", ", args));
        }

        // Add the required joins to a query
        public void addJoin(Query query, BaseTable fromTable) {
            if (fromTable.toString().equals(table.toString()))
                return;
            query.join(fromTable, table);
        }

        // For use as a DAO
        protected String value;
        public String getString() {
            return value;
        }

        public int getInt() {
            return Integer.parseInt(value);
        }

        public boolean isNull() {
            return value == null;
        }
    }

    /**
     * A query that includes SQL operators/aggregate functions/etc
     * Can contain multiple columns
     */
    public static class QueryColumn extends Column {
        // Can have multiple columns from multiple tables
        protected List<BaseTable> tables;

        public QueryColumn(Object... args) {
            super();
            tables = new ArrayList<>();
            // Make the query
            StringBuilder q = new StringBuilder();
            for (Object arg : args)
                q.append(arg.toString());
            name = q.toString();
            addTables(args);
        }

        protected static Column fromFormat(String fmt, Column col, Object... args) {
            QueryColumn q = new QueryColumn(col);
            q.addTables(args);
            // fmt should use {column} as the column placeholder
            q.name = String.format(fmt.replace("{column}", col.toString()), args);
            return q;
        }

        // Add any required tables to the table list
        protected void addTables(Object... cols) {
            for (Object arg : cols) {
                if (arg instanceof Column)
                    if (arg instanceof QueryColumn)
                        tables.addAll(((QueryColumn) arg).tables);
                    else
                        tables.add(((Column) arg).table);
            }
        }

        // Override to add all joins in the tables list
        @Override
        public void addJoin(Query query, BaseTable fromTable) {
            for (BaseTable otherTable : tables)
                if (! fromTable.toString().equals(otherTable.toString()))
                    query.join(fromTable, otherTable);
        }
    }


    /**
     *  Homemade query builder
     */
    public static Query select(Object... args) {
        return new Query("SELECT", args);
    }

    // Different semantics for UNION
    public static Query union(Query q, Query... queries) {
        return q.union(queries);
    }

    public static class Query {
        // Query elements
        protected String queryType;
        protected boolean isDistinct;
        protected List<Pair<Object, String>> selectColumns = new ArrayList<>(); // Column, alias
        protected List<Column> joinColumns = new ArrayList<>();
        protected BaseTable fromTable;
        protected Map<String, String> joins = new LinkedHashMap<>(); // table name, join statment
        protected QueryStringBuilder strGroup = new QueryStringBuilder(" GROUP BY");
        protected QueryStringBuilder strHaving = new QueryStringBuilder(" HAVING");
        protected List<QueryStringBuilder> whereList = new ArrayList<>();
        protected QueryStringBuilder strOrder = new QueryStringBuilder(" ORDER BY");
        protected Object limit;
        protected Object offset;

        protected List<Query> union;
        protected Query mParentQuery; // The parent of the query in the filter chain

        protected Query(String type, Object... args) {
            queryType = type;
            select(args);
        }

        protected Query(Query other) {
            queryType = other.queryType;
            isDistinct = other.isDistinct;
            selectColumns = new ArrayList<>(other.selectColumns);
            joinColumns = new ArrayList<>(other.joinColumns);
            fromTable = other.fromTable;
            joins = new LinkedHashMap<>(other.joins);
            strGroup = new QueryStringBuilder(other.strGroup);
            strHaving = new QueryStringBuilder(other.strHaving);
            whereList = new ArrayList<>();
            for (QueryStringBuilder q : other.whereList)
                whereList.add(new QueryStringBuilder(q));
            strOrder = new QueryStringBuilder(other.strOrder);
            limit = other.limit;
            offset = other.offset;
            union = other.union; // Shouldn't need to change the other queries in the union
            mParentQuery = other;
        }

        // SELECT
        // ------

        // Adds an automatic alias for every column ("column[0-n]")
        public Query select(Object... cols) {
            for (Object col : cols) {
                String alias = "column" + selectColumns.size();
                // Preserve _id column for CursorAdapters
                if (col.toString().endsWith("._id") || col.toString().endsWith(".id"))
                    alias = "_id";
                Pair<Object, String> pair = Pair.create(col, alias);
                selectColumns.add(pair);
                addJoinColumn(col);
            }
            return this;
        }

        public Query distinct(boolean distinct) {
            isDistinct = distinct;
            return this;
        }

        public Query distinct() {
            return distinct(true);
        }

        public Query as(String alias) {
            // Update the last alias
            int idx = selectColumns.size() - 1;
            Pair<Object, String> pair = selectColumns.get(idx);
            selectColumns.set(idx, Pair.create(pair.first, alias));
            return this;
        }

        // Select a column to use as a section index
        public Query sectionIndex(Object col) {
            return select(col).as(INDEX_COLUMN);
        }

        // Select a section index column and sort by this column
        public Query sectionIndex(Object col, String ascDesc) {
            return sectionIndex(col).order(col, ascDesc);
        }

        // FROM
        // ----
        public Query from(BaseTable table) {
            fromTable = table;
            return this;
        }

        // JOIN
        // ----

        // Join using defined relationships
        public Query join(BaseTable t1, BaseTable t2) {
            return _join(t1, t2, false);
        }

        public Query leftJoin(BaseTable t1, BaseTable t2) {
            return _join(t1, t2, true);
        }

        // Join on specified columns
        public Query join(Object table, Column on1, Column on2) {
            return _join(table, on1 + " = " + on2, false);
        }

        public Query leftJoin(Object table, Column on1, Column on2) {
            return _join(table, on1 + " = " + on2, true);
        }

        // Search for a columns to complete this join
        protected Query _join(BaseTable t1, BaseTable t2, boolean isLeft) {
            if (joins.containsKey(t2.toString()))
                return this;
            JoinEntry entry = BaseTable.getJoin(t1, t2);
            if (entry != null)
                return _join(t2, entry.text, entry.isLeft || isLeft);
            // Look for a many to many join path if we don't have a simple join
            Map<String, JoinEntry> map = BaseTable.joinMap.get(t1.TABLE_NAME);
            if (map == null)
                throw new JoinException(t1, t2);
            for (String other : map.keySet()) {
                entry = BaseTable.getJoin(other, t2);
                if (entry != null) {
                    // Join to the intermediate table
                    JoinEntry firstJoin = BaseTable.getJoin(t1, other);
                    _join(other, firstJoin.text, firstJoin.isLeft || isLeft);
                    // Join to the second table
                    _join(t2, entry.text, entry.isLeft || isLeft);
                    return this;
                }
            }
            throw new JoinException(t1, t2);
        }

        protected Query _join(Object table, String joinOn, boolean isLeft) {
            if (joins.containsKey(table.toString()))
                return this;
            joins.put(table.toString(),
                (isLeft ? " LEFT JOIN " : " JOIN ") + table + " ON " + joinOn);
            return this;
        }

        protected void addJoinColumn(Object col) {
            if (col instanceof Column)
                joinColumns.add((Column) col);
        }

        // WHERE
        // -----
        public Query where(Object col, Object oper, Object val) {
            // Don't use AND or OR before WHERE so there will be a valid QueryStringBuilder
            // Don't add another QueryStringBuilder if the last one is empty
            if (whereList.isEmpty() || ! whereList.get(whereList.size() - 1).isEmpty())
                whereList.add(new QueryStringBuilder());
            return _addWhere(col, oper, val);
        }

        public Query and(Object col, Object oper, Object val) {
            return _addWhere(" AND", col, oper, val);
        }

        public Query or(Object col, Object oper, Object val) {
            return _addWhere(" OR", col, oper, val);
        }

        // Better semantics for WHERE column = ? AND column = ? ...
        public Query whereEq(Object firstColumn, Object... columns) {
            where(firstColumn, "=", "?");
            for (Object columnName : columns)
                and(columnName, "=", "?");
            return this;
        }

        protected Query _addWhere(Object bool, Object col, Object oper, Object val) {
            QueryStringBuilder q = whereList.get(whereList.size() - 1);
            if (oper == null)
                oper = "=";
            if (val == null)
                val = "?";
            else if (val != "?")
                val = DatabaseUtils.sqlEscapeString(val.toString());
            q.append(bool, " ", col, oper, val);
            // Add join
            addJoinColumn(col);
            return this;
        }

        protected Query _addWhere(Object col, Object oper, Object val) {
            return _addWhere("", col, oper, val);
        }

        // Where can be used as a filter
        public Query pushWhere() {
            whereList.add(new QueryStringBuilder());
            return this;
        }

        public Query pushWhere(Object col, Object oper, Object val) {
            pushWhere();
            return where(col, oper, val);
        }

        public Query popWhere() {
            whereList.remove(whereList.size() - 1);
            return this;
        }

        // GROUP BY, HAVING, ORDER BY, LIMIT
        // ---------------------------------
        public Query group(Object... cols) {
            if (! strGroup.isEmpty())
                strGroup.append(",");
            strGroup.append(" ").appendDelim(", ", cols);
            addJoinColumn(cols[0]);
            return this;
        }

        public Query having(Object... args) {
            strHaving.append(" ").appendDelim(" ", args);
            addJoinColumn(args[0]);
            return this;
        }

        public Query orderAsc(Object arg) {
            return order(arg, "ASC");
        }

        public Query orderDesc(Object arg) {
            return order(arg, "DESC");
        }

        public Query order(Object... args) {
            for (int i = 0; i < args.length; i+=2) {
                if (! strOrder.isEmpty())
                    strOrder.append(", ");
                else
                    strOrder.append(" ");
                // Column
                strOrder.append(args[i]).append(" ");
                addJoinColumn(args[i]);
                // ASC/DESC.  Assume ASC if there are an odd number of args
                if (args.length > i+1)
                    strOrder.append(args[i+1]);
                else
                    strOrder.append("ASC");
            }
            return this;
        }

        public Query limit(Object limit) {
            this.limit = limit;
            return this;
        }

        public Query limit(Object offset, Object limit) {
            this.offset = offset;
            this.limit = limit;
            return this;
        }

        public Query offset(Object offset) {
            this.offset = offset;
            return this;
        }

        // Union
        public Query union(Query... other) {
            if (union == null)
                union = new ArrayList<>();
            union.addAll(Arrays.asList(other));
            return this;
        }

        // Filter: create a copy of the query to use as a filter
        public Query pushFilter() {
            return new Query(this);
        }

        public Query popFilter() {
            if (mParentQuery == null)
                return this;
            return mParentQuery;
        }

        // Assemble the query
        public String toString() {
            QueryStringBuilder q = new QueryStringBuilder("SELECT");
            if (isDistinct)
                q.append(" DISTINCT");
            // Columns
            {
                String delim = " ";
                for (Pair<Object, String> col : selectColumns) {
                    q.append(delim).append(col.first.toString()).append(" AS ").append(col.second);
                    delim = ", ";
                }
            }
            // From
            q.append(" FROM ").append(fromTable);
            // Check columns for joins
            for (Column col : joinColumns)
                col.addJoin(this, fromTable);
            // Add joins
            for (String str : joins.values())
                q.append(str);
            // Where/Group By/Having
            if (whereList.size() > 0) {
                boolean hasWhere = false;
                for (QueryStringBuilder where : whereList) {
                    if (! where.isEmpty()) {
                        // Make sure we actually have a where clause before adding "WHERE"
                        if (! hasWhere)
                            q.append(" WHERE");
                        q.append(hasWhere ? " AND " : "",  "(", where, ")");
                        hasWhere = true;
                    }
                }
            }
            q.append(strGroup).append(strHaving);
            // Union
            if (union != null)
                for (Query other : union)
                    q.append(" UNION ").append(other.toString());
            // Order By
            q.append(strOrder);
            // Limit/offset
            if (limit != null)
                q.append(" LIMIT ").append(limit);
            if (offset != null)
                q.append(" OFFSET ").append(offset);
            Log.v("SQL", q.toString());
            return q.toString();
        }

        // Helper class that provides better append methods
        private static class QueryStringBuilder {
            public StringBuilder q;
            public boolean mHasValue;

            QueryStringBuilder() {
                q = new StringBuilder();
            }
            QueryStringBuilder(String initial) {
                q = new StringBuilder(initial);
            }

            protected QueryStringBuilder(QueryStringBuilder other) {
                q = new StringBuilder(other.q.toString());
                mHasValue = other.mHasValue;
            }

            // append overloads
            public QueryStringBuilder append(Object str) {
                mHasValue = true;
                q.append(str.toString());
                return this;
            }

            public QueryStringBuilder append(Object... args) {
                return appendDelim(" ", args);
            }

            public QueryStringBuilder appendDelim(String delim, Object... args) {
                append(args[0]);
                for (int i = 1; i < args.length; i++)
                    append(delim).append(args[i]);
                return this;
            }

            public boolean isEmpty() {
                return ! mHasValue;
            }

            public String toString() {
                return mHasValue ? q.toString() : "";
            }
        }
    }
}
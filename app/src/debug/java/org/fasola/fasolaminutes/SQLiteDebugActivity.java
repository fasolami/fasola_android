package org.fasola.fasolaminutes;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.System.nanoTime;

public class SQLiteDebugActivity extends BackActivity {
    public static boolean DEBUG_SQLITE = false;
    public static boolean isDebug() {
        return DEBUG_SQLITE;
    }

    public static ArrayList<String> sQueries = new ArrayList<>();
    public static ArrayList<String[]> sQueryArgs = new ArrayList<>();
    public static HashMap<String, Cursor> sCursors = new HashMap<>();
    public static SQLiteDebugActivity sActivity;

    DrawerLayout mDrawerLayout;
    View mDrawer;
    ListView mDrawerList;
    ArrayAdapter<String> mAdapter;
    int mSelection = -1;

    EditText mQueryText;
    EditText mQueryArgs;
    ListView mCursorList;
    ResourceCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_sqlite_activity);
        // Drawer
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawer = findViewById(R.id.query_drawer);
        mDrawerList = (ListView)findViewById(R.id.query_list);
        mAdapter = new ArrayAdapter<String>(this, R.layout.debug_sqlite_list_item, sQueries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (position == mSelection)
                    view.setBackgroundResource(R.color.tab_background);
                else
                    view.setBackgroundColor(Color.TRANSPARENT);
                return view;
            }
        };
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setQuery(position);
                mDrawerLayout.closeDrawer(mDrawer);
            }
        });
        // Main view
        mQueryText = (EditText)findViewById(R.id.query_text);
        mQueryArgs = (EditText)findViewById(R.id.query_args);
        mCursorList = (ListView)findViewById(R.id.cursor_list);
        mCursorAdapter = new ResourceCursorAdapter(this, R.layout.debug_sqlite_list_item, null, 0) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView)view.findViewById(android.R.id.text1)).setText(DatabaseUtils.dumpCurrentRowToString(cursor));
            }
        };
        mCursorList.setAdapter(mCursorAdapter);
        mCursorList.setFastScrollEnabled(true);
        // Select the first item
        setQuery(0);
    }

    // Lifecycle callbacks
    @Override
    protected void onResume() {
        sActivity = this;
        mAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        sActivity = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.debug_sqlite_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Sort
        if (item.getItemId() == R.id.menu_run_query) {
            reloadQuery();
            return true;
        }
        else if (item.getItemId() == R.id.menu_show_drawer) {
            if (mDrawerLayout.isDrawerOpen(mDrawer))
                mDrawerLayout.closeDrawer(mDrawer);
            else
                mDrawerLayout.openDrawer(mDrawer);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Sets a query and optionally reloads the query. */
    protected void setQuery(String query, String[] queryArgs, boolean reload) {
        mQueryText.setText(query);
        mQueryArgs.setText(TextUtils.join(",", queryArgs));
        if (reload) {
            mLoader.setQuery(query, queryArgs);
            getSupportLoaderManager().restartLoader(1, null, mLoader);
        }
        else {
            try {
                mCursorAdapter.changeCursor(sCursors.get(query));
            }
            catch (Exception e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                Log.e("SQLiteDebug", Log.getStackTraceString(e));
            }
        }
    }

    /** Sets a query without reloading. */
    protected void setQuery(String query, String... queryArgs) {
        setQuery(query, queryArgs, false);
    }

    /** Selects the query at the index */
    protected void setQuery(int idx) {
        mSelection = idx;
        setQuery(sQueries.get(idx), sQueryArgs.get(idx));
        mCursorAdapter.notifyDataSetChanged();
    }

    /** Reloads the current query */
    protected void reloadQuery() {
        if (mSelection != -1) {
            sQueries.set(mSelection, mQueryText.getText().toString());
            String queryArgs = mQueryArgs.getText().toString().trim();
            if (queryArgs.isEmpty())
                sQueryArgs.set(mSelection, new String[] {});
            else
                sQueryArgs.set(mSelection, queryArgs.split(","));
            setQuery(sQueries.get(mSelection), sQueryArgs.get(mSelection), true);
        }
    }

    // Custom minutes loader to work around debug hacks
    DebugLoader mLoader = new DebugLoader();

    public class DebugLoader extends MinutesLoader {
        String mStringQuery;
        public void setQuery(String query, String... queryArgs) {
            mStringQuery = query;
            mQueryArgs = queryArgs;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(MinutesApplication.getContext()) {
                @Override
                public Cursor loadInBackground() {
                    Thread.currentThread().setName("MinutesLoader: " + mStringQuery);
                    try {
                        Cursor cursor = MinutesDb.getInstance().query(mStringQuery, mQueryArgs);
                        long start = nanoTime();
                        Log.v("SQL", mStringQuery);
                        Log.v("SQL", "Query length: " + mStringQuery.length());
                        cursor.getCount();
                        Log.v("SQL", "query time (secs): " + (nanoTime() - start) / 1000000000.);
                        // Return hacked up cursor that automatically generates id column
                        return new CursorWrapper(cursor) {
                            static final int ID_COLUMN = 100000;
                            @Override
                            public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
                                if (columnName.equals("_id")) {
                                    int idx = super.getColumnIndex(columnName);
                                    return idx == -1 ? ID_COLUMN : idx;
                                }
                                return super.getColumnIndexOrThrow(columnName);
                            }

                            @Override
                            public long getLong(int columnIndex) {
                                if (columnIndex == ID_COLUMN)
                                    return getPosition();
                                else
                                    return super.getLong(columnIndex);
                            }
                        };
                    }
                    catch (final Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SQLiteDebugActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                                Log.e("SQLiteDebug", Log.getStackTraceString(e));
                            }
                        });
                        return null;
                    }
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            // Don't close old cursor; this will happen in cursorAdapter.swapCursor;
            sCursors.put(mStringQuery, cursor);
            Toast.makeText(
                    SQLiteDebugActivity.this,
                    String.format("%d rows returned from query", cursor == null ? -1 : cursor.getCount()),
                    Toast.LENGTH_SHORT).show();
            SQLiteDebugActivity.this.setQuery(mStringQuery, mQueryArgs);
        }
    }

    /**
     * Adds a query to the query list.
     *
     * <p>If the query already exists, this just updates queryArgs
     */
    public static void addQuery(SQL.Query query, String... queryArgs) {
        String queryString = query.toString();
        int idx = sQueries.indexOf(queryString);
        if (idx == -1) {
            sQueries.add(queryString);
            sQueryArgs.add(queryArgs);
        }
        else {
            sQueryArgs.set(idx, queryArgs);
        }
        if (sActivity != null)
            sActivity.mAdapter.notifyDataSetChanged();
    }

    static {
        sQueries.add("[query]");
        sQueryArgs.add(new String[]{});
    }
}

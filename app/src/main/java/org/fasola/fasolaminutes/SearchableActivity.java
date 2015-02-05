package org.fasola.fasolaminutes;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;


public class SearchableActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("searchable activity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
            doSearch(intent.getStringExtra(SearchManager.QUERY));
    }

    protected void doSearch(String query) {
        MinutesDb db = MinutesDb.getInstance(this);
        String sqlQuery = SQL.select(C.Leader._ID).select(C.Leader.FULL_NAME).as("name")
                .from(C.Leader)
                .where(C.Leader.FIRST_NAME, "LIKE", "?")
                    .or(C.Leader.LAST_NAME, "LIKE", "?")
                .toString();
        Log.v("doSearch", sqlQuery);
        Log.v("doSearch", query + "%");
        Cursor cursor = db.query(sqlQuery, new String[] {query + "%", query + "%"});
        setListAdapter(new SimpleCursorAdapter(
            this, android.R.layout.simple_list_item_1, cursor,
            new String[] {"name"}, new int[] {android.R.id.text1}, 0));
    }
}

package org.fasola.fasolaminutes;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 *  The base class for this app's Activities.
 *  Changes Up button to work as Back button.
 */
public class BackActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! isTaskRoot() && getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        if (BuildConfig.DEBUG) {
            getMenuInflater().inflate(R.menu.debug_sqlite_base_menu, menu);
            return true;
        }
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && getActionBar() != null &&
                (getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            // Use up button as back button
            onBackPressed();
            return true;
        }
        else if (SQLiteDebugActivity.handleOptionsItemSelected(this, item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

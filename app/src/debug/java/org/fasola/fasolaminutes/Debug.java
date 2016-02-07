package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Debug menu hooks.
 */
public class Debug {
    public static boolean onOptionsItemSelected(Context context, MenuItem item) {
        // SQLite
        if (item.getItemId() == R.id.menu_sqlite_debug) {
            context.startActivity(new Intent(context, SQLiteDebugActivity.class));
            return true;
        }
        else if (item.getItemId() == R.id.menu_catch_sql) {
            item.setChecked(! item.isChecked());
            SQLiteDebugActivity.DEBUG_SQLITE = item.isChecked();
            return true;
        }
        return false;
    }

    public static void createOptionsMenu(MenuInflater menuInflater, Menu menu) {
        menuInflater.inflate(R.menu.debug_sqlite_base_menu, menu);
        // Checkables
        MenuItem item = menu.findItem(R.id.menu_catch_sql);
        if (item != null)
            item.setChecked(SQLiteDebugActivity.DEBUG_SQLITE);
    }
}

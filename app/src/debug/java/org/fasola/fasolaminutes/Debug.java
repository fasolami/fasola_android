package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.lang.reflect.Field;

/**
 * Debug menu hooks.
 */
public class Debug {
    public static boolean SIMULATE_NO_WIFI = false;

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
        // Connection
        else if (item.getItemId() == R.id.menu_reset_data_streaming) {
            // Reset session
            try {
                Field f = ConnectionStatus.class.getDeclaredField("mAllowStreaming");
                f.setAccessible(true);
                f.set(null, false);
            }
            catch (Exception e) {
                Log.e("DebugClass", e.toString());
            }
            // Reset shared prefs
            context.getSharedPreferences("PlaybackPreferences", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("allowStreaming", false)
                    .apply();
            return true;
        }
        else if (item.getItemId() == R.id.menu_simulate_no_wifi) {
            item.setChecked(!item.isChecked());
            SIMULATE_NO_WIFI = item.isChecked();
            return true;
        }
        return false;
    }

    public static void createOptionsMenu(MenuInflater menuInflater, Menu menu) {
        menuInflater.inflate(R.menu.debug_sqlite_base_menu, menu);
        menuInflater.inflate(R.menu.debug_connection_menu, menu);
        // Checkables
        MenuItem item = menu.findItem(R.id.menu_catch_sql);
        if (item != null)
            item.setChecked(SQLiteDebugActivity.DEBUG_SQLITE);
        item = menu.findItem(R.id.menu_simulate_no_wifi);
        if (item != null)
            item.setChecked(SIMULATE_NO_WIFI);
    }
}

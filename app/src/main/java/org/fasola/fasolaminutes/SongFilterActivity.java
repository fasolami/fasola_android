package org.fasola.fasolaminutes;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;

import com.appyvet.materialrangebar.RangeBar;

import java.util.ArrayList;

public class SongFilterActivity extends BaseActivity {
    public final static String EXTRA_FILTER_PARCEL = "org.fasola.fasolaminutes.SONG_FILTER_PARCEL";
    public final static String CHECKBOXES = "org.fasola.fasolaminutes.SONG_FILTER_CHECKBOXES";
    public final static String PAGE_RANGE = "org.fasola.fasolaminutes.SONG_FILTER_PAGE_RANGE";
    public final static int[] CHECKBOX_IDS = new int[]{
        R.id.key_a,
        R.id.key_a_flat,
        R.id.key_b,
        R.id.key_b_flat,
        R.id.key_c,
        R.id.key_d,
        R.id.key_d_flat,
        R.id.key_e,
        R.id.key_e_flat,
        R.id.key_f,
        R.id.key_f_sharp,
        R.id.key_g,
        R.id.key_major,
        R.id.key_minor,
        R.id.key_single,
        R.id.key_multiple,
        R.id.time_2_2,
        R.id.time_2_4,
        R.id.time_3_2,
        R.id.time_3_4,
        R.id.time_4_4,
        R.id.time_6_4,
        R.id.time_6_8,
        R.id.time_single,
        R.id.time_multiple,
        R.id.page_bottom,
        R.id.page_full,
        R.id.page_left,
        R.id.page_right,
        R.id.page_top,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_song_filter);
        // This should be set in the style, but apparently we can't use this in xml
        // See: https://issuetracker.google.com/issues/37036728
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.key_layout).setClipToOutline(true);
            findViewById(R.id.major_minor_layout).setClipToOutline(true);
            findViewById(R.id.multiple_key_layout).setClipToOutline(true);
            findViewById(R.id.time_layout).setClipToOutline(true);
            findViewById(R.id.multiple_time_layout).setClipToOutline(true);
            findViewById(R.id.page_orientation_layout).setClipToOutline(true);
            findViewById(R.id.page_left_right_layout).setClipToOutline(true);
        }
        // Set the initial state
        loadBundle(savedInstanceState != null ?
            savedInstanceState.getBundle(EXTRA_FILTER_PARCEL) :
            getIntent().getBundleExtra(EXTRA_FILTER_PARCEL));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_song_filter_activity, menu);
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void clear() {
        for (int id : CHECKBOX_IDS)
            ((CheckBox) findViewById(id)).setChecked(false);
        RangeBar pageRange = ((RangeBar) findViewById(R.id.page_number));
        pageRange.setRangePinsByIndices(0, pageRange.getTickCount() - 1);
    }

    private void loadBundle(Bundle state) {
        clear();
        if (state != null) {
            // restore checkboxes
            ArrayList<Integer> checkboxes = state.getIntegerArrayList(CHECKBOXES);
            if (checkboxes != null) {
                for (int id : checkboxes) {
                    ((CheckBox) findViewById(id)).setChecked(true);
                }
            }
            // restore page range
            int [] pages = state.getIntArray(PAGE_RANGE);
            if (pages != null && pages.length == 2) {
                RangeBar pageRange = ((RangeBar) findViewById(R.id.page_number));
                int offset = (int)pageRange.getTickStart();
                pageRange.setRangePinsByIndices(pages[0] - offset, pages[1] - offset);
            }
        }
    }

    public Bundle saveBundle() {
        Bundle state = new Bundle();
        // all checked checkboxes
        ArrayList<Integer> checkboxes = new ArrayList<>();
        for (int id : CHECKBOX_IDS) {
            if (((CheckBox) findViewById(id)).isChecked())
                checkboxes.add(id);
        }
        state.putIntegerArrayList(CHECKBOXES, checkboxes);
        // page range
        RangeBar pageRange = ((RangeBar) findViewById(R.id.page_number));
        if (pageRange.getLeftIndex() != 0 || pageRange.getRightIndex() != pageRange.getTickCount() - 1) {
            int offset = (int) pageRange.getTickStart();
            state.putIntArray(PAGE_RANGE, new int[]{
                offset + pageRange.getLeftIndex(),
                offset + pageRange.getRightIndex()
            });
        }
        return state;
    }

    public void onSaveInstanceState(final Bundle saveInstanceState) {
        super.onSaveInstanceState(saveInstanceState);
        saveInstanceState.putBundle(EXTRA_FILTER_PARCEL, saveBundle());
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_FILTER_PARCEL, saveBundle());
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }
}
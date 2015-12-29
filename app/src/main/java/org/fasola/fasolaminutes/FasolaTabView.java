package org.fasola.fasolaminutes;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FasolaTabView extends LinearLayout {
    View mSelectedView;
    int NORMAL_COLOR;
    int SELECTED_COLOR;

    public FasolaTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        NORMAL_COLOR = getResources().getColor(R.color.fasolatab_deselected);
        SELECTED_COLOR = getResources().getColor(R.color.fasolatab_selected);
        // Inflate the layout
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.fasola_tabs, this, true);
        setBackgroundColor(getResources().getColor(R.color.fasolatab_background));
        // Apply color and click handler to items
        for (int i = 0; i < getChildCount(); i++) {
            setIconColor(getChildAt(i), NORMAL_COLOR);
            final int idx = i;
            getChildAt(i).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(MainActivity.ACTIVITY_POSITION, idx);
                    getContext().startActivity(intent);
                }
            });
        }
        // Make playlist tab open the drawer
        findViewById(R.id.tab_playlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View root = view.getRootView();
                DrawerLayout drawerLayout = (DrawerLayout)root.findViewById(R.id.drawer_layout);
                View playlistDrawer = root.findViewById(R.id.playlist_drawer);
                if (drawerLayout != null && playlistDrawer != null)
                    drawerLayout.openDrawer(playlistDrawer);
            }
        });
    }

    private void setIconColor(View textView, int color) {
        if (textView == null)
            return;
        Drawable icon = ((TextView) textView).getCompoundDrawables()[1]; // Top
        icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public void setSelection(int idx) {
        setIconColor(mSelectedView, NORMAL_COLOR);
        mSelectedView = getChildAt(idx);
        setIconColor(mSelectedView, SELECTED_COLOR);
    }
}

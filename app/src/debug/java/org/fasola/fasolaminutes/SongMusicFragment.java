package org.fasola.fasolaminutes;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;


/** Fragment that loads music from sacredharpbremen in a WebView. */
public class SongMusicFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return new WebView(getActivity());
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long id = getArguments().getLong(CursorListFragment.EXTRA_ID, -1);
        // Stats summary
        SQL.Query query = C.Song.select(C.Song.number, C.Song.title, C.Song.titleOrdinal).whereEq(C.Song.id);
        getLoaderManager().initLoader(1, null, new MinutesLoader(query, String.valueOf(id)) {
            @Override
            public void onLoadFinished(Cursor cursor) {
                int color = getResources().getColor(R.color.fasola_background);
                int smallestComponent = Math.min(Color.red(color),
                        Math.min(Color.green(color),
                                Color.blue(color)));
                // largest alpha we an achieve to get the given color;
                double alpha = smallestComponent / 255.0;
                // Set the color
                int tintColor = Color.rgb(
                        0xff & (int) (Math.max(0, (Color.red(color) - 255 * alpha) / (1 - alpha))),
                        0xff & (int) (Math.max(0, (Color.green(color) - 255 * alpha) / (1 - alpha))),
                        0xff & (int) (Math.max(0, (Color.blue(color) - 255 * alpha) / (1 - alpha)))
                );

                // Make the image url
                cursor.moveToFirst();
                String page = cursor.getString(0);
                String title = cursor.getString(1);
                String ordinal = cursor.getString(2).toLowerCase();
                boolean needsZero = page.length() == 2 ||
                        (page.length() == 3 && Character.isLetter(page.charAt(2)));
                String url = String.format(
                        "http://www.sacredharpbremen.org/ressourcen/%s-%s/%s %s.jpg",
                        needsZero ? "0" + page : page,
                        title.toLowerCase().replaceAll("[^a-z]", "-") +
                                (ordinal.isEmpty() ? "" : "-" + ordinal),
                        page,
                        title + (ordinal.isEmpty() ? "" : " (" + ordinal + ")")
                );

                WebView webView = (WebView) view;
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setBuiltInZoomControls(true);
                webView.getSettings().setDisplayZoomControls(false);
                String data = String.format(
                        "<body style='margin: 0; padding: 0; background-color: #%06X;'>" +
                            "<div style='float: left; background-color: #%06X;'>" +
                                "<img style='opacity: %.4f;' src='%s'>" +
                            "</div>" +
                        "</body>",
                        0xffffff & color,
                        0xffffff & tintColor,
                        alpha,
                        url);
                Log.v("SongMusicFragment", "Loading webview:" + data);
                webView.loadData(data, "text/html", "utf-8");
            }
        });
    }
}
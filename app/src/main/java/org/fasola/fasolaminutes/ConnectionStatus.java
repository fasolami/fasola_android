package org.fasola.fasolaminutes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

public class ConnectionStatus {
    private static boolean mAllowStreaming;
    private static final String PREFERENCES_FILE = "PlaybackPreferences";
    private static final String STREAMING_KEY = "allowStreaming";

    // Connection enums
    public static final int CAN_PLAY = 0;
    public static final int NO_CONNECTION = 1;
    public static final int NO_WIFI = 2;

    /** PlaybackService action from ConnectionStatus */
    public static final String ACTION_PLAY = "org.fasola.fasolaminutes.action.CONNECTION_PLAY";

    /** Returns true if playback is allowed (on wifi or has already asked user). */
    public static boolean canPlay(Context context) {
        return getPlayStatus(context) == CAN_PLAY;
    }

    /** Returns connection status: one of {@code CAN_PLAY, NO_CONNECTION, NO_WIFI}. */
    public static int getPlayStatus(Context context) {
        // Allowed for this session
        if (mAllowStreaming)
            return CAN_PLAY;
        // Always allowed
        final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        if (preferences.getBoolean(STREAMING_KEY, false)) {
            mAllowStreaming = true;
            return CAN_PLAY;
        }
        // Check for wifi connection
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = cm.getActiveNetworkInfo();
        if (network != null && network.isConnectedOrConnecting()) {
            if (network.getType() == ConnectivityManager.TYPE_WIFI) {
                if (! BuildConfig.DEBUG)
                    return CAN_PLAY;
                else {
                    return Debug.SIMULATE_NO_WIFI ? NO_WIFI : CAN_PLAY;
                }
            }
            else
                return NO_WIFI;
        }
        else
            return NO_CONNECTION;
    }

    /**
     * Prompts the user to allow streaming without wifi.
     *
     * <p>Starts playback if streaming is allowed or connection is wifi.
     *
     * @param context FragmentActivity to use for showing the prompt.
     *                If this is not a FragmentActivity, prompts on the top activity.
     *                If no activities exist, starts a NowPlayingActivity and prompts.
     */
    public static void promptStreaming(Context context) {
        switch(getPlayStatus(context)) {
            case CAN_PLAY:
                startPlayback(context);
                break;
            case NO_CONNECTION:
                Toast.makeText(context, "No connection available.", Toast.LENGTH_SHORT).show();
                break;
            case NO_WIFI:
                // Prompt with passed context
                if (context instanceof FragmentActivity) {
                    ConnectionDialogFragment dialog = new ConnectionDialogFragment();
                    dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "streaming_dialog");
                    context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                }
                // Prompt with top activity
                else {
                    Activity top = MinutesApplication.getTopActivity();
                    if (top instanceof FragmentActivity) {
                        ConnectionDialogFragment dialog = new ConnectionDialogFragment();
                        dialog.show(((FragmentActivity) top).getSupportFragmentManager(), "streaming_dialog");
                        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    }
                    else if (top == null) {
                        // Prompt by opening NowPlayingActivity with synthesized back stack.
                        context.startActivities(new Intent[] {
                                new Intent(context, MainActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                new Intent(context, NowPlayingActivity.class)
                                        .setAction(NowPlayingActivity.PROMPT_STREAMING)
                        });
                        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    }
                    else {
                        Log.e("ConnectionStatus", "Expected a FragmentActivity");
                    }
                }
                break;
        }
    }

    // Start playback
    private static void startPlayback(Context context) {
        Intent intent = new Intent(ACTION_PLAY, null, context, PlaybackService.class);
        context.startService(intent);
    }

    /**
    * A DialogFragment that prompts the user to allow streaming over wifi
    */
    public static class ConnectionDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.streaming_prompt);
            builder.setPositiveButton("Always Allow", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAllowStreaming = true;
                    Context context = getActivity();
                    SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
                    preferences.edit().putBoolean(STREAMING_KEY, true).apply();
                    startPlayback(context);
                }
            });
            builder.setNeutralButton("Allow", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAllowStreaming = true;
                    startPlayback(getActivity());
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAllowStreaming = false;
                }
            });
            return builder.create();
        }
    }
}

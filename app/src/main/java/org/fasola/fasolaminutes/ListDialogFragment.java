package org.fasola.fasolaminutes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * A simple DialogFragment with a list
 */
public class ListDialogFragment extends DialogFragment {
    public interface Listener {
        void onListDialogClick(DialogFragment dialog, int which, Bundle data);
    }

    Listener mListener;

    public static ListDialogFragment newInstance(int title, String[] items) {
        return newInstance(title, items, null);
    }

    public static ListDialogFragment newInstance(int title, String[] items, Bundle data) {
        ListDialogFragment frag = new ListDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putStringArray("items", items);
        args.putBundle("bundle", data);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        String[] items = getArguments().getStringArray("items");
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onListDialogClick(
                            ListDialogFragment.this, which, getArguments().getBundle("bundle"));
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host implements the callback interface
        Fragment target = getTargetFragment(); // Host can be a Fragment as well as an Activity
        try {
            mListener = (Listener)(target != null ? target : activity);
        } catch (ClassCastException e) {
            throw new ClassCastException((target != null ? target : activity).toString()
                    + " must implement ListDialogFragment.Listener");
        }
    }
}

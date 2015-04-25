package org.fasola.fasolaminutes;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

/**
 * A simple DialogFragment with a list
 */
public class ListDialogFragment extends DialogFragment {
    String mTitle;
    String[] mItems;
    DialogInterface.OnClickListener mListener;

    public ListDialogFragment(String title, String[] items, DialogInterface.OnClickListener listener) {
        super();
        mTitle = title;
        mItems = items;
        mListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(mTitle).setItems(mItems, mListener);
        return builder.create();
    }
}

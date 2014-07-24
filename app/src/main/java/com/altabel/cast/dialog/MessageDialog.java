package com.altabel.cast.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.altabel.cast.R;

/**
 * Created by nikolai on 24.07.2014.
 */
public class MessageDialog extends DialogFragment {
    MessageDialogListener listener;
    private Activity activity;
    private String title = null;
    private String message = null;
    private String namePositiveBtn = null;
    private String nameNegativeBtn = null;

    public MessageDialog(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public MessageDialog() {}

    public MessageDialog(String title, String message, String namePositiveBtn, String nameNegativeBtn) {
        this.title = title;
        this.message = message;
        this.namePositiveBtn = namePositiveBtn;
        this.nameNegativeBtn = nameNegativeBtn;
    }

    public interface MessageDialogListener{
        public void onMsgDialogPositiveClick(DialogFragment dialog);
        public void onMsgDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        try{
            listener = (MessageDialogListener) activity;
        }catch (ClassCastException exc){
            throw new ClassCastException(activity.toString() + " must implement MessageDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ContextThemeWrapper theme = new ContextThemeWrapper(activity, android.R.style.Theme_Holo_Light_Dialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(theme);

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View dialogView = layoutInflater.inflate(R.layout.dialog, null);

        if(message != null && !message.isEmpty()){
            TextView textView = (TextView)dialogView.findViewById(R.id.tv_dialog_info);
            textView.setText(message);
        }

        RelativeLayout positive = (RelativeLayout) dialogView.findViewById(R.id.btn_positive);
        if(namePositiveBtn != null) {
            ((TextView) positive.findViewById(R.id.positiveTextButton)).setText(namePositiveBtn);
            positive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onMsgDialogPositiveClick(MessageDialog.this);
                }
            });
        } else {
            positive.setVisibility(View.GONE);
        }

        RelativeLayout negative = (RelativeLayout) dialogView.findViewById(R.id.btn_negative);
        if(nameNegativeBtn != null) {
            ((TextView) negative.findViewById(R.id.negativeTextButton)).setText(nameNegativeBtn);
            negative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onMsgDialogNegativeClick(MessageDialog.this);
                }
            });
        } else {
            negative.setVisibility(View.GONE);
        }

        builder.setView(dialogView);
        builder.setCancelable(false);
        return builder.create();
    }
}

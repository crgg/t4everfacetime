package com.t4app.videocalltest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class MessagesUtils {
    private static boolean isDialogShowing = false;
    private static AlertDialog currentDialog;

    public static void showErrorDialog(Context context, String errorMessage) {
        if (errorMessage == null){
            return;
        }
        if (isDialogShowing){
            return;
        }
        isDialogShowing = true;

        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            Activity activity = (Activity) context;
            LayoutInflater inflater = activity.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.normal_error_layout, null);
            builder.setView(dialogView)
                    .setCancelable(false);

            currentDialog = builder.create();
            Objects.requireNonNull(currentDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
            Button ok_btn = dialogView.findViewById(R.id.ok_btn);

            tvMessage.setText(errorMessage);

            ok_btn.setOnClickListener(new SafeClickListener() {
                @Override
                public void onSafeClick(View v) {
                    currentDialog.dismiss();
                    isDialogShowing = false;
                }
            });

            currentDialog.show();
        } catch (Exception e) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            isDialogShowing = false;
        }
    }

}

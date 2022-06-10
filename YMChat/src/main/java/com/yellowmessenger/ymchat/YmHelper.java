package com.yellowmessenger.ymchat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

public class YmHelper {

    private static void startInstalledAppDetailsActivity(@NonNull final Context context) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    public static void showSnackBarWithSettingAction(@NonNull final Context context, @NonNull final View view, @NonNull final String message) {
        Snackbar snackbar = Snackbar.make(view,
                message,
                Snackbar.LENGTH_LONG);
        TextView textView =
                (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
        textView.setSingleLine(false);

        snackbar.setAction(context.getString(R.string.ym_text_settings), v -> startInstalledAppDetailsActivity(context))
                .show();
    }

    public static void showMessageInSnackBar(@NonNull final View view, @NonNull final String message) {
        Snackbar snackbar = Snackbar.make(view,
                message,
                Snackbar.LENGTH_LONG);
        TextView textView =
                (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
        textView.setSingleLine(false);
        snackbar.setAction("", v -> {
                })
                .show();
    }

}

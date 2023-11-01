package com.w3engineers.mesh.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

import androidx.appcompat.app.AlertDialog;

import com.w3engineers.mesh.R;

/**
 * Provides Dialogue related utility
 */
public class DialogUtil {
    static ProgressDialog progressDialog;
    static AlertDialog alertDialog;

    public static void showConfirmationDialog(Context context,
                                              String title,
                                              String message,
                                              String negativeText,
                                              String positiveText,
                                              final DialogButtonListener listener) {
        try {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context, R.style.DefaultAlertDialogStyle);
            alertDialogBuilder.setTitle(Html.fromHtml("<b>" + title + "</b>"));
            alertDialogBuilder.setMessage(Html.fromHtml("<font color='#757575'>" + message + "</font>"));

            if (negativeText != null) {
                alertDialogBuilder.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.onClickNegative();
                        }

                        dialog.cancel();
                    }
                });
            }

            if (positiveText != null) {
                alertDialogBuilder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (listener != null) {
                            listener.onClickPositive();
                        }

                        dialog.cancel();
                    }
                });
            }

            alertDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (listener != null) {
                        listener.onCancel();
                    }

                    dialog.cancel();
                }
            });
            alertDialog = alertDialogBuilder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public interface DialogButtonListener {
        void onClickPositive();

        void onCancel();


        void onClickNegative();
    }

    public static void dismissDialog() {
        if (alertDialog != null) {
            try {
                alertDialog.dismiss();
                alertDialog = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void showLoadingProgress(Context context) {
        try {
            if (progressDialog != null) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = null;
            }
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void dismissLoadingProgress() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isDialogShowing() {
        if (alertDialog != null) {
            return true;
        } else {
            return false;
        }
    }
}

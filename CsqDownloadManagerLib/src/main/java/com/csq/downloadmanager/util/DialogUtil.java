/**
 * description :
 * E-mail:csqwyyx@163.com
 * github:https://github.com/John-Chen
 */
package com.csq.downloadmanager.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import com.csq.downloadmanager.R;

public class DialogUtil {

    // ------------------------ Constants ------------------------


    // ------------------------- Fields --------------------------


    // ----------------------- Constructors ----------------------


    // -------- Methods for/from SuperClass/Interfaces -----------


    // --------------------- Methods public ----------------------

    public static void showConfirmDialog(@NonNull Context context,
                                         @NonNull String message,
                                         @NonNull final ConfirmListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.prompt);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.ok();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.cancel();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // --------------------- Methods private ---------------------


    // --------------------- Getter & Setter -----------------


    // --------------- Inner and Anonymous Classes ---------------

    public interface ConfirmListener{
        void ok();
        void cancel();
    }

    // --------------------- logical fragments -----------------

}

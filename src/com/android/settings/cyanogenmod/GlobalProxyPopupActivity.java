package com.android.settings.cyanogenmod;

import org.cyanogenmod.support.proxy.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;

public class GlobalProxyPopupActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String callingPackage = getCallingPackage();
        if (TextUtils.isEmpty(callingPackage)) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String appName = getApplicationName(this, callingPackage);
        String formattedMessage = String.format(getString(R.string.global_proxy_dialog_message, appName));
        builder.setMessage(formattedMessage);
        builder.setTitle(R.string.global_proxy_dialog_title);

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Settings.Global.putString(getContentResolver(), Settings.Global.GLOBAL_PROXY_PACKAGE_NAME, callingPackage);
                Util.broadcastProxyStateChange(GlobalProxyPopupActivity.this, callingPackage);
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
        builder.show();
    }

    private String getApplicationName(Context ctx, String pkgName) {
        PackageManager manager = ctx.getPackageManager();
        String applicationName = null;
        try {
            applicationName = (String) manager.getApplicationInfo(pkgName, 0)
                    .loadLabel(manager);
        } catch (NameNotFoundException ignore) {
        }
        return applicationName;
    }
}

package com.android.settings.cyanogenmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cyanogenmod.support.proxy.GlobalProxyManager;
import org.cyanogenmod.support.proxy.Util;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import com.android.settings.R;

public class GlobalProxyDialog extends DialogPreference {

    public GlobalProxyDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GlobalProxyDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        builder.setPositiveButton(null, null);
        builder.setNegativeButton(null, null);
        final List<PackageInfo> packages = getProxyManagers();
        Collections.sort(packages, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo lhs, PackageInfo rhs) {
                return lhs.applicationLabel.compareTo(rhs.applicationLabel);
            }
        });

        PackageInfo dummy = new PackageInfo();
        dummy.applicationLabel = getContext().getString(R.string.global_proxy_dialog_default);
        packages.add(0, dummy);

        ArrayAdapter adapter = new ArrayAdapter(getContext(),
                com.android.internal.R.layout.select_dialog_singlechoice, packages) {
            @Override
            public String getItem(int position) {
                return packages.get(position).applicationLabel;
            }
        };

        int selectedIndex = 0;
        String currentPackage = getCurrentProxyPackage();
        if (!TextUtils.isEmpty(currentPackage)) {
            for (int i = 1; i < packages.size(); i++) {
                if (packages.get(i).packageName.equals(currentPackage)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        builder.setSingleChoiceItems(adapter, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                PackageInfo info = packages.get(position);
                ContentResolver res = getContext().getContentResolver();
                Settings.Global.putString(res, Settings.Global.GLOBAL_PROXY_PACKAGE_NAME,
                        info.packageName);

                if (TextUtils.isEmpty(info.packageName)) {
                    ConnectivityManager connectivityManager = (ConnectivityManager)
                            getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    connectivityManager.setGlobalProxy(null);
                }
                Util.broadcastProxyStateChange(getContext(), info.packageName);
                getDialog().dismiss();
            }
        });
    }

    private String getCurrentProxyPackage() {
        ContentResolver res = getContext().getContentResolver();
        return Settings.Global.getString(res, Settings.Global.GLOBAL_PROXY_PACKAGE_NAME);
    }

    private List<PackageInfo> getProxyManagers() {
        List<PackageInfo> packages = new ArrayList<PackageInfo>();
        PackageManager pkgManager = getContext().getPackageManager();
        Intent i = new Intent();
        i.setAction(GlobalProxyManager.PROXY_CHANGE_ACTION);
        List<ResolveInfo> matches = pkgManager.queryBroadcastReceivers(i, 0);
        for (ResolveInfo rInfo : matches) {
            PackageInfo info = new PackageInfo();
            info.applicationLabel = rInfo.loadLabel(pkgManager).toString();
            info.packageName = rInfo.activityInfo.packageName;
            packages.add(info);
        }
        return packages;
    }

    class PackageInfo {
        String packageName;
        String applicationLabel;
    }

}

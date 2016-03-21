/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import cyanogenmod.providers.CMSettings;
import cyanogenmod.weatherservice.WeatherProviderService;

import static org.cyanogenmod.internal.logging.CMMetricsLogger.WEATHER_SETTINGS;

import java.util.ArrayList;
import java.util.List;

public class WeatherServiceSettings extends SettingsPreferenceFragment {

    private Context mContext;
    private WeatherProviderServiceInfoAdapter mAdapter;
    private Handler mHandler;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    protected int getMetricsCategory() {
        return WEATHER_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
        registerPackageMonitor();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterPackageMonitor();
    }

    private void registerPackageMonitor() {
        mPackageMonitor.register(mContext, BackgroundThread.getHandler().getLooper(),
                UserHandle.ALL, true);
    }

    private void unregisterPackageMonitor() {
        mPackageMonitor.unregister();
    }

    private PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateAdapter();
                }
            });
        }
    };

    private void updateAdapter() {
        final PackageManager pm = getContext().getPackageManager();
        final Intent intent = new Intent(WeatherProviderService.SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfoList = pm.queryIntentServices(intent,
                PackageManager.GET_SERVICES);
        List<WeatherProviderServiceInfo> weatherProviderServiceInfos
                = new ArrayList<>(resolveInfoList.size());
        ComponentName activeService = getEnabledWeatherServiceProvider();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (resolveInfo.serviceInfo == null) continue;

            if (resolveInfo.serviceInfo.packageName == null
                    || resolveInfo.serviceInfo.name == null) {
                //Really?
                continue;
            }

            if (!resolveInfo.serviceInfo.permission.equals(
                    cyanogenmod.platform.Manifest.permission.BIND_WEATHER_PROVIDER_SERVICE)) {
                continue;
            }
            WeatherProviderServiceInfo serviceInfo = new WeatherProviderServiceInfo();
            serviceInfo.componentName = new ComponentName(resolveInfo.serviceInfo.packageName,
                    resolveInfo.serviceInfo.name);
            serviceInfo.isActive = serviceInfo.componentName.equals(activeService);
            serviceInfo.caption = resolveInfo.loadLabel(pm);
            serviceInfo.icon = resolveInfo.loadIcon(pm);

            weatherProviderServiceInfos.add(serviceInfo);
        }
        mAdapter.clear();
        mAdapter.addAll(weatherProviderServiceInfos);

    }

    private ComponentName getEnabledWeatherServiceProvider() {
        String activeWeatherServiceProvider = CMSettings.Secure.getString(
                mContext.getContentResolver(), CMSettings.Secure.WEATHER_PROVIDER_SERVICE);
        if (activeWeatherServiceProvider == null) return null;
        return ComponentName.unflattenFromString(activeWeatherServiceProvider);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = getListView();
        ViewGroup contentRoot = (ViewGroup) listView.getParent();
        listView.setItemsCanFocus(true);

        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.empty_weather_state, contentRoot, false);
        TextView emptyTextView = (TextView) emptyView.findViewById(R.id.message);
        emptyTextView.setText(R.string.weather_settings_no_services_prompt);

        listView.setEmptyView(emptyView);

        contentRoot.addView(emptyView);
        mAdapter = new WeatherProviderServiceInfoAdapter(mContext);
        listView.setAdapter(mAdapter);
    }

    private class WeatherProviderServiceInfo {
        CharSequence caption;
        Drawable icon;
        boolean isActive;
        ComponentName componentName;
    }

    private class WeatherProviderServiceInfoAdapter
            extends ArrayAdapter<WeatherProviderServiceInfo> {

        private final LayoutInflater mInflater;

        public WeatherProviderServiceInfoAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            WeatherProviderServiceInfo info = getItem(position);
            final View row = convertView != null ? convertView :
                    buildRow(parent);
            row.setTag(info);

            ((ImageView) row.findViewById(android.R.id.icon))
                    .setImageDrawable(info.icon);

            ((TextView) row.findViewById(android.R.id.title)).setText(info.caption);

            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
            radioButton.setChecked(info.isActive);
            radioButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    row.onTouchEvent(event);
                    return false;
                }
            });

            return row;
        }

        private View buildRow(ViewGroup parent) {
            final View row =  mInflater.inflate(R.layout.weather_service_provider_info_row,
                    parent, false);
            final View header = row.findViewById(android.R.id.widget_frame);
            header.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    setActiveWeatherProviderService((WeatherProviderServiceInfo) row.getTag());
                }
            });

            return row;
        }

        private void setActiveWeatherProviderService(WeatherProviderServiceInfo info) {
            WeatherProviderServiceInfo currentSelection = getCurrentSelection();
            if (info.equals(currentSelection)) return;
            if (currentSelection != null) {
                currentSelection.isActive = false;
            }
            info.isActive = true;
            CMSettings.Secure.putString(mContext.getContentResolver(),
                    CMSettings.Secure.WEATHER_PROVIDER_SERVICE,
                        info.componentName.flattenToString());
            notifyDataSetChanged();
        }

        private WeatherProviderServiceInfo getCurrentSelection() {
            for (int indx = 0; indx < getCount(); indx++) {
                WeatherProviderServiceInfo info = getItem(indx);
                if (info.isActive) {
                    return info;
                }
            }
            return null;
        }
    }
}

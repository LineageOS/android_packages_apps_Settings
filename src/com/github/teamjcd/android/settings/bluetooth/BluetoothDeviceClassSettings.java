package com.github.teamjcd.android.settings.bluetooth;

import android.annotation.SuppressLint;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassData;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassStore;

import java.util.List;

import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassStore.getBluetoothDeviceClassStore;

public class BluetoothDeviceClassSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    public static final String ACTION_BLUETOOTH_DEVICE_CLASS_SETTINGS = "com.github.teamjcd.android.settings.BLUETOOTH_DEVICE_CLASS_SETTINGS";

    private static final String TAG = "BluetoothDeviceClassSettings";

    private static final int MENU_NEW = Menu.FIRST;

    private BluetoothAdapter mAdapter;
    private BluetoothDeviceClassStore mStore;

    private Integer mSelectedKey;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_DEVICE_PICKER;
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mStore = getBluetoothDeviceClassStore(getPrefContext());

        //Register Broadcast to wait enable bluetooth
        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        } else {
            saveInitialValue();
            fillList();
        }
    }

    @SuppressLint("NewApi")
    private void saveInitialValue() {
        BluetoothDeviceClassData defaultClass = mStore.getDefault();
        if (defaultClass == null) {
            BluetoothClass bluetoothClass = mAdapter.getBluetoothClass();
            mStore.saveDefault(new BluetoothDeviceClassData(
                    "Default",
                    bluetoothClass.getDeviceClass()
            ));
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.bluetooth_device_class_settings);
    }

    @Override
    public void onResume() {
        super.onResume();
        fillList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new))
                .setIcon(R.drawable.ic_add_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW:
                addNewBluetoothDeviceClass();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());

        if (newValue instanceof String) {
            setSelectedBluetoothDeviceClassKey((String) newValue);
        }

        return true;
    }

    private void fillList() {
        List<BluetoothDeviceClassData> codDataList = mStore.getAll();
        Log.d(TAG, "fillList(): codDataList - " + codDataList);

        if (!codDataList.isEmpty()) {
            final PreferenceGroup codPrefList = (PreferenceGroup) findPreference("bluetooth_device_class_list");
            codPrefList.removeAll();

            mSelectedKey = getSelectedBluetoothDeviceClassKey();

            for (BluetoothDeviceClassData codData : codDataList) {
                final BluetoothDeviceClassPreference pref = new BluetoothDeviceClassPreference(getPrefContext());

                pref.setKey(Integer.toString(codData.getId()));
                pref.setTitle(codData.getName());
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                pref.setSummary(Integer.toHexString(codData.getDeviceClass()));

                if ((mSelectedKey != null) && mSelectedKey.equals(codData.getId())) {
                    pref.setChecked();
                }

                codPrefList.addPreference(pref);
            }
        }
    }

    private Integer getSelectedBluetoothDeviceClassKey() {
        // TODO
        return null;
    }

    private void setSelectedBluetoothDeviceClassKey(String key) {
        // TODO
    }

    private void addNewBluetoothDeviceClass() {
        // TODO
    }

    private void restoreDefaultBluetoothDeviceClass() {
        // TODO
    }
}

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

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassData;
import com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassStore;

import static com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassStore.getBluetoothDeviceClassStore;

public class BluetoothDeviceClassSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    public static final String ACTION_BLUETOOTH_DEVICE_CLASS_SETTINGS = "com.github.teamjcd.android.settings.BLUETOOTH_DEVICE_CLASS_SETTINGS";

    public static final String BLUETOOTH_DEVICE_CLASS_ID = "bluetooth_device_class_id";

    private static final String TAG = "BluetoothDeviceClassSettings";

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static boolean mRestoreDefaultBluetoothDeviceClassMode;

    private HandlerThread mRestoreDefaultBluetoothDeviceClassThread;

    private BluetoothAdapter adapter;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_DEVICE_PICKER;
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = BluetoothAdapter.getDefaultAdapter();

        //Register Broadcast to wait enable bluetooth
        if (!adapter.isEnabled()) {
            adapter.enable();
        } else {
            saveInitialValue();
        }
    }

    @SuppressLint("NewApi")
    private void saveInitialValue() {
        //TODO Provider does not register
        // Error : Failed to find provider info for com.github.teamjcd.android.settings.bluetooth.db.BluetoothDeviceClassContentProvider
//        BluetoothDeviceClassStore bluetoothDeviceClassStore = getBluetoothDeviceClassStore(this.getPrefContext());
//        BluetoothDeviceClassData defaultClass = bluetoothDeviceClassStore.getDefault();
//        if (defaultClass == null) {
//            BluetoothClass bluetoothClass = adapter.getBluetoothClass();
//            bluetoothDeviceClassStore.saveDefault(new BluetoothDeviceClassData(
//                    "Default",
//                    bluetoothClass.getDeviceClass()
//            ));
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.bluetooth_device_class_settings);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mRestoreDefaultBluetoothDeviceClassMode) {
            fillList();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultBluetoothDeviceClassThread != null) {
            mRestoreDefaultBluetoothDeviceClassThread.quit();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new))
                .setIcon(R.drawable.ic_add_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        /*menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);*/

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW:
                addNewBluetoothDeviceClass();
                return true;
            case MENU_RESTORE:
                restoreDefaultBluetoothDeviceClass();
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
        // TODO
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

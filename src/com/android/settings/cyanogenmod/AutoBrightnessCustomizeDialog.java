/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Spline;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

import com.android.settings.R;

public class AutoBrightnessCustomizeDialog extends AlertDialog
        implements DialogInterface.OnClickListener {
    private static final String TAG = "AutoBrightnessCustomizeDialog";

    private TextView mSensorLevel;
    private ListView mConfigList;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private static class SettingRow {
        int lux;
        int backlight;
        public SettingRow(int lux, int backlight) {
            this.lux = lux;
            this.backlight = backlight;
        }
    };

    private SettingRowAdapter mAdapter;
    private int mMinLevel;
    private boolean mIsDefault;

    private SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            final int lux = Math.round(event.values[0]);
            mSensorLevel.setText(getContext().getString(R.string.light_sensor_current_value, lux));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public AutoBrightnessCustomizeDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Context context = getContext();
        View view = getLayoutInflater().inflate(R.layout.dialog_auto_brightness_levels, null);
        setView(view);
        setTitle(R.string.auto_brightness_dialog_title);
        setCancelable(true);

        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok), this);
        setButton(DialogInterface.BUTTON_NEUTRAL,
                context.getString(R.string.auto_brightness_reset_button), this);
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), this);

        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mMinLevel = pm.getMinimumAbsoluteScreenBrightness();

        mSensorLevel = (TextView) view.findViewById(R.id.light_sensor_value);

        ImageButton more = (ImageButton) view.findViewById(R.id.more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pm = new PopupMenu(v.getContext(), v);
                pm.inflate(R.menu.auto_brightness_setup_more);
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.auto_brightness_menu_preview:
                                showPreview();
                                return true;
                            case R.id.auto_brightness_menu_help:
                                showHelp();
                                return true;
                        }
                        return false;
                    }
                });
                pm.show();
             }
        });

        mConfigList = (ListView) view.findViewById(android.R.id.list);
        mAdapter = new SettingRowAdapter(context, new ArrayList<SettingRow>());
        mConfigList.setAdapter(mAdapter);
        registerForContextMenu(mConfigList);
    }

    @Override
    protected void onStart() {
        updateSettings(false);

        super.onStart();

        mSensorManager.registerListener(mLightSensorListener,
                mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        Button neutralButton = getButton(DialogInterface.BUTTON_NEUTRAL);
        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirmation();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(mLightSensorListener, mLightSensor);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        menu.setHeaderTitle(R.string.auto_brightness_level_options);

        menu.add(Menu.NONE, Menu.FIRST, 0, R.string.auto_brightness_menu_edit_lux)
                .setEnabled(!mAdapter.isLastItem(info.position));
        menu.add(Menu.NONE, Menu.FIRST + 1, 1, R.string.auto_brightness_menu_split)
                .setEnabled(mAdapter.canSplitRow(info.position));
        menu.add(Menu.NONE, Menu.FIRST + 2, 2, R.string.auto_brightness_menu_remove)
                .setEnabled(mAdapter.getCount() > 1);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;

        switch (item.getItemId() - Menu.FIRST) {
            case 0:
                showSetup(position);
                return true;
            case 1:
                showSplitDialog(position);
                break;
            case 2:
                mAdapter.removeRow(position);
                return true;
        }

        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            putSettings();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            cancel();
        }
    }

    private void updateSettings(boolean forceDefault) {
        int[] lux = null, values = null;

        if (!forceDefault) {
            lux = fetchItems(Settings.System.AUTO_BRIGHTNESS_LUX);
            values = fetchItems(Settings.System.AUTO_BRIGHTNESS_BACKLIGHT);
        }

        if (lux != null && values != null && lux.length != values.length - 1) {
            Log.e(TAG, "Found invalid backlight settings, ignoring");
            values = null;
        }

        if (lux == null || values == null) {
            final Resources res = getContext().getResources();
            lux = res.getIntArray(com.android.internal.R.array.config_autoBrightnessLevels);
            values = res.getIntArray(com.android.internal.R.array.config_autoBrightnessLcdBacklightValues);
            mIsDefault = true;
        } else {
            mIsDefault = false;
        }

        mAdapter.initFromSettings(lux, values);
    }

    private void showHelp() {
        final View v = getLayoutInflater().inflate(R.layout.auto_brightness_help, null);

        final AlertDialog d = new AlertDialog.Builder(getContext())
            .setTitle(R.string.auto_brightness_help_dialog_title)
            .setCancelable(true)
            .setView(v)
            .setNegativeButton(R.string.auto_brightness_close_button, null)
            .create();

        d.show();
    }

    private void showPreview() {
        final int n = mAdapter.getCount();
        float[] x = new float[n];
        float[] y = new float[n];

        for (int i = 0; i < n; i++) {
            SettingRow row = mAdapter.getItem(i);
            x[i] = row.lux;
            //XXX: should mMinLevel be treated as 0 in the preview?
            y[i] = (float) row.backlight / PowerManager.BRIGHTNESS_ON;
        }

        final View v = getLayoutInflater().inflate(R.layout.auto_brightness_preview, null);
        final CubicSplinePreviewView preview =
                (CubicSplinePreviewView) v.findViewById(R.id.brightness_preview);
        preview.setSpline(x, y);

        final AlertDialog d = new AlertDialog.Builder(getContext())
            .setTitle(R.string.auto_brightness_preview_dialog_title)
            .setCancelable(true)
            .setView(v)
            .setNegativeButton(R.string.auto_brightness_close_button, null)
            .create();

        d.show();
    }

    private void showSetup(final int position) {
        final SettingRow row = mAdapter.getItem(position);
        final View v = getLayoutInflater().inflate(R.layout.auto_brightness_level_setup, null);
        final EditText lux = (EditText) v.findViewById(R.id.lux);
        final SeekBar backlightBar = (SeekBar) v.findViewById(R.id.backlight);
        final EditText backlightInput = (EditText) v.findViewById(R.id.backlight_input);
        final DecimalFormat backlightInputFormat = new DecimalFormat("0.0",
                DecimalFormatSymbols.getInstance(Locale.US));

        final AlertDialog d = new AlertDialog.Builder(getContext())
            .setTitle(R.string.auto_brightness_level_dialog_title)
            .setCancelable(true)
            .setView(v)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    try {
                        int newLux = Integer.valueOf(lux.getText().toString());
                        mAdapter.setLuxForRow(position, newLux);
                    } catch (NumberFormatException e) {
                        //ignored
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();

        backlightBar.setMax(brightnessToProgress(PowerManager.BRIGHTNESS_ON));

        lux.setText(String.valueOf(row.lux));
        backlightBar.setProgress(brightnessToProgress(row.backlight));
        backlightInput.setText(backlightInputFormat.format(brightnessToPercent(row.backlight)));

        backlightBar.setOnSeekBarChangeListener(new BrightnessSeekBarChangeListener() {
            @Override
            protected int getPosition(SeekBar seekBar) {
                return position;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                super.onProgressChanged(seekBar, progress, fromUser);

                float brightness = progressToBrightness(seekBar.getProgress());
                backlightInput.setText(backlightInputFormat.format(brightnessToPercent(brightness)));
            }
        });

        backlightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean ok = false;
                try {
                    float minValue = position == 0
                            ? mMinLevel
                            : mAdapter.getItem(position - 1).backlight;
                    float maxValue = mAdapter.isLastItem(position)
                            ? PowerManager.BRIGHTNESS_ON
                            : mAdapter.getItem(position + 1).backlight;
                    float newBrightness = percentToBrightness(Float.valueOf(s.toString()));

                    if (newBrightness >= minValue && newBrightness <= maxValue) {
                        ok = true;
                        backlightBar.setProgress(brightnessToProgress(newBrightness));
                    }
                } catch (NumberFormatException e) {
                    //ignored, ok is false anyway
                }

                Button okButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
                if (okButton != null) {
                    okButton.setEnabled(ok);
                }
            }
        });

        d.show();
    }

    private void showSplitDialog(final int position) {
        final SettingRow row = mAdapter.getItem(position);
        final View v = getLayoutInflater().inflate(R.layout.auto_brightness_split_dialog, null);
        final TextView label = (TextView) v.findViewById(R.id.split_label);
        final EditText value = (EditText) v.findViewById(R.id.split_position);

        final AlertDialog d = new AlertDialog.Builder(getContext())
            .setTitle(R.string.auto_brightness_split_dialog_title)
            .setCancelable(true)
            .setView(v)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    int splitLux = Integer.valueOf(value.getText().toString());
                    mAdapter.splitRow(position, splitLux);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();

        final int minLux = row.lux + 1;
        final int maxLux = mAdapter.isLastItem(position) ? 0 : mAdapter.getItem(position + 1).lux - 1;

        label.setText(getContext().getString(R.string.auto_brightness_split_lux_format,
                minLux, maxLux));
        value.setText(String.valueOf(minLux));
        value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean ok = false;
                try {
                    int newLux = Integer.valueOf(s.toString());
                    ok = newLux >= minLux && newLux <= maxLux;
                } catch (NumberFormatException e) {
                    //ignored, ok is false anyway
                }
                Button okButton = d.getButton(DialogInterface.BUTTON_POSITIVE);
                if (okButton != null) {
                    okButton.setEnabled(ok);
                }
            }
        });

        d.show();
    }

    private void showResetConfirmation() {
        final AlertDialog d = new AlertDialog.Builder(getContext())
            .setTitle(R.string.auto_brightness_reset_dialog_title)
            .setCancelable(true)
            .setMessage(R.string.auto_brightness_reset_confirmation)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    updateSettings(true);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();

        d.show();
    }

    private void putSettings() {
        int[] lux = null, values = null;

        if (!mIsDefault) {
            lux = mAdapter.getLuxValues();
            values = mAdapter.getBacklightValues();
        }

        putItems(Settings.System.AUTO_BRIGHTNESS_LUX, lux);
        putItems(Settings.System.AUTO_BRIGHTNESS_BACKLIGHT, values);
    }

    private int[] fetchItems(String setting) {
        String value = Settings.System.getString(getContext().getContentResolver(), setting);
        if (value != null) {
            String[] values = value.split(",");
            if (values != null && values.length != 0) {
                int[] result = new int[values.length];
                int i;

                for (i = 0; i < values.length; i++) {
                    try {
                        result[i] = Integer.valueOf(values[i]);
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
                if (i == values.length) {
                    return result;
                }
            }
        }

        return null;
    }

    private void putItems(String setting, int[] values) {
        String value = null;
        if (values != null) {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    builder.append(",");
                }
                builder.append(values[i]);
            }
            value = builder.toString();
        }
        Settings.System.putString(getContext().getContentResolver(), setting, value);
    }

    private class SettingRowAdapter extends ArrayAdapter<SettingRow> {
        public SettingRowAdapter(Context context, ArrayList<SettingRow> rows) {
            super(context, 0, rows);
            setNotifyOnChange(false);
        }

        private boolean isLastItem(int position) {
            return position == getCount() - 1;
        }

        public boolean canSplitRow(int position) {
            if (isLastItem(position)) {
                return false;
            }

            SettingRow row = getItem(position);
            SettingRow next = getItem(position + 1);
            return next.lux > (row.lux + 1);
        }

        public void initFromSettings(int[] lux, int[] values) {
            ArrayList<SettingRow> settings = new ArrayList<SettingRow>(values.length);
            for (int i = 0; i < lux.length; i++) {
                settings.add(new SettingRow(i == 0 ? 0 : lux[i - 1], values[i]));
            }
            settings.add(new SettingRow(lux[lux.length - 1], values[values.length - 1]));

            clear();
            addAll(settings);
            notifyDataSetChanged();
        }

        public int[] getLuxValues() {
            int count = getCount();
            int[] lux = new int[count - 1];

            for (int i = 1; i < count; i++) {
                lux[i - 1] = getItem(i).lux;
            }

            return lux;
        }

        public int[] getBacklightValues() {
            int count = getCount();
            int[] values = new int[count];

            for (int i = 0; i < count; i++) {
                values[i] = getItem(i).backlight;
            }
            return values;
        }

        public void splitRow(int position, int splitLux) {
            if (!canSplitRow(position)) {
                return;
            }

            ArrayList<SettingRow> rows = new ArrayList<SettingRow>();
            for (int i = 0; i <= position; i++) {
                rows.add(getItem(i));
            }

            SettingRow lastRow = getItem(position);
            rows.add(new SettingRow(splitLux, lastRow.backlight));

            for (int i = position + 1; i < getCount(); i++) {
                rows.add(getItem(i));
            }

            clear();
            addAll(rows);
            sanitizeValuesAndNotify();
        }

        public void removeRow(int position) {
            if (getCount() <= 1) {
                return;
            }

            remove(getItem(position));
            sanitizeValuesAndNotify();
        }

        public void setLuxForRow(final int position, int newLux) {
            final SettingRow row = getItem(position);

            if (isLastItem(position) || row.lux == newLux) {
                return;
            }

            row.lux = newLux;
            sanitizeValuesAndNotify();
        }

        public void sanitizeValuesAndNotify() {
            final int count = getCount();

            getItem(0).lux = 0;
            for (int i = 1; i < count; i++) {
                SettingRow lastRow = getItem(i - 1);
                SettingRow thisRow = getItem(i);

                thisRow.lux = Math.max(lastRow.lux + 1, thisRow.lux);
                thisRow.backlight = Math.max(lastRow.backlight, thisRow.backlight);
            }

            mIsDefault = false;
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Holder holder;

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        R.layout.auto_brightness_list_item, parent, false);
                holder = new Holder();
                holder.lux = (TextView) convertView.findViewById(R.id.lux);
                holder.backlight = (SeekBar) convertView.findViewById(R.id.backlight);
                holder.percent = (TextView) convertView.findViewById(R.id.backlight_percent);
                convertView.setTag(holder);

                holder.backlight.setMax(brightnessToProgress(PowerManager.BRIGHTNESS_ON));
                holder.backlight.setOnSeekBarChangeListener(new BrightnessSeekBarChangeListener() {
                    @Override
                    protected int getPosition(SeekBar seekBar) {
                        return (Integer) seekBar.getTag();
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        super.onProgressChanged(seekBar, progress, fromUser);

                        if (fromUser) {
                            int pos = getPosition(seekBar);
                            progress = seekBar.getProgress();
                            getItem(pos).backlight = Math.round(progressToBrightness(progress));
                            mIsDefault = false;
                        }

                        holder.updatePercent();
                    }
                });
            } else {
                holder = (Holder) convertView.getTag();
            }

            SettingRow row = (SettingRow) getItem(position);

            final int resId = isLastItem(position)
                    ? R.string.auto_brightness_last_level_format
                    : R.string.auto_brightness_level_format;

            holder.lux.setText(getContext().getString(resId, row.lux));

            holder.backlight.setTag(position);
            holder.backlight.setProgress(brightnessToProgress(row.backlight));
            holder.updatePercent();

            return convertView;
        }

        private class Holder {
            TextView lux;
            SeekBar backlight;
            TextView percent;

            public void updatePercent() {
                int percentValue = Math.round((float) backlight.getProgress() * 100F / backlight.getMax());
                percent.setText(getContext().getString(
                        R.string.auto_brightness_brightness_format, percentValue));
            }
        };
    };

    private float brightnessToPercent(float brightness) {
        brightness -= mMinLevel;
        return brightness * 100F / (PowerManager.BRIGHTNESS_ON - mMinLevel);
    }

    private float percentToBrightness(float percent) {
        float brightness = percent * (PowerManager.BRIGHTNESS_ON - mMinLevel) / 100F;
        return brightness + mMinLevel;
    }

    private int brightnessToProgress(float brightness) {
        brightness -= mMinLevel;
        return (int) (brightness * 100F);
    }

    private float progressToBrightness(int progress) {
        float brightness = (float) progress / 100F;
        return brightness + mMinLevel;
    }

    private abstract class BrightnessSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private boolean mIsDragging = false;

        private void updateBrightness(float brightness) {
            final Window window = getWindow();
            final WindowManager.LayoutParams params = window.getAttributes();
            params.screenBrightness = brightness;
            window.setAttributes(params);
        }

        protected abstract int getPosition(SeekBar seekBar);

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int pos = getPosition(seekBar);
            if (fromUser) {
                int minValue = pos == 0
                        ? 0
                        : brightnessToProgress(mAdapter.getItem(pos - 1).backlight);
                int maxValue = mAdapter.isLastItem(pos)
                        ? seekBar.getMax()
                        : brightnessToProgress(mAdapter.getItem(pos + 1).backlight);

                if (progress < minValue) {
                    seekBar.setProgress(minValue);
                    progress = minValue;
                } else if (progress > maxValue) {
                    seekBar.setProgress(maxValue);
                    progress = maxValue;
                }
            }

            if (mIsDragging) {
                float brightness = progressToBrightness(progress);
                updateBrightness(brightness / PowerManager.BRIGHTNESS_ON);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            float brightness = progressToBrightness(seekBar.getProgress());
            updateBrightness(brightness / PowerManager.BRIGHTNESS_ON);
            mIsDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            updateBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
            mIsDragging = false;
        }
    };
}

package com.android.settings.applications;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.settings.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class LockPatternActivity extends Activity {
    public static final String PATTERN_LOCK_PROTECTED_APPS = "pattern_lock_protected_apps";
    public static final String RECREATE_PATTERN = "recreate_pattern_lock";

    private static final int MIN_PATTERN_SIZE = 4;
    private static final int MAX_PATTERN_RETRY = 5;
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;

    LockPatternView mLockPatternView;

    TextView mPatternLockHeader;
    Button mCancel;
    Button mContinue;
    byte[] mPatternHash;

    int mRetry = 0;

    boolean mCreate;
    boolean mConfirming = false;

    Runnable mCancelPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
            mContinue.setEnabled(false);

            if (mCreate) {
                if (mConfirming) {
                    mPatternLockHeader.setText(getResources()
                            .getString(R.string.lockpattern_need_to_confirm));
                } else {
                    mPatternLockHeader.setText(getResources()
                            .getString(R.string.lockpattern_recording_intro_header));
                }
            } else {
                mPatternLockHeader.setText(getResources()
                        .getString(R.string.lockpattern_settings_enable_summary));
            }
        }
    };

    View.OnClickListener mCancelOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        }
    };

    View.OnClickListener mContinueOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            if (mConfirming) {
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PATTERN_LOCK_PROTECTED_APPS,
                        Base64.encodeToString(mPatternHash, Base64.DEFAULT));
                editor.commit();
                setResult(RESULT_OK);
                finish();
            } else {
                mConfirming = true;
                mLockPatternView.clearPattern();

                mPatternLockHeader.setText(getResources().getString(
                        R.string.lockpattern_need_to_confirm));
                btn.setText(getResources().getString(R.string.lockpattern_confirm_button_text));
                btn.setEnabled(false);
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.patternlock);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String pattern = prefs.getString(PATTERN_LOCK_PROTECTED_APPS, null);
        mCreate = pattern == null;
        if (RECREATE_PATTERN.equals(getIntent().getAction())) {
            mCreate = true;
        }

        if (pattern != null) {
            mPatternHash = Base64.decode(pattern, Base64.DEFAULT);
        }

        mPatternLockHeader = (TextView) findViewById(R.id.pattern_lock_header);
        mCancel = (Button) findViewById(R.id.pattern_lock_btn_cancel);
        mCancel.setOnClickListener(mCancelOnClickListener);
        mContinue = (Button) findViewById(R.id.pattern_lock_btn_continue);
        mContinue.setOnClickListener(mContinueOnClickListener);
        if (mCreate) {
            mContinue.setEnabled(false);
            mPatternLockHeader.setText(getResources().getString(R.string.lockpattern_recording_intro_header));
        } else {
            mCancel.setVisibility(View.GONE);
            mContinue.setVisibility(View.GONE);
            mPatternLockHeader.setText(getResources().getString(R.string.lockpattern_settings_enable_summary));
        }

        mLockPatternView = (LockPatternView) findViewById(R.id.lock_pattern_view);

        //Setup Pattern Lock View
        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setFocusable(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());

    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {

        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);

            mPatternLockHeader.setText(getResources().getText(
                    R.string.lockpattern_recording_inprogress));
            mContinue.setEnabled(false);
        }

        public void onPatternCleared() {
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
            //Check inserted Pattern
            if (mCreate) {
                if (pattern.size() < MIN_PATTERN_SIZE) {
                    mPatternLockHeader.setText(getResources().getString(
                            R.string.lockpattern_recording_incorrect_too_short,
                            LockPatternUtils.MIN_LOCK_PATTERN_SIZE));

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
                    return;
                }

                if (mConfirming) {
                    if (Arrays.equals(mPatternHash, patternToHash(pattern))) {
                        mContinue.setText(getResources()
                                .getString(R.string.lockpattern_confirm_button_text));
                        mContinue.setEnabled(true);
                        mPatternLockHeader.setText(getResources().getString(
                                R.string.lockpattern_pattern_confirmed_header));
                    } else {
                        mContinue.setEnabled(false);

                        mPatternLockHeader.setText(getResources().getString(
                                R.string.lockpattern_need_to_unlock_wrong));
                        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mCancelPatternRunnable,
                                PATTERN_CLEAR_TIMEOUT_MS);
                    }
                } else {
                    //Save pattern, user needs to redraw to confirm
                    mPatternHash = patternToHash(pattern);

                    mPatternLockHeader.setText(getResources().getString(
                            R.string.lockpattern_pattern_entered_header));
                    mContinue.setEnabled(true);
                }
            } else {
                //Check against existing pattern
                if (Arrays.equals(mPatternHash, patternToHash(pattern))) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    mRetry++;
                    mPatternLockHeader.setText(getResources().getString(
                            R.string.lockpattern_need_to_unlock_wrong));

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);

                    if (mRetry >= MAX_PATTERN_RETRY) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(
                                        R.string.lockpattern_too_many_failed_confirmation_attempts_header),
                                Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            }
        }

        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {}
    }

    /*
     * Generate an SHA-1 hash for the pattern. Not the most secure, but it is
     * at least a second level of protection. First level is that the file
     * is in a location only readable by the system process.
     * @param pattern the gesture pattern.
     * @return the hash of the pattern in a byte array.
     */
    public byte[] patternToHash(List<LockPatternView.Cell> pattern) {
        if (pattern == null) {
            return null;
        }

        final int patternSize = pattern.size();
        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            LockPatternView.Cell cell = pattern.get(i);
            res[i] = (byte) (cell.getRow() * 3 + cell.getColumn());
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(res);
            return hash;
        } catch (NoSuchAlgorithmException nsa) {
            return res;
        }
    }
}

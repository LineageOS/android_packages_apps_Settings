/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import com.android.internal.widget.LockPatternUtils;

import android.app.Activity;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Window;

import android.util.Log;
import android.os.Handler;

import com.authentec.AuthentecHelper;

public class ConfirmLockFinger extends Activity {

    private static final boolean DEBUG = true;
    private static final String TAG = "ConfirmLockFinger";
    private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";

    private LockPatternUtils mLockPatternUtils;
    private int mNumWrongConfirmAttempts;
    private CountDownTimer mCountdownTimer;

    private TextView mHeaderTextView;
    private TextView mFooterTextView;

    private static Thread mExecutionThread = null;
    private VerifyRunner mVerifyRunner = new VerifyRunner();
    // The confirm screen would be put into lockout state every four bad swipes.
    private static boolean sAttemptLockout;
    private ConfirmLockFinger mConfirmLockFinger;
    private AuthentecHelper fingerhelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't show the tutorial if the user has seen it before.
        mLockPatternUtils = new LockPatternUtils(this);
        mConfirmLockFinger = this;

        fingerhelper = AuthentecHelper.getInstance(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.confirm_lock_finger);

        mHeaderTextView = (TextView) findViewById(R.id.headerText);
        mFooterTextView = (TextView) findViewById(R.id.footerText);

        if (savedInstanceState != null) {
            mNumWrongConfirmAttempts = savedInstanceState.getInt(KEY_NUM_WRONG_ATTEMPTS);
        }

        // Initialize the verify runner.
        mExecutionThread = null;
        sAttemptLockout = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // deliberately not calling super since we are managing this in full
        outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (DEBUG) Log.d(TAG, "onPause");

        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (DEBUG) Log.d(TAG, "onResume");

        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline();
        if (deadline != 0) {
            if (DEBUG) Log.d(TAG,"onResume: In lockout state");
            handleAttemptLockout(deadline);
        } else {
            // The onFinish() method of the CountDownTimer object would not be
            // called when the screen is off. So we need to reset the sAttemptLockout
            // if the lockout has finished.
            sAttemptLockout = false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            if (DEBUG) Log.d(TAG,"onWindowFocusChanged: has window focus");

            if (mExecutionThread == null) {
                if (DEBUG) Log.d(TAG,"onWindowFocusChanged: start verify runner from scratch");
                mExecutionThread = new Thread(mVerifyRunner);
                if (!sAttemptLockout) {
                    mExecutionThread.start();
                }
            } else {
                if ((!sAttemptLockout) && !(mExecutionThread.isAlive())) {
                    if (DEBUG) Log.d(TAG,"onWindowFocusChanged: restart verify runner");
                    if (mExecutionThread.getState() == Thread.State.TERMINATED) {
                        // If the thread state is TERMINATED, it cannot be start() again.
                        // Create a thread for verification.
                        mExecutionThread = new Thread(mVerifyRunner);
                    }
                    mExecutionThread.start();
                }
            }
        }
    }

    /**
      * Provides a Runnable class to handle the finger
      * verification
      */
    private class VerifyRunner implements Runnable {
        public void run() {

            // Clear header and footer text for a new verification.
            runOnUiThread(new Runnable() {
                public void run() {
                    mHeaderTextView.setText("");
                    mFooterTextView.setText("");
                }
            });

            try {
                // Launch the LAP verify dialog. If user is authenticated, then finish with
                // success. Otherwise, finish with cancel.

                int iResult = 0;
                try {
                    iResult = fingerhelper.fingerprintUnlock("lap-verify", ConfirmLockFinger.this);
                } catch (Exception e) {
                    iResult = AuthentecHelper.eAM_STATUS_LIBRARY_NOT_AVAILABLE;
                }

                switch (iResult) {
                    case AuthentecHelper.eAM_STATUS_OK:
                        Log.i(TAG, "LAP Verify OK");
                        setResult(RESULT_OK);
                        finish();
                        break;

                    case AuthentecHelper.eAM_STATUS_NO_STORED_CREDENTIAL:
                        // Should never happen. But if that is the case, we return OK to
                        // let the confirmation pass.
                        Log.e(TAG, "No stored credential, returning OK");
                        setResult(RESULT_OK);
                        finish();
                        break;

                    case AuthentecHelper.eAM_STATUS_LIBRARY_NOT_AVAILABLE:
                        // Failed because library not available.
                        Log.e(TAG, "Library failed to load... cannot proceed!");
                        setResult(RESULT_CANCELED);
                        finish();
                        break;

                    case AuthentecHelper.eAM_STATUS_USER_CANCELED:
                        // Failed because user canceled.
                        Log.e(TAG, "Simulating device lock.\nYou may not cancel!");
                        setResult(RESULT_CANCELED);
                        finish();
                        break;

                    case AuthentecHelper.eAM_STATUS_UI_TIMEOUT:
                        // UI timeout, lockout for sometime.
                        Log.e(TAG, "UI timeout!");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                mHeaderTextView.setText(R.string.lockfinger_ui_timeout_header);
                                long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                                handleAttemptLockout(deadline);
                            }
                        });
                        break;

                    case AuthentecHelper.eAM_STATUS_CREDENTIAL_LOCKED:
                        // Failed to verify for 4 times, lockout for sometime.
                        Log.e(TAG, "Credential locked!");
                        runOnUiThread(new Runnable() {
                             public void run() {
                                mHeaderTextView.setText(R.string.lockfinger_too_many_bad_swipes_header);
                                long deadline = mLockPatternUtils.setLockoutAttemptDeadline();
                                handleAttemptLockout(deadline);
                             }
                        });
                        break;

                    default:
                        Log.e(TAG, "Other results: " + iResult);
                        setResult(RESULT_CANCELED);
                        finish();
                }
            } catch (Exception e) {e.printStackTrace();}

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        // Indicate the lockout state.
        sAttemptLockout = true;

        long elapsedRealtime = SystemClock.elapsedRealtime();
        mCountdownTimer = new CountDownTimer(
                elapsedRealtimeDeadline - elapsedRealtime,
                LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsCountdown = (int) (millisUntilFinished / 1000);
                mFooterTextView.setText(getString(
                        R.string.lockfinger_lockout_countdown_footer,
                        secondsCountdown));
            }

            @Override
            public void onFinish() {
                mNumWrongConfirmAttempts = 0;
                if (DEBUG) Log.d(TAG, "handleAttemptLockout: onFinish");
                sAttemptLockout = false;

                // Start the verification if the confirm screen has the window focus.
                if (hasWindowFocus()) {
                    if (mExecutionThread != null && !(mExecutionThread.isAlive())) {
                        if (DEBUG) Log.d(TAG,"Lockout onFinish: restart verify runner");
                        if (mExecutionThread.getState() == Thread.State.TERMINATED)
                        {
                            // If the thread state is TERMINATED, it cannot be start() again.
                            // Create a thread for verification.
			                mExecutionThread = new Thread(mVerifyRunner);
                            mExecutionThread.start();
                        }
                        else
                        {
                            mExecutionThread.start();
                        }
                    }
                }
            }
        }.start();
    }
}

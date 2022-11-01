package com.android.settings.biometrics.face;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.fragment.app.FragmentActivity;
import androidx.window.R;
import com.android.settings.biometrics.face.FaceEnrollEnrolling;
public class FaceEnrollActivityDirector extends FragmentActivity {
    private Intent mExtras;
    private boolean mFirstTime = true;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mExtras = getIntent();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 4) {
            Intent intent2 = new Intent(mExtras);
            intent2.putExtra("accessibility_diversity", false);
            intent2.putExtra("from_multi_timeout", true);
            startEnrollActivity(intent2);
        } else if (resultCode == 5) {
            startEnrollActivity(mExtras);
        } else {
            setResult(resultCode, data);
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mFirstTime) {
            mFirstTime = false;
            startEnrollActivity(mExtras);
        }
    }
    private void startEnrollActivity(Intent intent) {
        Intent enrollIntent;
        boolean trafficLight = getResources().getBoolean(R.bool.config_face_enroll_use_traffic_light);
        // Use SettingsGoogleFutureFaceEnroll
        if (trafficLight) {
            enrollIntent = new Intent("com.google.android.settings.future.biometrics.faceenroll.action.ENROLL");
        } else {
            enrollIntent = new Intent(this, FaceEnrollEnrolling.class);
        }
        if (trafficLight) {
            String packageName = getString(R.string.config_face_enroll_traffic_light_package);
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalStateException("Package name must not be empty");
            }
            enrollIntent.setPackage(packageName);
        }
        enrollIntent.putExtras(intent);
        startActivityForResult(enrollIntent, 1);
    }
}


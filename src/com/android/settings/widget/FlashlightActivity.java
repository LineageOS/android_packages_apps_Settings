
package com.android.settings.widget;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

public class FlashlightActivity extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setBackgroundColor(Color.WHITE);
        setContentView(linearLayout);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final Window window = getWindow();
        LayoutParams attrs = window.getAttributes();
        attrs.screenBrightness = 1;
        window.setAttributes(attrs);
    }

}

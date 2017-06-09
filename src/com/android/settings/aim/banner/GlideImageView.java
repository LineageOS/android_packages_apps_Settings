/*
 * Copyright (C) 2017 AIM ROM
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

package com.android.settings.aim.banner;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.content.Context;
import android.util.AttributeSet;
import com.android.settings.R;

public class GlideImageView extends ImageView
{

    /**
     * @param context
     */
    public GlideImageView(Context context)
    {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public GlideImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public GlideImageView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
	Glide.with(this.getContext()).load(R.raw.banner).asGif().crossFade().into(this);
    }
}

/*
 * Copyright (C) 2010 Florian Sundermann
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

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

public class SubItem {
        private String fName;
        private Drawable fImage;
        public Bundle fExtra = null;
        public ComponentName fProvider = null;

        public SubItem(String name, Drawable image) {
            fName = name;
            fImage = image;
        }

        public void setExtra(Bundle aValue) {
            fExtra = aValue;
        }
        public Bundle getExtra() {
            return fExtra;
        }

        public void setProvider(ComponentName aValue) {
            fProvider = aValue;
        }
        public ComponentName getProvider() {
            return fProvider;
        }

        public String getName() {
            return fName;
        }

        public Drawable getImage() {
            return fImage;
        }
}

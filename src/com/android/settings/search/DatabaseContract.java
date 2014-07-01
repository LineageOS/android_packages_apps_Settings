/*
 * Copyright (C) 2014 The CyanogenMod Project
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

package com.android.settings.search;

import android.provider.BaseColumns;

public final class DatabaseContract {

    public static final String AUTHORITY =
            "com.android.settings.SettingsSearchRecentSuggestionsProvider";

    public static final String TABLE_NAME = "settings";

    private DatabaseContract() {}

    public static class Settings implements BaseColumns {
        public static String ACTION_TITLE = "title";

        public static String ACTION_HEADER = "header";

        public static String ACTION_ICON = "icon";

        public static String ACTION_LEVEL = "level";

        public static String ACTION_FRAGMENT = "fragment";

        public static String ACTION_PARENT_TITLE = "parent_title";

        public static String ACTION_KEY = "key";
    }
}

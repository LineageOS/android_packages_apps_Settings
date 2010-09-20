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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.graphics.drawable.Drawable;

public class Item extends SubItem {
    private final ArrayList<SubItem> fItems = new ArrayList<SubItem>();
    private String fPackageName;

    public Item(String name, Drawable image) {
        super(name, image);
    }

    public ArrayList<SubItem> getItems() {
        return fItems;
    }

    @Override
    public String getName() {
        if (fItems.size() == 1)
            return fItems.get(0).getName();
        else
            return super.getName();
    }

    @Override
    public Drawable getImage() {
        if (fItems.size() == 1)
            return fItems.get(0).getImage();
        else
            return super.getImage();
    }

    public void setPackageName(String aValue) {
        fPackageName = aValue;
    }

    public String getPackageName() {
        return fPackageName;
    }

    public void sort() {
        Collections.sort(fItems, new Comparator<SubItem>() {

            @Override
            public int compare(SubItem object1, SubItem object2) {
                return object1.getName().compareToIgnoreCase(object2.getName());
            }

        });
    }
}

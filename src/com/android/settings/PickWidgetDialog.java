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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

public class PickWidgetDialog {

    AlertDialog fDialog;
    private final AppWidgetPickActivity fOwner;

    private class ClickListener implements DialogInterface.OnClickListener {

        public ClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            SubItem subItem = PickWidgetDialog.this.fItemAdapter.getItem(which);
            PickWidgetDialog.this.fDialog.dismiss();

            PickWidgetDialog.this.showDialog(subItem);
        }

    }

    private class CancelListener implements OnCancelListener {
        private final boolean fCancelOwner;

        public CancelListener(boolean cancelOwner) {
            fCancelOwner = cancelOwner;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (fCancelOwner) {
                PickWidgetDialog.this.fOwner.setResult(AppWidgetPickActivity.RESULT_CANCELED);
                PickWidgetDialog.this.fOwner.finish();
            } else {
                PickWidgetDialog.this.showDialog(null);
            }
        }
    }



    public PickWidgetDialog(AppWidgetPickActivity owner) {
        fOwner = owner;
    }

    ItemAdapter fItemAdapter;

    public void showDialog(SubItem subItem) {
        if (subItem == null || subItem instanceof Item) {
            AlertDialog.Builder ab = new AlertDialog.Builder(fOwner);

            if (subItem == null) {
                ab.setTitle(fOwner.getString(R.string.widget_picker_title));
                fItemAdapter = new ItemAdapter(fOwner, 0, fOwner.getItems());
                ab.setAdapter(fItemAdapter, new ClickListener());
            }
            else {
                Item itm = (Item)subItem;
                if (itm.getItems().size() == 1) {
                    fOwner.finishOk(itm.getItems().get(0));
                    return;
                }

                ab.setTitle(subItem.getName());
                fItemAdapter = new ItemAdapter(fOwner, 0, itm.getItems());
                ab.setAdapter(fItemAdapter, new ClickListener());
            }

            ab.setOnCancelListener(new CancelListener(subItem == null));
            fDialog =  ab.create();
            fDialog.show();
        }
        else
            fOwner.finishOk(subItem);
    }
}

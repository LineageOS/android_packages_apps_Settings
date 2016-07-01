/*
 *     Copyright (C) 2016 The CyanogenMod Project
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.android.settings;

import android.os.Bundle;
import android.app.Activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class CmRadioPropDisplay extends Activity {
    static final String PROPERTY_LIST_FILE = "persist.dbg.property_list_file";
    private static final String TAG = "CmRadioPropDisplay";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radio_properties_display);
        TextView output_1 = (TextView) findViewById(R.id.output_01);
        TextView output_2 = (TextView) findViewById(R.id.output_02);
        try {
            XmlPullParserFactory Object = XmlPullParserFactory.newInstance();
            XmlPullParser Parser = Object.newPullParser();
            try{
                String mssg=SystemProperties.get(PROPERTY_LIST_FILE, "NA");
                File myfile=new File(mssg);
                FileInputStream in_s = new FileInputStream(myfile);
                output_1.setText(PROPERTY_LIST_FILE+": "+mssg);
                Parser.setInput(in_s, null);
            }
            catch(FileNotFoundException e){
                try{
                    InputStream in_s = this.getAssets().open("radioproperties.xml");
                    Parser.setInput(in_s, null);
                }
                catch(FileNotFoundException e1){
                    Log.d(TAG, "FileNotFoundException: specified xml file not found either in device or in apk.");
                    output_1.setText(getString(R.string.cm_radio_info_xml_not_found_label));
                }
            }
            int event = Parser.getEventType();
            StringBuilder sb = new StringBuilder();
            while (event != XmlPullParser.END_DOCUMENT)
            {   switch (event){
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        String mssg1=SystemProperties.get(Parser.getText(), "NA");
                        sb.append(Parser.getText()+": "+mssg1);
                        sb.append(System.getProperty("line.separator"));
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                event = Parser.next();
            }
            output_2.setText(sb);
        }
        catch(XmlPullParserException e){
            e.printStackTrace();
            Log.d(TAG, "XmlPullParserException: error parsing the xml file.");
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(NullPointerException e){
            e.printStackTrace();
        }
    }
}
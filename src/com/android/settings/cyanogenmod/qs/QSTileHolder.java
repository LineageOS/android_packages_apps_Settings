package com.android.settings.cyanogenmod.qs;

/**
 * Created by Arasthel on 01/01/15.
 *
 * This class holds the icon, the name - or the string the user sees, and the value which will be stored
 */
public class QSTileHolder {

    private int drawableId;
    private String value;
    private String name;

    public QSTileHolder(int drawableId, String value, String name) {
        this.drawableId = drawableId;
        this.value = value;
        this.name = name;
    }

    public int getDrawableId() {
        return drawableId;
    }

    public void setDrawableId(int drawableId) {
        this.drawableId = drawableId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

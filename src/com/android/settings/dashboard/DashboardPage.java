package com.android.settings.dashboard;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardPage implements Parcelable {

    public int pageIndex;

    public int dashboardRes;

    public int iconRes;

    /**
     * Resource ID of title of the page that is shown to the user.
     */
    public int titleRes;

    /**
     * Title of the page that is shown to the user.
     */
    public CharSequence title;

    public List<DashboardCategory> categories = new ArrayList<DashboardCategory>();

    public DashboardPage() {

    }

    public DashboardPage(int iconRes, CharSequence title, int pageIndex, int dashboardRes) {
        this.iconRes = iconRes;
        this.title = title;
        this.pageIndex = pageIndex;
        this.dashboardRes = dashboardRes;
    }

    public CharSequence getTitle(Resources res) {
        if (titleRes != 0) {
            return res.getText(titleRes);
        }
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pageIndex);
        dest.writeInt(dashboardRes);
        dest.writeInt(iconRes);
        dest.writeInt(titleRes);
        TextUtils.writeToParcel(title, dest, flags);

        final int count = categories.size();
        dest.writeInt(count);

        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);
            category.writeToParcel(dest, flags);
        }
    }

    public void readFromParcel(Parcel in) {
        pageIndex = in.readInt();
        dashboardRes = in.readInt();
        iconRes = in.readInt();
        titleRes = in.readInt();
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);

        final int count = in.readInt();

        for (int n = 0; n < count; n++) {
            DashboardCategory category = DashboardCategory.CREATOR.createFromParcel(in);
            categories.add(category);
        }
    }

    DashboardPage(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<DashboardPage> CREATOR = new Creator<DashboardPage>() {
        @Override
        public DashboardPage createFromParcel(Parcel in) {
            return new DashboardPage(in);
        }

        @Override
        public DashboardPage[] newArray(int size) {
            return new DashboardPage[size];
        }
    };

}

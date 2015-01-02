package com.android.settings.dashboard;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;

public class DashboardPages extends Fragment {

    ViewPager mPager;

    public static DashboardPage[] DASHBOARD_PAGES = {
            new DashboardPage(R.drawable.ic_tab_wireless, "Wireless", 0, R.xml.dashboard_recents),
            new DashboardPage(R.drawable.ic_apps_tab, "Device", 1, R.xml.dashboard_device),
            new DashboardPage(R.drawable.ic_device_tab, "System", 2, R.xml.dashboard_apps)
    };
    private LeAdapter mAdapter;

    public class LeAdapter extends FragmentPagerAdapter
            implements PagerSlidingTabStrip.IconTabProvider {

        public LeAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return DASHBOARD_PAGES.length;
        }

        @Override
        public Fragment getItem(int position) {
            return DashboardSummary.newInstance(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return DASHBOARD_PAGES[position].getTitle(getActivity().getResources());
        }

        @Override
        public int getPageIconResId(int position) {
            return DASHBOARD_PAGES[position].iconRes;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dashboard_pages, container, false);
        mPager = (ViewPager) rootView.findViewById(R.id.pager);


        mAdapter = new LeAdapter(getChildFragmentManager());
        mPager.setAdapter(mAdapter);
        if (!ActivityManager.isLowRamDeviceStatic()) {
            mPager.setOffscreenPageLimit(mAdapter.getCount() - 1);
        }

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setViewPager(mPager);
//        tabs.setOutlineProvider(new ViewOutlineProvider() {
//            @Override
//            public void getOutline(View view, Outline outline) {
//                outline.setRect(view.getLeft(), view.getTop() + 10, view.getRight(), view.getBottom());
//                outline.setAlpha(1.f);
//            }
//        });

        return rootView;
    }
}

package com.android.settings.applications;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.IntProperty;
import android.util.Property;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.WindowManagerPolicyControl;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.SpinnerBar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandedDesktopPreferenceFragment extends SettingsPreferenceFragment
        implements AdapterView.OnItemClickListener, ApplicationsState.Callbacks,
        AbsListView.MultiChoiceModeListener, SpinnerBar.OnSpinnerItemSelectedListener {
    private static final int STATE_DISABLED = 0;
    private static final int STATE_STATUS_HIDDEN = 1;
    private static final int STATE_NAVIGATION_HIDDEN = 2;
    private static final int STATE_BOTH_HIDDEN = 3;

    private static final int STATE_DISABLE_FOR_ALL = 0;
    private static final int STATE_ENABLE_FOR_ALL = 1;
    private static final int STATE_USER_CONFIGURABLE = 2;

    private static final String USER_APPS = "user_apps";
    private static final String ALL_APPS_VISIBLE = "all_apps_visible";

    private AllPackagesAdapter mAllPackagesAdapter;
    private ApplicationsState mApplicationsState;
    private UserPackagesAdapter mUserPackagesAdapter;
    private View mProgressBar;
    private View mEmptyView;
    private ListView mUserListView;
    private ListView mAllListView;
    private View mAddAll;
    private ApplicationsState.Session mSession;
    private ActivityFilter mActivityFilter;
    private Map<String, ApplicationsState.AppEntry> mEntryMap =
            new HashMap<String, ApplicationsState.AppEntry>();
    private ActionMode mActionMode;
    private Interpolator mInterpolator;
    private int mExpandedDesktopState;
    private boolean mNeedCallRebuild;
    private SpinnerBar mSpinnerBar;


    private View.OnClickListener mAddCloseButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mAllListView.getVisibility() == View.VISIBLE) {
                hideAllApps();
            } else {
                showAllApps();
            }
        }
    };

    private AdapterView.OnItemClickListener mAllAppsOnClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mAllListView.setItemChecked(position, true);
        }
    };

    private LayoutTransition.TransitionListener mTransitionListener = new LayoutTransition.TransitionListener() {
        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view == mAllListView && (transitionType == LayoutTransition.DISAPPEARING
                    || transitionType == LayoutTransition.CHANGE_DISAPPEARING) && mNeedCallRebuild) {
                mNeedCallRebuild = false;
                rebuild();
            }
        }
    };
    private View.OnClickListener mCloseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) v.getTag();

            mUserPackagesAdapter.entries.remove(entry.info.packageName);
            mUserPackagesAdapter.notifyDataSetChanged();

            WindowManagerPolicyControl.removeFromWhiteLists(entry.info.packageName);
            WindowManagerPolicyControl.saveToSettings(getActivity(), Settings.Global.POLICY_CONTROL_SELECTED);
            if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
                WindowManagerPolicyControl.saveToSettings(getActivity());
            }

            rebuild();
        }
    };

    private void showAddAll() {
        if (mExpandedDesktopState != STATE_USER_CONFIGURABLE) {
            return;
        }
        mAddAll.setVisibility(View.VISIBLE);
    }

    private void hideAddAll() {
        mAddAll.setVisibility(View.GONE);
    }

    private void hideSpinnerBar() {
        if (mSpinnerBar != null) {
            mSpinnerBar.hide();
        }
    }

    private void showSpinnerBar() {
        if (mSpinnerBar != null) {
            mSpinnerBar.show();
        }
    }

    private void hideAllApps() {
        mAllListView.clearChoices();
        mAllListView.setVisibility(View.INVISIBLE);
        showAddAll();
        showSpinnerBar();
        mActionMode.finish();
    }

    private void showAllApps() {
        hideSpinnerBar();
        mAllListView.setVisibility(View.VISIBLE);
        mAllListView.startLayoutAnimation();
        hideAddAll();
        mActionMode = mAllListView.startActionMode(this);
    }

    private int getExpandedDesktopState(ContentResolver cr) {
        String value = Settings.Global.getString(cr, Settings.Global.POLICY_CONTROL);
        if (TextUtils.isEmpty(value)) {
            return STATE_DISABLE_FOR_ALL;
        }
        if ("immersive.full=*".equals(value)) {
            return STATE_ENABLE_FOR_ALL;
        }
        return STATE_USER_CONFIGURABLE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mApplicationsState.newSession(this);
        mSession.resume();
        mActivityFilter = new ActivityFilter(getActivity().getPackageManager());

        mAllPackagesAdapter = new AllPackagesAdapter(getActivity());
        mUserPackagesAdapter = new UserPackagesAdapter(getActivity());
        WindowManagerPolicyControl.reloadFromSetting(getActivity(),
                Settings.Global.POLICY_CONTROL_SELECTED);

        if (savedInstanceState == null || !savedInstanceState.containsKey(USER_APPS)) {
            mUserPackagesAdapter.entries.addAll(WindowManagerPolicyControl.getWhiteLists());
        } else {
            mUserPackagesAdapter.entries.addAll(savedInstanceState.getStringArrayList(USER_APPS));
        }
        mUserPackagesAdapter.notifyDataSetChanged();

        mInterpolator = AnimationUtils.loadInterpolator(getActivity(),
                android.R.interpolator.fast_out_slow_in);

        mExpandedDesktopState = getExpandedDesktopState(getActivity().getContentResolver());

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        rebuild();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mUserPackagesAdapter.entries != null && !mUserPackagesAdapter.entries.isEmpty()) {
            outState.putStringArrayList(USER_APPS, mUserPackagesAdapter.entries);
        }
        outState.putBoolean(ALL_APPS_VISIBLE, mAllListView.getVisibility() == View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.expanded_desktop, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.pause();
        mSession.release();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUserListView = (ListView) view.findViewById(R.id.user_list_view);
        mUserListView.setAdapter(mUserPackagesAdapter);
        mUserListView.setOnItemClickListener(this);

        mAllListView = (ListView) view.findViewById(R.id.all_list_view);
        mAllListView.setAdapter(mAllPackagesAdapter);
        mAllListView.setOnItemClickListener(mAllAppsOnClickListener);

        mSpinnerBar = ((SettingsActivity) getActivity()).getSpinnerBar();
        mSpinnerBar.show();
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.expanded_desktop_states,
                R.layout.spinner_simple_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerBar.setAdapter(arrayAdapter);
        mSpinnerBar.addOnItemSelectedListener(this);
        mSpinnerBar.setTextViewLabel(getString(R.string.expanded_desktop_state));
        mSpinnerBar.setSpinnerPosition(mExpandedDesktopState);

        mAddAll = view.findViewById(R.id.floating_action_button);
        mAddAll.setOnClickListener(mAddCloseButtonClickListener);

        if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
            showAddAll();
        }

        mEmptyView = view.findViewById(R.id.nothing_to_show);
        mProgressBar = view.findViewById(R.id.progress_bar);

        ((ViewGroup) view).getLayoutTransition().setStartDelay(LayoutTransition.APPEARING, 0);
        ((ViewGroup) view).getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);

                Animation a = new TranslateAnimation(0, 0, view.getMeasuredHeight(), 0);
                a.setDuration(300);
                a.setInterpolator(mInterpolator);
                LayoutAnimationController lac = new LayoutAnimationController(a, 0.2f);
                mAllListView.setLayoutAnimation(lac);
                return true;
            }
        });

        ((ViewGroup) view).getLayoutTransition().addTransitionListener(mTransitionListener);

        if (savedInstanceState != null && savedInstanceState.getBoolean(ALL_APPS_VISIBLE, false)) {
            showAllApps();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.mode.performClick();
    }

    private void enableForAll() {
        mExpandedDesktopState = STATE_ENABLE_FOR_ALL;
        mUserPackagesAdapter.notifyDataSetInvalidated();
        writeValue("immersive.full=*");
        hideAddAll();
    }

    private void disableForAll() {
        mExpandedDesktopState = STATE_DISABLE_FOR_ALL;
        mUserPackagesAdapter.notifyDataSetInvalidated();
        writeValue("");
        mAllPackagesAdapter.notifyDataSetInvalidated();
        hideAddAll();
    }

    private void userConfigurableSettings() {
        mExpandedDesktopState = STATE_USER_CONFIGURABLE;
        mUserPackagesAdapter.notifyDataSetInvalidated();
        showAddAll();
        WindowManagerPolicyControl.saveToSettings(getActivity());
    }

    private void writeValue(String value) {
        Settings.Global.putString(getContentResolver(), Settings.Global.POLICY_CONTROL, value);
    }

    private static int getStateForPackage(String packageName) {
        int state = STATE_DISABLED;

        if (WindowManagerPolicyControl.immersiveStatusFilterMatches(packageName)) {
            state = STATE_STATUS_HIDDEN;
        }
        if (WindowManagerPolicyControl.immersiveNavigationFilterMatches(packageName)) {
            if (state == STATE_DISABLED) {
                state = STATE_NAVIGATION_HIDDEN;
            } else {
                state = STATE_BOTH_HIDDEN;
            }
        }
        return state;
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
        mActivityFilter.updateLauncherInfoList();
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> entries) {
        handleAppEntries(entries);
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle("Choose apps to add");
        getActivity().getMenuInflater().inflate(R.menu.expanded_desktop_chooser, menu);
        MenuItem launcherOnly = menu.findItem(R.id.show_launcher_only);
        launcherOnly.setChecked(mActivityFilter.onlyLauncher);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        switch (item.getItemId()) {
            case R.id.done:
                SparseBooleanArray positions = mAllListView.getCheckedItemPositions();
                final int count = positions.size();
                for (int i = 0; i < count; i++) {
                    if (positions.valueAt(i)) {
                        mUserPackagesAdapter.add(
                                mAllPackagesAdapter.entries
                                        .get(positions.keyAt(i)).info.packageName);
                    }
                }
                mUserPackagesAdapter.notifyDataSetChanged();
                mNeedCallRebuild = true;
                hideAllApps();
                return true;
            case R.id.show_launcher_only:
                boolean checked = !item.isChecked();
                item.setChecked(checked);
                mActivityFilter.setOnlyLauncher(checked);
                rebuild();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mAllListView.post(new Runnable() {
            @Override
            public void run() {
                hideAllApps();
            }
        });
    }

    @Override
    public void onSpinnerItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case STATE_DISABLE_FOR_ALL:
                disableForAll();
                break;
            case STATE_ENABLE_FOR_ALL:
                enableForAll();
                break;
            case STATE_USER_CONFIGURABLE:
                userConfigurableSettings();
                break;
        }
    }

    @Override
    public void onSpinnerNothingSelected(AdapterView<?> parent) {
        // Ignore
    }

    private class UserPackagesAdapter extends BaseAdapter implements AdapterView.OnItemSelectedListener {
        private ArrayList<String> entries = new ArrayList<>();
        private final Context context;
        private final LayoutInflater inflater;
        private final PackageManager mPackageManager;
        private final ModeAdapter modesAdapter;

        private UserPackagesAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            mPackageManager = context.getPackageManager();
            modesAdapter = new ModeAdapter(context);
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void add(String packageName) {
            if (!entries.contains(packageName)) {
                entries.add(packageName);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder(inflater.inflate(R.layout.expanded_item, parent, false));
                holder.mode.setAdapter(modesAdapter);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String packageName = entries.get(position);
            ApplicationsState.AppEntry entry = mEntryMap.get(packageName);
            if (entry == null) {
                try {
                    ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
                    if (info != null) {
                        entry = new ApplicationsState.AppEntry(context, info, position);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Do nothing
                }
            }

            if (entry == null) {
                return holder.rootView;
            }

            holder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            if (mExpandedDesktopState == STATE_USER_CONFIGURABLE) {
                holder.icon.setImageDrawable(entry.icon);
            } else {
                Drawable icon = entry.icon.getConstantState().newDrawable().mutate();
                icon.setColorFilter(0x60000000, PorterDuff.Mode.DST_IN);
                holder.icon.setImageDrawable(icon);
            }

            holder.mode.setOnItemSelectedListener(null);
            holder.mode.setSelection(getStateForPackage(entry.info.packageName), false);
            holder.mode.setOnItemSelectedListener(this);
            holder.mode.setTag(entry);

            holder.close.setOnClickListener(mCloseClickListener);
            holder.close.setTag(entry);

            return holder.rootView;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            ApplicationsState.AppEntry entry = (ApplicationsState.AppEntry) parent.getTag();

            WindowManagerPolicyControl.removeFromWhiteLists(entry.info.packageName);
            switch (position) {
                case STATE_STATUS_HIDDEN:
                    WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    break;
                case STATE_NAVIGATION_HIDDEN:
                    WindowManagerPolicyControl.addToNavigationWhiteList(entry.info.packageName);
                    break;
                case STATE_BOTH_HIDDEN:
                    WindowManagerPolicyControl.addToStatusWhiteList(entry.info.packageName);
                    WindowManagerPolicyControl.addToNavigationWhiteList(entry.info.packageName);
                    break;
            }
            String key = mExpandedDesktopState == STATE_USER_CONFIGURABLE ?
                    Settings.Global.POLICY_CONTROL : Settings.Global.POLICY_CONTROL_SELECTED;
            WindowManagerPolicyControl.saveToSettings(parent.getContext(), key);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private void handleAppEntries(List<ApplicationsState.AppEntry> entries) {
        mAllPackagesAdapter.setEntries(entries);
        mEntryMap.clear();
        for (ApplicationsState.AppEntry e : entries) {
            mEntryMap.put(e.info.packageName, e);
        }

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
        if (mUserListView != null && mUserListView.getEmptyView() != mEmptyView) {
            mUserListView.setEmptyView(mEmptyView);
        }

        mUserPackagesAdapter.notifyDataSetInvalidated();
    }

    private void rebuild() {
        ArrayList<ApplicationsState.AppEntry> newEntries = mSession.rebuild(
                mActivityFilter, ApplicationsState.ALPHA_COMPARATOR);
        if (newEntries != null) {
            handleAppEntries(newEntries);
        }
    }

    private class AllPackagesAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private List<ApplicationsState.AppEntry> entries = new ArrayList<>();

        public AllPackagesAdapter(Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return entries.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder(inflater.inflate(R.layout.expanded_item, parent, false));
                holder.mode.setVisibility(View.GONE);
                holder.close.setVisibility(View.GONE);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ApplicationsState.AppEntry entry = entries.get(position);

            holder.title.setText(entry.label);
            mApplicationsState.ensureIcon(entry);
            holder.icon.setImageDrawable(entry.icon);

            return holder.rootView;
        }

        private void setEntries(List<ApplicationsState.AppEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }
    }

    private static class ViewHolder {
        private TextView title;
        private Spinner mode;
        private ImageView icon;
        private View rootView;
        private View close;

        private ViewHolder(View view) {
            this.title = (TextView) view.findViewById(R.id.app_name);
            this.mode = (Spinner) view.findViewById(R.id.app_mode);
            this.icon = (ImageView) view.findViewById(R.id.app_icon);
            this.close = view.findViewById(R.id.close);
            this.rootView = view;

            view.setTag(this);
        }
    }

    private static class ModeAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private boolean hasNavigationBar = true;
        private final int[] items = {R.string.expanded_hide_nothing, R.string.expanded_hide_status,
                R.string.expanded_hide_navigation, R.string.expanded_hide_both};

        private ModeAdapter(Context context) {
            inflater = LayoutInflater.from(context);

            try {
                hasNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
            } catch (RemoteException e) {
                // Do nothing
            }
        }

        @Override
        public int getCount() {
            return hasNavigationBar ? 4 : 2;
        }

        @Override
        public Object getItem(int position) {
            return items[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view;
            if (convertView != null) {
                view = (TextView) convertView;
            } else {
                view = (TextView) inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            view.setText(items[position]);

            return view;
        }
    }

    private class ActivityFilter implements ApplicationsState.AppFilter {

        private final PackageManager mPackageManager;
        private final List<String> launcherResolveInfoList = new ArrayList<String>();
        private boolean onlyLauncher = true;

        private ActivityFilter(PackageManager packageManager) {
            this.mPackageManager = packageManager;

            updateLauncherInfoList();
        }

        public void updateLauncherInfoList() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfoList = mPackageManager.queryIntentActivities(i, 0);

            synchronized (launcherResolveInfoList) {
                launcherResolveInfoList.clear();
                for (ResolveInfo ri : resolveInfoList) {
                    launcherResolveInfoList.add(ri.activityInfo.packageName);
                }
            }
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            boolean show = !mUserPackagesAdapter.entries.contains(info.packageName);
            if (show && onlyLauncher) {
                synchronized (launcherResolveInfoList) {
                    show = launcherResolveInfoList.contains(info.packageName);
                }
            }
            return show;
        }

        public void setOnlyLauncher(boolean onlyLauncher) {
            this.onlyLauncher = onlyLauncher;
        }
    }

    private class RotationDrawable extends Drawable {
        public final Property<RotationDrawable, Integer> COLOR
                = new IntProperty<RotationDrawable>("currentColor") {

            @Override
            public void setValue(RotationDrawable object, int value) {
                object.setCurrentColor(value);
            }

            @Override
            public Integer get(RotationDrawable object) {
                return object.currentColor;
            }
        };
        public final Property<RotationDrawable, Integer> ROTATION
                = new IntProperty<RotationDrawable>("currentRotation") {
            @Override
            public void setValue(RotationDrawable object, int value) {
                object.setCurrentRotation(value);
            }

            @Override
            public Integer get(RotationDrawable object) {
                return object.currentRotation;
            }
        };

        private final Paint backgroundPaint;

        private int currentColor;
        private int currentRotation;

        private RotationDrawable() {
            backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(currentColor);
        }

        @Override
        public void draw(Canvas canvas) {
            final Rect bounds = getBounds();
            final int cx = bounds.width() / 2;
            final int cy = bounds.height() / 2;

            canvas.drawCircle(cx, cy, cx, backgroundPaint);
            canvas.translate(cx, cy);
            canvas.rotate(currentRotation);
            canvas.translate(- cx, - cy);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        public void setCurrentRotation(int currentRotation) {
            this.currentRotation = currentRotation;
            invalidateSelf();
        }

        public void setCurrentColor(int currentColor) {
            this.currentColor = currentColor;
            backgroundPaint.setColor(currentColor);
            invalidateSelf();
        }

        @Override
        public void getOutline(@NonNull Outline outline) {
            outline.setAlpha(1);
            outline.setOval(getBounds());
        }
    }

    private class AddCloseDrawable extends RotationDrawable {
        private final int colorNormal;
        private final int colorClose;
        private final int lineWidth;
        private final int contentSize;
        private final Paint plusPaint;
        
        private static final int ROTATION_NORMAL = 0;
        private static final int ROTATION_CLOSE = - 45;
        private static final int ANIMATION_DURATION = 200;

        private Animator currentAnimator;

        private AddCloseDrawable(Context context) {
            colorNormal = context.getResources().getColor(R.color.expanded_button_color_normal);
            colorClose = context.getResources().getColor(R.color.expanded_button_color_close);

            contentSize = (int) (24 * context.getResources().getDisplayMetrics().density);
            lineWidth = context.getResources().getDimensionPixelSize(R.dimen.fab_line_width);

            setCurrentColor(colorNormal);

            plusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            plusPaint.setColor(Color.WHITE);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);

            final Rect bounds = getBounds();
            final int cx = bounds.width() / 2;
            final int cy = bounds.height() / 2;

            canvas.drawRect(cx - contentSize / 2,
                    cy - lineWidth / 2,
                    cx + contentSize / 2,
                    cy + lineWidth / 2,
                    plusPaint);
            canvas.drawRect(cx - lineWidth / 2,
                    cy - contentSize / 2,
                    cx + lineWidth / 2,
                    cy + contentSize / 2, plusPaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        public void animateTo(int color, int rotation) {
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }

            if (!isVisible()) {
                setCurrentColor(color);
                setCurrentRotation(rotation);
                return;
            }

            AnimatorSet set = new AnimatorSet();

            set.playTogether(ObjectAnimator.ofArgb(this, COLOR, color),
                    ObjectAnimator.ofInt(this, ROTATION, rotation));
            set.setDuration(ANIMATION_DURATION);
            set.setInterpolator(mInterpolator);
            set.start();

            currentAnimator = set;
        }

        public void animateToClose() {
            animateTo(colorClose, ROTATION_CLOSE);
        }

        public void animateToAdd() {
            animateTo(colorNormal, ROTATION_NORMAL);
        }
    }

    private class WrappedRotationDrawable extends RotationDrawable {
        private Drawable drawable;

        private WrappedRotationDrawable(Context context, Drawable drawable) {
            this.drawable = drawable;

            setCurrentColor(context.getResources().getColor(R.color.expanded_button_color_normal));
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);

            drawable.draw(canvas);
        }

        @Override
        public void setBounds(Rect bounds) {
            setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
        }

        @Override
        public void setBounds(int left, int top, int right, int bottom) {
            final int width = Math.min(right - left, drawable.getMinimumHeight());
            final int height = Math.min(bottom - top, drawable.getMinimumHeight());

            final int dw = (right - left - width) / 2;
            final int dh = (bottom - top - height) / 2;

            super.setBounds(left, top, right, bottom);
            drawable.setBounds(left + dw, top + dh, right - dw, bottom - dh);
        }
    }
}
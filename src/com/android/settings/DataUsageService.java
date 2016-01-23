package com.android.settings;


import android.app.*;
import android.content.*;
import android.database.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.os.Process;
import android.support.v4.app.*;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.android.settings.net.*;
import android.net.NetworkTemplate;
import static android.net.NetworkTemplate.buildTemplateMobileAll;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.net.NetworkStats;
import android.os.SystemProperties;
import android.content.pm.UserInfo;

import com.google.gson.Gson;


import java.util.*;

/**
 * IntentService, launched by the AlarmManager at Boot Time from (BootReceiver) used
 * to collect per app cellular usage networking statistics and generate warning messages
 * to the user when an App consumes too much BW, giving the user an option to disable
 * Warning Message generation or to disable Network Access for the offending App
 */

public class DataUsageService extends IntentService {
    private final static String TAG = DataUsageService.class.getSimpleName();
    private final static String TAB_MOBILE = "mobile";
    private Context mContext;
    private final static boolean DEBUG = true;

    // Service worker tasks will run on the background thread, create a Handler to
    // communicate with UI Thread, if needed
    private Handler mUiHandler = new Handler();

    private INetworkStatsService mStatsService;
    private INetworkStatsSession mStatsSession;
    private NetworkTemplate mTemplate;
    private SubscriptionManager mSubscriptionManager;
    private List<SubscriptionInfo> mSubInfoList;
    private Map<Integer,String> mMobileTagMap;
    private UserManager mUserManager;
    private List<UserHandle> mProfiles;
    private long mLargest;
    private int mCurrentUserId;
    private UidDetailProvider mUidDetailProvider;
    SparseArray<DataUsageSummary.AppItem> mKnownItems;
    private NotificationManager mNotificationManager;

    // specifies minimum number of samples to collect before running algorithm
    private static final int MIN_SLOW_SAMPLE_COUNT = 10;
    private static final int MIN_FAST_SAMPLE_COUNT = 3;
    // specifies percentage by which fast average must exceed slow avg to trigger a warning
    // one standard deviation - or should it be 34%, since we are only looking at above and not
    // below. And how many standard deviations should it be?
    private static final int WARNING_PERCENTAGE = 68;
    // specifies maximum bw that is still considered as idle - to discard pings, etc...
    private static final long MAX_IDLE_BW = 1024;
    // specifies the sample period in msec
    public static final long SAMPLE_PERIOD = 60000;
    public static final long START_DELAY = 60000;

    // notification ID to use by the DataUsageService for updates to notifications
    public static final int DATA_USAGE_SERVICE_NOTIFICATION_ID = 102030;    // TODO - ???

    public static final String HIDE_ACTION      = "com.android.settings.data_usage_hide_action";
    public static final String DISABLE_ACTION   = "com.android.settings.data_usage_disable_action";
    public static final int DATA_USAGE_BROADCAST_REQUEST_CODE   = 0x102040; // TODO - ???
    public static final String DATA_USAGE_NOTIFICATION_UID   =
            "com.android.settings.data_usage_notification_uid";
    public static final String DATA_USAGE_NOTIFICATION_TITLE =
            "com.android.settings.data_usage_notification_title";

    public DataUsageService() {
        super(TAG);
    }

    @android.support.annotation.Nullable
    @Override
    public IBinder onBind(android.content.Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * When periodic alarm is generated, via AlarmManager, the Intent is delivered here
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = this;

        // initialize various networking managers/interfaces/sessions/etc...
        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mStatsSession = null;
        try {
            mStatsSession = mStatsService.openSession();
            mSubscriptionManager = SubscriptionManager.from(mContext);
            mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
            mMobileTagMap = initMobileTabTag(mSubInfoList);
            mTemplate = buildTemplateMobileAll(
                    getActiveSubscriberId(mContext, getSubId(TAB_MOBILE + "1")));
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: " + e.getMessage());
        }

        mUserManager = (UserManager)mContext.getSystemService(Context.USER_SERVICE);
        mProfiles = mUserManager.getUserProfiles();
        mCurrentUserId = ActivityManager.getCurrentUser();
        mUidDetailProvider = new UidDetailProvider(mContext);
        mKnownItems = new SparseArray<DataUsageSummary.AppItem>();
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        // run the actual dataUsage collection and processing
        dataUsageUpdate();
    }

    private static String getActiveSubscriberId(Context context, int subId) {
        final TelephonyManager tele = TelephonyManager.from(context);
        String retVal = tele.getSubscriberId(subId);
        return retVal;
    }


    private int getSubId(String currentTab) {
        if (mMobileTagMap != null) {
            Set<Integer> set = mMobileTagMap.keySet();
            for (Integer subId : set) {
                if (mMobileTagMap.get(subId).equals(currentTab)) {
                    return subId;
                }
            }
        }
        return -1;
    }

    private Map<Integer, String> initMobileTabTag(List<SubscriptionInfo> subInfoList) {
        Map<Integer, String> map = null;
        if (subInfoList != null) {
            String mobileTag;
            map = new HashMap<Integer, String>();
            for (SubscriptionInfo subInfo : subInfoList) {
                mobileTag = TAB_MOBILE + String.valueOf(subInfo.getSubscriptionId());
                map.put(subInfo.getSubscriptionId(), mobileTag);
            }
        }
        return map;
    }

    /**
     * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
     * Creates the item, if needed
     *
     */
    private void accumulate(int collapseKey, NetworkStats.Entry entry, int itemCategory) {
        int uid = entry.uid;
        DataUsageSummary.AppItem item = mKnownItems.get(collapseKey);
        if (item == null) {
            item = new DataUsageSummary.AppItem(collapseKey);
            item.category = itemCategory;
            mKnownItems.put(item.key, item);
        }
        item.addUid(uid);
        item.total += entry.rxBytes + entry.txBytes;
        if (mLargest < item.total) {
            mLargest = item.total;
        }

    }



    private void clearStats() {
        for(int i = 0; i < mKnownItems.size(); i++) {
            int key = mKnownItems.keyAt(i);
            DataUsageSummary.AppItem appItem = mKnownItems.get(key);
            appItem.total = 0;
        }
    }

    private class DataUsageExtraInfo {
        ArrayList<Long> samples;
    }
    private String mAppWarnExtra;

    private void dataUsageUpdate() {
        long startTime = 0;
        long endTime = System.currentTimeMillis() * 2;
        mLargest = 0;

        clearStats();

        NetworkStats networkStats = null;
        try {
            if (mStatsSession != null) {
                networkStats = mStatsSession.getSummaryForAllUid(mTemplate, startTime, endTime,
                        false);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: " + e.getMessage());
        }


        // collect network stats for all app consuming bw
        if (networkStats != null) {
            int size = networkStats.size();
            NetworkStats.Entry entry = null;

            for(int i = 0; i < size; i++) {
                entry = networkStats.getValues(i, entry);
                int collapseKey;
                int category;
                int uid = entry.uid;
                int userId = UserHandle.getUserId(uid);
                if (UserHandle.isApp(uid)) {
                    if (mProfiles.contains(new UserHandle(userId))) {
                        if (userId != mCurrentUserId) {
                            // add to a managed user item
                            int managedKey = UidDetailProvider.buildKeyForUser(userId);
                            accumulate(managedKey, entry,
                                    DataUsageSummary.AppItem.CATEGORY_USER);
                        }
                        collapseKey = uid;
                        category = DataUsageSummary.AppItem.CATEGORY_APP;
                    } else {
                        // if it is a removed user, add it to the removed users' key
                        UserInfo userInfo = mUserManager.getUserInfo(userId);
                        if (userInfo == null) {
                            collapseKey = UID_REMOVED;
                            category = DataUsageSummary.AppItem.CATEGORY_APP;
                        } else {
                            collapseKey = UidDetailProvider.buildKeyForUser(userId);
                            category = DataUsageSummary.AppItem.CATEGORY_USER;
                        }
                    }
                    accumulate(collapseKey, entry, category);
                }
            }
        }
        boolean appWarnActive = false;
        long appWarnBytes = 0;
        long appWarnUid;
        int appWarnSlowSamples;
        int appWarnFastSamples;
        long appWarnSlowAvg;
        long appWarnFastAvg;
        String appWarnExtra = "";

        // lookup Apps in the DB that have warning enabled
        Cursor cursor = getContentResolver().query(
                DataUsageProvider.CONTENT_URI,
                null,       // projection - return all
                DataUsageProvider.DATAUSAGE_DB_ENB + " = ? ",
                new String [] { "1" },
                null
        );
        if (cursor == null) {
            return;
        }

        while(cursor.moveToNext()) {
            appWarnUid = cursor.getInt(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_UID);
            appWarnActive = cursor.getInt(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_ACTIVE) > 0;
            appWarnBytes = cursor.getLong(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_BYTES);
            appWarnSlowSamples = cursor.getInt(DataUsageProvider
                    .DATAUSAGE_DB_COLUMN_OF_SLOW_SAMPLES);
            appWarnSlowAvg = cursor.getLong(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_SLOW_AVG);
            appWarnFastSamples = cursor.getInt(DataUsageProvider
                    .DATAUSAGE_DB_COLUMN_OF_FAST_SAMPLES);
            appWarnFastAvg = cursor.getLong(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_FAST_AVG);
            mAppWarnExtra = cursor.getString(DataUsageProvider.DATAUSAGE_DB_COLUMN_OF_EXTRA);

            DataUsageSummary.AppItem appItem = mKnownItems.get((int)appWarnUid);

            if (appItem != null) {
                final UidDetail detail = mUidDetailProvider.getUidDetail(appItem.key, true);
                long bytesDelta = appWarnBytes == 0 ? 0 : appItem.total - appWarnBytes;
                if (DEBUG) {
                    Log.v(TAG, detail.label.toString() +
                            " cur:" + appItem.total +
                            " prev:" + appWarnBytes +
                            " SlowSamples:" + appWarnSlowSamples +
                            " SlowAvg:" + appWarnSlowAvg +
                            " FastSamples:" + appWarnFastSamples +
                            " FastAvg:" + appWarnFastAvg
                    );
                }
                if (bytesDelta > MAX_IDLE_BW) {
                    // enough BW consumed during this sample - evaluate algorithm
                    if (DEBUG) {
                        Log.v(TAG, "bytesDelta:" + bytesDelta + " greater than MAX_IDLE_BW");
                    }
                    if (appWarnSlowSamples < MIN_SLOW_SAMPLE_COUNT) {
                        if (DEBUG) {
                            Log.v(TAG, "SlowSamples:" + appWarnSlowSamples + " less than:" +
                                    MIN_SLOW_SAMPLE_COUNT);
                            Log.v(TAG, "SlowAvg:" + appWarnSlowAvg);
                        }
                        // not enough samples acquired for the slow average, keep accumulating
                        // samples
                        appWarnSlowAvg = computeAvg(appWarnSlowAvg, appWarnSlowSamples,
                                MIN_SLOW_SAMPLE_COUNT, bytesDelta);
                        appWarnSlowSamples++;

                        // fast average requires fewer samples than slow average, so at this point
                        // we may have accumulated enough or not, need to check
                        if (appWarnFastSamples < MIN_FAST_SAMPLE_COUNT) {
                            // not enough fast samples
                            appWarnFastAvg = computeAvg(appWarnFastAvg, appWarnFastSamples,
                                    MIN_FAST_SAMPLE_COUNT, bytesDelta);
                            appWarnFastSamples++;
                        } else {
                            // enough fast samples
                            appWarnFastAvg = computeAvg(appWarnFastAvg, appWarnFastSamples,
                                    MIN_FAST_SAMPLE_COUNT, bytesDelta);
                        }

                        updateDb(appItem.key,
                                appWarnSlowAvg, appWarnSlowSamples,
                                appWarnFastAvg, appWarnFastSamples,
                                0, appItem.total);
                    } else {
                        if (DEBUG) {
                            Log.v(TAG, "SlowSamples:" + appWarnSlowSamples + " greater than " +
                                    MIN_SLOW_SAMPLE_COUNT);
                        }
                        // enough samples acquired for the average, evaluate warning algorithm
                        float avgExceedPercent = appWarnFastAvg-appWarnSlowAvg;
                        avgExceedPercent /= appWarnSlowAvg;
                        avgExceedPercent *= 100;
                        if (DEBUG) {
                            Log.v(TAG, "avg exceeded percent:" + avgExceedPercent);
                        }

                        if ((appWarnFastAvg > appWarnSlowAvg) && (avgExceedPercent >
                                WARNING_PERCENTAGE)) {
                            genNotification(appItem.key, detail.label.toString(), !appWarnActive);
                            if (!appWarnActive) {
                                appWarnActive = true;
                            }
                        } else {
                            appWarnActive = false;
                        }
                        appWarnSlowAvg = computeAvg(appWarnSlowAvg, appWarnSlowSamples,
                                MIN_SLOW_SAMPLE_COUNT, bytesDelta);
                        appWarnFastAvg = computeAvg(appWarnFastAvg, appWarnFastSamples,
                                MIN_FAST_SAMPLE_COUNT, bytesDelta);
                        updateDb(
                                appItem.key,
                                appWarnSlowAvg, appWarnSlowSamples,
                                appWarnFastAvg, appWarnFastSamples,
                                appWarnActive ? 1 : 0, appItem.total
                        );

                    }
                } else {
                    // not enough BW consumed during this sample - simply update bytes
                    if (DEBUG) {
                        Log.v(TAG, "bytesDelta:" + bytesDelta + " less than MAX_IDLE_BW:" + MAX_IDLE_BW);
                    }
                    updateDb(appItem.key, appItem.total);
                }
            }
        }
        cursor.close();
    }

    long computeAvg(long avg, int samples, int min_samples, long delta) {
        float temp;

        if (samples < min_samples) {
            temp = avg * samples;
            temp += delta;
            temp /= (samples + 1);
            return (long)temp;
        } else {
            temp = avg * (samples - 1);
            temp += delta;
            temp /= samples;
            return (long)temp;
        }
    }


    private void updateDb(int uid, long bytes) {
        ContentValues values = new ContentValues();
        String extraInfo = genExtraInfo(bytes);
        if (DEBUG) {
            Log.v(TAG, "UID:" + uid + " Bytes:" + bytes + " ExtraInfo:" + extraInfo);
        }
        values.put(DataUsageProvider.DATAUSAGE_DB_BYTES, bytes);
        values.put(DataUsageProvider.DATAUSAGE_DB_EXTRA, extraInfo);
        getContentResolver().update(
                DataUsageProvider.CONTENT_URI,
                values,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String[]{String.valueOf(uid)}
        );
    }

    private void updateDb(
            int uid, long slowAvg, int slowSamples, long fastAvg, int fastSamples,
            int active, long bytes
    ) {
        ContentValues values = new ContentValues();
        String extraInfo = genExtraInfo(bytes);
        values.put(DataUsageProvider.DATAUSAGE_DB_SLOW_AVG, slowAvg);
        values.put(DataUsageProvider.DATAUSAGE_DB_SLOW_SAMPLES, slowSamples);
        values.put(DataUsageProvider.DATAUSAGE_DB_FAST_AVG, fastAvg);
        values.put(DataUsageProvider.DATAUSAGE_DB_FAST_SAMPLES, fastSamples);
        values.put(DataUsageProvider.DATAUSAGE_DB_ACTIVE, active);
        values.put(DataUsageProvider.DATAUSAGE_DB_BYTES, bytes);
        values.put(DataUsageProvider.DATAUSAGE_DB_EXTRA, extraInfo);

        getContentResolver().update(
                DataUsageProvider.CONTENT_URI,
                values,
                DataUsageProvider.DATAUSAGE_DB_UID + " = ? ",
                new String[]{String.valueOf(uid)}
        );
    }

    private final static int MAX_EXTRA_SAMPLE_COUNT = 10;
    private String genExtraInfo(long bytes) {

        Gson gson = new Gson();

        DataUsageExtraInfo extraInfo;
        if (mAppWarnExtra == null || mAppWarnExtra == "") {
            extraInfo = null;
        } else {
            try {
                extraInfo = gson.fromJson(mAppWarnExtra, DataUsageExtraInfo.class);
            } catch (Exception e) {
                extraInfo = null;
            }
        }

        if (extraInfo == null) {
            extraInfo = new DataUsageExtraInfo();
            extraInfo.samples = new ArrayList<Long>();
        }
        if (DEBUG) {
            Log.v(TAG, "genExtraInfo: currentSamples:" + extraInfo.samples.size());
        }
        if (extraInfo.samples.size() == MAX_EXTRA_SAMPLE_COUNT) {
            extraInfo.samples.remove(0);
        }
        extraInfo.samples.add(bytes);
        String extraInfoJson = gson.toJson(extraInfo);
        return extraInfoJson;
    }



    private void genNotification(long uid, String appTitle, boolean firstTime) {
        Intent hideIntent = new Intent();
        hideIntent.setAction(HIDE_ACTION);
        hideIntent.putExtra(DATA_USAGE_NOTIFICATION_UID, uid);
        hideIntent.putExtra(DATA_USAGE_NOTIFICATION_TITLE, appTitle);
        PendingIntent hidePendingIntent = PendingIntent.getBroadcast(
                mContext, DATA_USAGE_BROADCAST_REQUEST_CODE, hideIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent disableIntent = new Intent();
        disableIntent.setAction(DISABLE_ACTION);
        disableIntent.putExtra(DATA_USAGE_NOTIFICATION_UID, uid);
        PendingIntent disablePendingIntent = PendingIntent.getBroadcast(
                mContext, DATA_USAGE_BROADCAST_REQUEST_CODE, disableIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // create an intent to launch DataUsage Activity
        Intent dataUsageIntent = new Intent(mContext, Settings.DataUsageSummaryActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(Settings.DataUsageSummaryActivity.class);
        stackBuilder.addNextIntent(dataUsageIntent);

        PendingIntent dataUsagePendingIntent = stackBuilder.getPendingIntent(0, PendingIntent
                .FLAG_UPDATE_CURRENT);

        // NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
        Notification.Builder builder = new Notification.Builder(mContext)
                // .setSmallIcon(R.drawable.data_warning)
                // .setSmallIcon(R.drawable.ic_sim_card_alert_white_48dp)
                .setSmallIcon(R.drawable.data_usage_48dp)
                .setContentTitle("Mobile data alert")
                .setAutoCancel(true)        // remove notification when clicked on
                .setContentText(appTitle)   // non-expanded view message
                .setColor(mContext.getColor(R.color.data_usage_notification_icon_color))
                .setStyle(new Notification.BigTextStyle()
                    .bigText(appTitle + " is using a lot of mobile data. Tap to view details."));

        if (firstTime) {
            builder.addAction(
                    // R.drawable.data_warning_disable,
                    // android.R.drawable.stat_sys_data_bluetooth,
                    R.drawable.data_usage_disable_24dp,
                    getResources().getString(R.string.data_usage_disable_long),
                    disablePendingIntent);
        } else {
            builder.addAction(
                    // R.drawable.data_warning_disable,
                    // android.R.drawable.stat_sys_data_bluetooth,
                    R.drawable.data_usage_disable_24dp,
                    getResources().getString(R.string.data_usage_disable_short),
                    disablePendingIntent);
            builder.addAction(
                    // R.drawable.data_warning_hide,
                    // android.R.drawable.stat_sys_download_done,
                    R.drawable.data_usage_hide_24dp,
                    getResources().getString(R.string.data_usage_hide),
                    hidePendingIntent)
            ;
        }

        builder.setContentIntent(dataUsagePendingIntent);
        mNotificationManager.notify(DATA_USAGE_SERVICE_NOTIFICATION_ID, builder.build());
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

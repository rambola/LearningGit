/*
 * BORQS Software Solutions Pvt Ltd. CONFIDENTIAL
 * Copyright (c) 2014 All rights reserved.
 *
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by BORQS Software
 * Solutions Pvt Ltd. No part of the Material may be used,copied,
 * reproduced, modified, published, uploaded,posted, transmitted,
 * distributed, or disclosed in any way without BORQS Software
 * Solutions Pvt Ltd. prior written permission.
 *
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by BORQS Software Solutions Pvt Ltd. in writing.
 *
 */

package com.android.systemui;

import java.util.ArrayList;
import java.util.Comparator;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.os.ServiceManager;
import android.app.INotificationManager;
import android.service.notification.INotificationListener;
import android.os.RemoteException;
import android.content.ComponentName;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.app.ActivityManager;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.view.Gravity;

import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;

import android.service.notification.NotificationRankingUpdate;
import android.service.notification.IStatusBarNotificationHolder;
import android.os.Process;
/* NotificationManagerActivity is used to show All Active Notifications
 which are present in the StatusBar */

public class NotificationManagerActivity extends Activity {

    private static final String TAG = "NotificationManagerActivity";
    private static final boolean DEBUG = false;
    private static int NOTIFICATION_DELAY = 100;

    private PackageManager mPm;
    private INotificationManager mNoMan;
    private Context mContext;
    private NotificationHistoryAdapter mAdapter;
    private ListView mListView;
    private TextView mTextView;
    private IStatusBarService mBarService;
    private HistoricalNotificationInfo mNotificationInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Android actionbar icon/homebutton should not be shown for this
        // acitivity
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        setContentView(R.layout.notifications);
        mContext = this;
        mPm = mContext.getPackageManager();
        // NotificationManager Object created to get all Active notifications
        mNoMan = INotificationManager.Stub.asInterface(ServiceManager
                .getService(Context.NOTIFICATION_SERVICE));
        // Statusbarservice object to be called while clearing a/all
        // notification
        mBarService = IStatusBarService.Stub.asInterface(ServiceManager
                .getService(Context.STATUS_BAR_SERVICE));
        try {
            // register for a listener for which we get onNotificationPosted and
            // onnotification removed callback
            if (mNoMan != null)
                mNoMan.registerListener(mListener,
                        new ComponentName(mContext.getPackageName(), this
                                .getClass().getCanonicalName()), 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mListView = (ListView) findViewById(R.id.notifyList);
        mTextView = (TextView) findViewById(R.id.notification);
        // Construct the adapter to fill the ListView
        mAdapter = new NotificationHistoryAdapter(mContext);
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the active notifications List whenever we resume the activity
        refreshList();
    }

    private Runnable mRefreshListRunnable = new Runnable() {
        @Override
        public void run() {
            refreshList();
        }
    };

    private INotificationListener.Stub mListener = new INotificationListener.Stub() {
        // Callbacks to listen to post of data on the notification
        @Override
        public void onNotificationPosted(IStatusBarNotificationHolder notification,
                                         NotificationRankingUpdate update)
                throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "onNotificationPosted: " + notification);

            if (mListView != null) {
                final Handler h = mListView.getHandler();
                h.removeCallbacks(mRefreshListRunnable);
                h.postDelayed(mRefreshListRunnable, NOTIFICATION_DELAY);
            }
        }

        // Callbacks to listen to remove of data on the notification
        @Override
        public void onNotificationRemoved(IStatusBarNotificationHolder notification,
                                          NotificationRankingUpdate update)
                throws RemoteException {
            if (DEBUG)
                Log.d(TAG, "======================================= onnotificationremoved");

            if (mListView != null) {
                // Post to a runnable to Refresh the data
                final Handler h = mListView.getHandler();
                h.removeCallbacks(mRefreshListRunnable);
                h.postDelayed(mRefreshListRunnable, 100);

            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) throws RemoteException {
        }

        @Override
        public void onNotificationRankingUpdate(NotificationRankingUpdate update)
                throws RemoteException {
        }

        @Override
        public void onListenerHintsChanged(int hints) throws RemoteException {
        }

        @Override
        public void onListenerConnected(NotificationRankingUpdate update) {
        }

        @Override
        public void onNotificationEnqueued(IStatusBarNotificationHolder notificationHolder,
                                           int importance, boolean user) throws RemoteException {
            // no-op in the listener
        }

        @Override
        public void onNotificationVisibilityChanged(String key, long time, boolean visible)
                throws RemoteException {
            // no-op in the listener
        }

        @Override
        public void onNotificationClick(String key, long time) throws RemoteException {
            // no-op in the listener
        }

        @Override
        public void onNotificationActionClick(String key, long time, int actionIndex)
                throws RemoteException {
            // no-op in the listener
        }

        @Override
        public void onNotificationRemovedReason(String key, long time, int reason)
                throws RemoteException {
            // no-op in the listener
        }
    }; // Sort using timestamp as Priority
    private final Comparator<HistoricalNotificationInfo> mNotificationSorter = new
            Comparator<HistoricalNotificationInfo>() {
                @Override
                public int compare(HistoricalNotificationInfo lhs,
                                   HistoricalNotificationInfo rhs) {
                    return (int) (rhs.timestamp - lhs.timestamp);
                }
            };

    private void refreshList() {

        List<HistoricalNotificationInfo> infos = loadNotifications();
        if (infos != null) {
            if (DEBUG)
                Log.d(TAG, "=========================  notification available" + infos.size());

            if (infos.size() == 0) {
                mAdapter.clear();
                mTextView.setVisibility(View.VISIBLE);
                mTextView.setText(R.string.no_notification);
                mTextView.setGravity(Gravity.CENTER_VERTICAL
                        | Gravity.CENTER_HORIZONTAL);
                return;
            }
            // when new notification arrives we refresh the list. if menu option
            // is open close the list
            ((Activity) mContext).closeOptionsMenu();
            mTextView.setVisibility(View.GONE);
            mAdapter.clear();
            mAdapter.addAll(infos);

            mAdapter.sort(mNotificationSorter);
        } else {
            if (DEBUG)
                Log.d(TAG, "=========================  notification not available");

            mTextView.setText(R.string.no_notification);
            mTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        }

    }

    private Drawable loadPackageIconDrawable(String pkg, int userId) {
        Drawable icon = null;
        try {
            icon = mPm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return icon;
    }

    private Resources getResourcesForUserPackage(String pkg, int userId) {
        Resources r = null;

        if (pkg != null) {
            try {
                if (userId == UserHandle.USER_ALL) {
                    userId = UserHandle.USER_OWNER;
                }
                r = mPm.getResourcesForApplicationAsUser(pkg, userId);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(TAG, "Icon package not found: " + pkg);
                return null;
            }
        } else {
            r = mContext.getResources();
        }
        return r;
    }

    private CharSequence loadPackageName(String pkg) {
        try {
            ApplicationInfo info = mPm.getApplicationInfo(pkg,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            if (info != null)
                return mPm.getApplicationLabel(info);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pkg;
    }

    private Drawable loadIconDrawable(String pkg, int userId, int resId) {
        Resources r = getResourcesForUserPackage(pkg, userId);

        if (resId == 0) {
            return null;
        }

        try {
            return r.getDrawable(resId);
        } catch (RuntimeException e) {
            Log.w(TAG,
                    "Icon not found in " + (pkg != null ? resId : "<system>")
                            + ": " + Integer.toHexString(resId));
        }

        return null;
    }

    private static class HistoricalNotificationInfo {
        public String pkg;
        public Drawable pkgicon;
        public CharSequence subtitle;
        public Drawable icon;
        public CharSequence title;
        public int priority;
        public int user;
        public long timestamp;
        public boolean active;
        public Notification notification;
        public boolean clearflag;
        public String tag;
        public int Id;
        public String key;
    }

    private List<HistoricalNotificationInfo> loadNotifications() {
        final int currentUserId = ActivityManager.getCurrentUser();
        try {
            StatusBarNotification[] active = mNoMan.getActiveNotifications(mContext.getPackageName());

            List<HistoricalNotificationInfo> list = new ArrayList<HistoricalNotificationInfo>(
                    active.length);

            for (StatusBarNotification sbn : active) {
                final HistoricalNotificationInfo info = new HistoricalNotificationInfo();
                Log.e(TAG, "loadNotifications(), sbn.getPackageName(): "+sbn.getPackageName()+", mPackageList.size(): "+NotificationFilterListener.mPackageList.size());

                if(NotificationFilterListener.mPackageList.size() == 0
                                || NotificationFilterListener.mPackageList.contains(sbn.getPackageName())){
                    Log.e(TAG, "loadNotifications(), inside if..........");

                    info.key = sbn.getKey();
                    info.pkg = sbn.getPackageName();
                    info.clearflag = sbn.isClearable();
                    info.tag = sbn.getTag();
                    info.Id = sbn.getId();
                    info.user = sbn.getUserId();
                    info.icon = loadIconDrawable(info.pkg, info.user,
                            sbn.getNotification().icon);
                    info.pkgicon = loadPackageIconDrawable(info.pkg, info.user);
                    if (sbn.getNotification() != null) {
                        info.notification = sbn.getNotification();
                        if (sbn.getNotification().extras != null) {
                            info.subtitle = sbn.getNotification().extras
                                    .getString(Notification.EXTRA_TEXT);
                            info.title = sbn.getNotification().extras
                                    .getString(Notification.EXTRA_TITLE);
                            if (info.title == null || "".equals(info.title)) {
                                info.title = sbn.getNotification().extras
                                        .getString(Notification.EXTRA_TEXT);
                            }
                        }
                        if (info.title == null || "".equals(info.title)) {
                            if (sbn.getNotification().tickerText != null &&
                                    !(sbn.getNotification().tickerText.equals(""))) {
                                info.title = sbn.getNotification().tickerText.toString();
                            } else {
                                info.title = info.subtitle;
                            }
                        }
                        if (DEBUG) {
                            Log.d(TAG, "================ title========" + sbn.getNotification().extras.getString(Notification.EXTRA_TEXT));
                            Log.d(TAG, "================ title========" + sbn.getNotification().extras.getString(Notification.EXTRA_TITLE));
                            Log.d(TAG, "================ title========" + sbn.getNotification().extras.getString(Notification.EXTRA_INFO_TEXT));
                            Log.d(TAG, "================ title========" + sbn.getNotification().extras.getString(Notification.EXTRA_SUB_TEXT));
                        }
                        info.timestamp = sbn.getPostTime();
                        info.priority = sbn.getNotification().priority;
                        Log.d(TAG, info.timestamp + " " + info.pkg + " "
                                + info.title);
                    }
                    if ((info.title != null && info.title.length() != 0)
                            || (info.subtitle != null && info.subtitle.length() != 0))
                        list.add(info);
                }
            }
            return list;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class NotificationHistoryAdapter extends
            ArrayAdapter<HistoricalNotificationInfo> {
        private final LayoutInflater mInflater;

        public NotificationHistoryAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final HistoricalNotificationInfo info = getItem(position);
            if (DEBUG) {
                Log.d(TAG, "=======================================NotificationHistoryAdapte" + info);
                Log.d(TAG, " info.pkg , info.title" + info.pkg + " " + info.title);
            }
            final View row = convertView != null ? convertView
                    : createRow(parent);
            row.setTag(info);
            // bind icon
            if (info.icon != null) {
                ((ImageView) row.findViewById(android.R.id.icon))
                        .setVisibility(View.VISIBLE);
                ((ImageView) row.findViewById(android.R.id.icon))
                        .setImageDrawable(info.icon);
            } else {
                ((ImageView) row.findViewById(android.R.id.icon))
                        .setVisibility(View.GONE);
            }
            ((DateTimeView) row.findViewById(R.id.timestamp))
                    .setTime(info.timestamp);
            // bind caption
            ((TextView) row.findViewById(R.id.title)).setText(info.title);
            // set subtitle
            ((TextView) row.findViewById(R.id.subtitle)).setText(info.subtitle);
            row.setFocusable(true);
            return row;
        }

        private View createRow(ViewGroup parent) {
            final View row = mInflater.inflate(R.layout.notification_log_row,
                    parent, false);
            return row;
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        menu.clear();
        int i = 0;
        mNotificationInfo = (HistoricalNotificationInfo) mListView.getSelectedItem();
        // to show notification action in menu options
        if (mNotificationInfo != null) {
            if (mNotificationInfo.notification != null
                    && mNotificationInfo.notification.actions != null) {
                for (i = 0; i < mNotificationInfo.notification.actions.length; i++) {
                    menu.add(Menu.NONE, i, Menu.NONE,
                            mNotificationInfo.notification.actions[i].title);
                }
            }
            if (mNotificationInfo.clearflag) {
                menu.add(R.string.clear);
                menu.add(R.string.clearAll);
            }
        }

        if (menu.size() == 0) {
            return false;
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getTitle().equals(getString(R.string.clear))) {
            try {
                mBarService.onNotificationClear(mNotificationInfo.pkg, mNotificationInfo.tag,
                        mNotificationInfo.Id, mNotificationInfo.user);
                return true;
            } catch (Exception e) {
                Log.d(TAG, "" + e.toString());
            }
        } else if (item.getTitle().equals(getString(R.string.clearAll))) {
            try {
                List<HistoricalNotificationInfo> activenotify = loadNotifications();
                for (int i = 0; i < activenotify.size(); i++) {
                    if (activenotify.get(i).clearflag) {
                        mBarService.onClearAllNotifications(Process.SYSTEM_UID);
                        return true;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent();
        try {
            mNotificationInfo.notification.actions[item.getItemId()].actionIntent
                    .send(mContext, 0, intent);
            return true;
        } catch (Exception e) {
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == event.ACTION_UP
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            HistoricalNotificationInfo notify = (HistoricalNotificationInfo) mListView
                    .getSelectedItem();
            if (notify != null) {
                if (notify.notification.contentIntent != null) {
                    Intent intent = new Intent();
                    try {
                        notify.notification.contentIntent
                                .send(mContext, 0, intent);
                        /* Status bar service api called to clear the notification
                           when the contentIntent is triggered to launch the application
                        */
                        mBarService.onNotificationClick(notify.key);

                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (event.getAction() == event.ACTION_UP
                && event.getKeyCode() == KeyEvent.KEYCODE_CLEAR) {
            HistoricalNotificationInfo clrnotify = (HistoricalNotificationInfo) mListView
                    .getSelectedItem();
            if (clrnotify != null) {
                if (clrnotify.clearflag) {
                    try {
                        if (mBarService != null)
                            mBarService.onNotificationClear(clrnotify.pkg,
                                    clrnotify.tag, clrnotify.Id, clrnotify.user);
                    } catch (Exception e) {
                        Log.d(TAG, "exception" + e);
                        return false;
                    }
                    return true;
                } else {
                    Toast.makeText(mContext,
                            R.string.no_clearable_notification,
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}

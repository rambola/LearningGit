package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.Cursor;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;

import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import java.util.ArrayList;

/**
 * Created by Pathi.r on 7/6/2017.
 */

public class NotificationFilterListener extends BroadcastReceiver
{
    String TAG = NotificationFilterListener.class.getName();
    private final String PACKAGE_COLUMN_NAME = "AppPackageName";
    public static ArrayList<String> mPackageList = new ArrayList<>();

    private final boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean enableFiltering = intent.getBooleanExtra(Intent.EXTRA_SONIM_ENABLE_FILTER, false);

        if(DEBUG) {
            Log.i("SystemUI", "SystemUI, NotificationFilterListener, onReceive()......intent.getAction(): " + intent.getAction() + ", enableFiltering: " + enableFiltering);
            Log.i("SystemUI", "SystemUI, NotificationFilterListener, onReceive()......ContentURI from intent: " + intent.getStringExtra(Intent.EXTRA_SONIM_CONTENT_URI));
        }

        if(enableFiltering) {
            getWhiteListedAppsFromProvider(context, intent.getStringExtra(Intent.EXTRA_SONIM_CONTENT_URI));
        }
        else
            mPackageList.clear();

//        ((SystemUIApplication) context).startServicesIfNeeded();

//        context.stopService(new Intent(context, SystemUIService.class));
//        context.startService(new Intent(context, SystemUIService.class));

//        new NotificationData(NotificationData.Environment).updateRankingAndSort(getCurrentRanking());

//        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        StatusBarNotification[] statusBarNotifications = mNotificationManager.getActiveNotifications();
//        Log.d("SystemUI", "SystemUI, NotificationFilterListener, statusBarNotifications.length: "+statusBarNotifications.length);
//        for(int i=0; i<statusBarNotifications.length; i++) {
//            Log.d("SystemUI", "SystemUI, NotificationFilterListener, packageName: " + statusBarNotifications[i].getPackageName() + ", statusBarNotifications[i].getId(): " + statusBarNotifications[i].getId());
//        }

//        mNotificationManager.cancel();

//        INotificationManager mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
//        StatusBarNotification[] statusBarNotifications = mNoMan.getActiveNotifications(context.getPackageName());
//        Log.d("SystemUI", "SystemUI, NotificationFilterListener, statusBarNotifications.length: "+statusBarNotifications.length);
//        for(int i=0; i<statusBarNotifications.length; i++) {
//            Log.d("SystemUI", "SystemUI, NotificationFilterListener, packageName: " + statusBarNotifications[i].getPackageName() + ", statusBarNotifications[i].getId(): " + statusBarNotifications[i].getId());
//        }

//        SystemUIApplication app = (SystemUIApplication) context;
        SystemUIApplication app = (SystemUIApplication) context.getApplicationContext();
        PhoneStatusBar statusBar = app.getComponent(PhoneStatusBar.class);
        statusBar.requestNotificationUpdate();
    }

    private void getWhiteListedAppsFromProvider(Context context, String contentUri)
    {
        Uri uri = Uri.parse(contentUri);
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        Log.d(TAG, "getWhiteListedAppsFromProvider(), cursor: " + cursor);

        if(null != cursor) {
            if (!cursor.moveToFirst())
                Log.d(TAG, "getWhiteListedAppsFromProvider(), no content is present in provider");
            else {
                do {
                    Log.d(TAG, "getWhiteListedAppsFromProvider(), packageName: " + cursor.getString(cursor.getColumnIndex(PACKAGE_COLUMN_NAME)));

                    if(!mPackageList.contains(cursor.getString(cursor.getColumnIndex(PACKAGE_COLUMN_NAME)))) {
                        mPackageList.add(cursor.getString(cursor.getColumnIndex(PACKAGE_COLUMN_NAME)));
                    }
                } while (cursor.moveToNext());
            }
        }

        Log.i(TAG, "getWhiteListedAppsFromProvider(), packagesList.size(): " + mPackageList.size());
    }

/*    private void getWhiteListedAppsFromProvider(Context context, String contentUri)
    {
        if(null != contentUri) {
            Uri uri = Uri.parse(contentUri);
            if(null != uri) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (null != cursor) {
                    if (!cursor.moveToFirst()) {
                        if(DEBUG) {
                            Log.d(TAG, "getWhiteListedAppsFromProvider(), no content is present in provider");
                        }
                    } else {
                        do {
                            String packageName = cursor.getString(cursor.getColumnIndex(PACKAGE_COLUMN_NAME));
                            if (null != packageName && !mPackageList.contains(packageName)) {
                                mPackageList.add(cursor.getString(cursor.getColumnIndex(PACKAGE_COLUMN_NAME)));
                            }
                        } while (cursor.moveToNext());
                    }
                }
            }
        }

        if(DEBUG) {
            Log.d(TAG, "getWhiteListedAppsFromProvider(), packagesList.size(): " + mPackageList.size());
        }
    }*/

     /*private void readFile(String filePath)
    {
//        String filePath = "/storage/emulated/0/Download/WhiteListedApps.txt";
        Log.d(TAG, "SystemUI(), readFile(), filePath: "+filePath);

        StringBuffer stringBuffer = new StringBuffer();
        String aDataRow = "";
        String aBuffer = "";

        try {
            File myFile = new File(filePath);

            FileInputStream fIn = new FileInputStream(myFile);

            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));

            while ((aDataRow = myReader.readLine()) != null)
            {
                aBuffer += aDataRow;
            }

            myReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        parseJSONAndSetWhiteListedApps(aBuffer.toString());
    }

    private void parseJSONAndSetWhiteListedApps(String fileContent)
    {
        Log.d(TAG, "SystemUI(), parseJSONAndSetWhiteListedApps(), fileContent: "+fileContent);

        try {
            JSONObject jsonObject = new JSONObject(fileContent);

            jsonObject = jsonObject.getJSONObject("KioskMode");

            if(jsonObject.has("WhiteListApps")) {
                JSONArray jsonArray = jsonObject.getJSONArray("WhiteListApps");

//                Set<String> packagesSet = new HashSet<>();

                for (int i = 0; i < jsonArray.length(); i++) {
//                    packagesSet.add(jsonArray.getString(i));

                    mPackageList.add(jsonArray.getString(i));
                }

                Log.d(TAG, "SystemUI(), parseJSONAndSetWhiteListedApps(), packagesList.size(): " + mPackageList.size());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

   private boolean isMyLauncherDefault(Context context) {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        final String myPackageName = "com.sonim.kiosk";
        List<ComponentName> activities = new ArrayList<ComponentName>();
        final PackageManager packageManager = context.getPackageManager();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            Log.d(TAG, "SystemUI(), isMyLauncherDefault().......myPackageName: "+myPackageName+", activity.getPackageName(): "+activity.getPackageName());

            if (myPackageName.equals(activity.getPackageName())) {
                Log.d(TAG, "SystemUI(), isMyLauncherDefault().......returning true.......");

                return true;
            }
        }

        Log.d(TAG, "SystemUI(), isMyLauncherDefault().......returning false.......");

        return false;
    }*/

}

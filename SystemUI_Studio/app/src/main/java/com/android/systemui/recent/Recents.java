/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.systemui.recent;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.statusbar.phone.PhoneStatusBar;


public class Recents extends SystemUI implements RecentsComponent {
    private static final String TAG = "Recents";
    private static final boolean DEBUG = true;

    boolean mBootCompleted = false;

    @Override
    public void start() {
        putComponent(Recents.class, this);
    }

    @Override
    protected void onBootCompleted() {
        mBootCompleted = true;
    }

    @Override
    public void showRecents(boolean triggeredFromAltTab, boolean fromHome) {
    }


    @Override
    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        Intent intent = new Intent(RecentsActivity.CLOSE_RECENTS_INTENT);
        intent.setPackage("com.android.systemui");
        sendBroadcastSafely(intent);
        RecentTasksLoader.getInstance(mContext).cancelPreloadingFirstTask();
        onVisibilityChanged(mContext, false);
    }



    @Override
    public void toggleRecents(Display display) {
    }

    @Override
    public void toggleRecents(Display display, int layoutDirection, View statusBarView) {

        if (DEBUG) Log.d(TAG, "toggle recents panel");
        try {
            TaskDescription firstTask = RecentTasksLoader.getInstance(mContext).getFirstTask();

            Intent intent = new Intent(RecentsActivity.TOGGLE_RECENTS_INTENT);
            intent.setClassName("com.android.systemui",
                    "com.android.systemui.recent.RecentsActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            if (firstTask == null) {
                if (RecentsActivity.forceOpaqueBackground(mContext)) {
                    ActivityOptions opts = ActivityOptions.makeCustomAnimation(mContext,
                            R.anim.recents_launch_from_launcher_enter,
                            R.anim.recents_launch_from_launcher_exit);
                    mContext.startActivityAsUser(intent, opts.toBundle(), new UserHandle(
                            UserHandle.USER_CURRENT));
                } else {
                    // The correct window animation will be applied via the activity's style
                    mContext.startActivityAsUser(intent, new UserHandle(
                            UserHandle.USER_CURRENT));
                }

            } else {
                Bitmap first = null;
                if (firstTask.getThumbnail() instanceof BitmapDrawable) {
                    first = ((BitmapDrawable) firstTask.getThumbnail()).getBitmap();
                } else {
                    first = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    Drawable d = RecentTasksLoader.getInstance(mContext).getDefaultThumbnail();
                    d.draw(new Canvas(first));
                }
                final Resources res = mContext.getResources();

                float thumbWidth = res
                        .getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_width);
                float thumbHeight = res
                        .getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_height);
                if (first == null) {
                    throw new RuntimeException("Recents thumbnail is null");
                }
                if (first.getWidth() != thumbWidth || first.getHeight() != thumbHeight) {
                    first = Bitmap.createScaledBitmap(first, (int) thumbWidth, (int) thumbHeight,
                            true);
                    if (first == null) {
                        throw new RuntimeException("Recents thumbnail is null");
                    }
                }


                DisplayMetrics dm = new DisplayMetrics();
                display.getMetrics(dm);
                // calculate it here, but consider moving it elsewhere
                // first, determine which orientation you're in.
                final Configuration config = res.getConfiguration();
                int x, y;

                if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    float appLabelLeftMargin = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_app_label_left_margin);
                    float appLabelWidth = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_app_label_width);
                    float thumbLeftMargin = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_thumbnail_left_margin);
                    float thumbBgPadding = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_thumbnail_bg_padding);

                    float width = appLabelLeftMargin +
                            +appLabelWidth
                            + thumbLeftMargin
                            + thumbWidth
                            + 2 * thumbBgPadding;

                    x = (int) ((dm.widthPixels - width) / 2f + appLabelLeftMargin + appLabelWidth
                            + thumbBgPadding + thumbLeftMargin);
                    y = (int) (dm.heightPixels
                            - res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_height)
                            - thumbBgPadding);
                    if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                        x = dm.widthPixels - x - res.getDimensionPixelSize(
                                R.dimen.status_bar_recents_thumbnail_width);
                    }

                } else { // if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    float thumbTopMargin = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_thumbnail_top_margin);
                    float thumbBgPadding = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_thumbnail_bg_padding);
                    float textPadding = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_text_description_padding);
                    float labelTextSize = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_app_label_text_size);
                    Paint p = new Paint();
                    p.setTextSize(labelTextSize);
                    float labelTextHeight = p.getFontMetricsInt().bottom
                            - p.getFontMetricsInt().top;
                    float descriptionTextSize = res.getDimensionPixelSize(
                            R.dimen.status_bar_recents_app_description_text_size);
                    p.setTextSize(descriptionTextSize);
                    float descriptionTextHeight = p.getFontMetricsInt().bottom
                            - p.getFontMetricsInt().top;

                    float statusBarHeight = res.getDimensionPixelSize(
                            com.android.internal.R.dimen.status_bar_height);
                    float recentsItemTopPadding = statusBarHeight;

                    float height = thumbTopMargin
                            + thumbHeight
                            + 2 * thumbBgPadding + textPadding + labelTextHeight
                            + recentsItemTopPadding + textPadding + descriptionTextHeight;
                    float recentsItemRightPadding = res
                            .getDimensionPixelSize(R.dimen.status_bar_recents_item_padding);
                    float recentsScrollViewRightPadding = res
                            .getDimensionPixelSize(R.dimen.status_bar_recents_right_glow_margin);
                    x = (int) (dm.widthPixels - res
                            .getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_width)
                            - thumbBgPadding - recentsItemRightPadding
                            - recentsScrollViewRightPadding);
                    y = (int) ((dm.heightPixels - statusBarHeight - height) / 2f + thumbTopMargin
                            + recentsItemTopPadding + thumbBgPadding + statusBarHeight);
                }

                ActivityOptions opts = ActivityOptions.makeThumbnailScaleDownAnimation(
                        statusBarView,
                        first, x, y,
                        new ActivityOptions.OnAnimationStartedListener() {
                            public void onAnimationStarted() {
                                Intent intent =
                                        new Intent(RecentsActivity.WINDOW_ANIMATION_START_INTENT);
                                intent.setPackage("com.android.systemui");
                                sendBroadcastSafely(intent);
                            }
                        });
                intent.putExtra(RecentsActivity.WAITING_FOR_WINDOW_ANIMATION_PARAM, true);
                startActivitySafely(intent, opts.toBundle());
            }
            onVisibilityChanged(mContext, true);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to launch RecentAppsIntent", e);
        }
    }

    /**
     * This is only called from the system user's Recents.  Secondary users will instead proxy their
     * visibility change events through to the system user via
     * {@link Recents#onBusEvent(RecentsVisibilityChangedEvent)}.
     */
    public void onVisibilityChanged(Context context, boolean visible) {
        SystemUIApplication app = (SystemUIApplication) context;
        PhoneStatusBar statusBar = app.getComponent(PhoneStatusBar.class);
        if (statusBar != null) {
            if (DEBUG) Log.d(TAG, "updateRecentsVisibility = "+visible);
            statusBar.updateRecentsVisibility(visible);
        }
    }


    @Override
    public void preloadRecents() {
        Intent intent = new Intent(RecentsActivity.PRELOAD_INTENT);
        intent.setClassName("com.android.systemui",
                "com.android.systemui.recent.RecentsPreloadReceiver");
        sendBroadcastSafely(intent);
        RecentTasksLoader.getInstance(mContext).preloadFirstTask();
    }

    @Override
    public void cancelPreloadingRecents() {
        Intent intent = new Intent(RecentsActivity.CANCEL_PRELOAD_INTENT);
        intent.setClassName("com.android.systemui",
                "com.android.systemui.recent.RecentsPreloadReceiver");
        sendBroadcastSafely(intent);
        RecentTasksLoader.getInstance(mContext).cancelPreloadingFirstTask();
    }

    @Override
    public void showNextAffiliatedTask() {
    }

    @Override
    public void showPrevAffiliatedTask() {
    }

    @Override
    public void onDraggingInRecentsEnded(float velocity) {
    }

    @Override
    public boolean dockTopTask(int dragMode, int stackCreateMode, Rect initialBounds,
                               int metricsDockAction) {
        return false;

    }


    @Override
    public void onDraggingInRecents(float distanceFromTop) {

    }

    /**
     * Send broadcast only if BOOT_COMPLETED
     */
    private void sendBroadcastSafely(Intent intent) {
        if (!mBootCompleted) return;
        mContext.sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
    }

    /**
     * Start activity only if BOOT_COMPLETED
     */
    private void startActivitySafely(Intent intent, Bundle opts) {
        if (!mBootCompleted) return;
        mContext.startActivityAsUser(intent, opts, new UserHandle(UserHandle.USER_CURRENT));
    }
}

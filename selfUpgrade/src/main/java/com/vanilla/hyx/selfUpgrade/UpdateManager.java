/*******************************************************************************
 * Copyright 2016 Michael Hyx
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.vanilla.hyx.selfUpgrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateManager extends BroadcastReceiver {

    private static final String TAG = "UpdateManager";

    public static final int UPDATE_TYPE_DIALOG = 0;

    public static final int UPDATE_TYPE_NOTIFICATIONBAR = 1;

    public static final int HANDLER_SHCEDULE_UPDATE = 1;

    private Context mContext;

    private ProgressDialog mDownloadDialog;

    private AlertDialog mUpdateDialog;

    private String mAppKey;

    private String mServerURL;

    private int mDisplayMode = 0;

    private int mUpdateMode = 0;

    private int mProgress;

    private Timer mUpdateTimer = null;

    private TimerTask mUpdateTask = null;

    private Handler mHandler = null;

    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (UpdateConfig.INTENT_ACTION_UPDATE_PROGRESS.equals(action)) {
            int max = intent.getIntExtra(UpdateConfig.UPDATE_SIZE, 0);
            mProgress = intent.getIntExtra(UpdateConfig.UPDATE_PROGRESS, 0);
            mDownloadDialog.setMax(max);
            mDownloadDialog.setProgress(mProgress);
        } else if (UpdateConfig.INTENT_ACTION_UPDATE_FINISH.equals(action)) {
            int max = intent.getIntExtra(UpdateConfig.UPDATE_SIZE, 0);
            mDownloadDialog.setMax(max);
            mDownloadDialog.setProgress(max);
            mDownloadDialog.dismiss();
        }else if (UpdateConfig.INTENT_ACTION_UPDATE_CHECK_ERROR.equals(action)) {
            Toast.makeText(context, context.getString(R.string.package_update_apk_check_error), Toast.LENGTH_SHORT).show();
        } else if (UpdateConfig.INTENT_ACTION_MANAGER_RELEASE.equals(action)) {
            recycle();
        }
    }

    private UpdateManager() {

    }

    UpdateManager(Context context, String appKey, String serverURL, int mode) {
        mContext = context;
        mAppKey = appKey;
        mServerURL = serverURL;
        mDisplayMode = mode;
        initReceiver();
    }

    private void initReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdateConfig.INTENT_ACTION_UPDATE_PROGRESS);
        intentFilter.addAction(UpdateConfig.INTENT_ACTION_UPDATE_FINISH);
        intentFilter.addAction(UpdateConfig.INTENT_ACTION_UPDATE_CHECK_ERROR);
        intentFilter.addAction(UpdateConfig.INTENT_ACTION_MANAGER_RELEASE);
        mContext.registerReceiver(this, intentFilter);
    }

    public void setDisplayMode(int mode) {
        mDisplayMode = mode;
    }

    public void startScheduledUpdate(long start, long period) {
        if (!isDialogAvailable(mContext)) {
            Toast.makeText(mContext, mContext.getString(R.string.package_update_context_error), Toast.LENGTH_SHORT).show();
        }
        initScheduledUpdate();
        resetTimer();
        mUpdateTimer = new Timer();
        mUpdateTimer.schedule(mUpdateTask, start*1000, period*1000);
    }

    public void cancelScheduledUpdate() {
        interceptDownload();
        resetTimer();
    }

    private void initScheduledUpdate() {
        if (mUpdateTask == null) {
            mUpdateTask = new TimerTask() {
                public void run() {
                    if (NetworkUtils.isNetworkAvailable(mContext)) {
                        long time = System.currentTimeMillis();
                        Log.d(TAG, "Scheduled update: " + (new Date(time)).toString());
                        //NetworkUtils.getUpdateInfo(mServerURL, mEntityID, mAppKey, getVersionCode());
                        checkScheduledUpdate(UpdateConfig.TEST_JSON, UPDATE_TYPE_DIALOG);
                    }
                }
            };
        }
        if (mHandler == null) {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case HANDLER_SHCEDULE_UPDATE:
                            Bundle bundle = msg.getData();
                            UpdateInfo info = (UpdateInfo) bundle.get("info");
                            int type = bundle.getInt("type");
                            startUpdate(info, type, true);
                            break;
                    }
                    super.handleMessage(msg);
                }
            };
        }
    }

    private void resetTimer() {
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mUpdateTimer = null;
        }
    }

    private void interceptDownload() {
        Intent intent = new Intent(UpdateConfig.INTENT_ACTION_UPDATE_INTERCEPT);
        mContext.sendBroadcast(intent);
    }

    /**
     * Recycle resources，called in onDestroy() method of Activity
     */
    public void recycle() {
        if (mHandler != null) {
            mHandler.removeMessages(HANDLER_SHCEDULE_UPDATE);
            mHandler = null;
        }
        cancelScheduledUpdate();
        mContext.unregisterReceiver(this);
        Log.d(TAG, "Recycled!!!");
    }

    /**
     * Show update notice in dialog.
     */
    public void checkUpdateForDialog() {
        if (NetworkUtils.isNetworkAvailable(mContext)) {
            if (isDialogAvailable(mContext)) {
                new CheckUpdateTask(mContext, UPDATE_TYPE_DIALOG, true).execute();
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.package_update_context_error), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.package_update_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show update notice in notification.
     */
    public void checkUpdateForNotification() {
        if (NetworkUtils.isNetworkAvailable(mContext)) {
            new CheckUpdateTask(mContext, UPDATE_TYPE_NOTIFICATIONBAR, false).execute();
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.package_update_network_error), Toast.LENGTH_SHORT).show();
        }
    }

    public int getVersionCode() {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
            } catch (PackageManager.NameNotFoundException ignored) {

            }
        }
        return 0;
    }

    public String getVersionName() {
        if (mContext != null) {
            try {
                return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignored) {

            }
        }
        return "";
    }

    private boolean isDialogAvailable(Context context) {
        return context != null && context instanceof Activity && !((Activity) context).isFinishing();
    }

    private void showValidDialog(Context context, Dialog dialog) {
        if (isDialogAvailable(context) && dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    private void showUpdateDialog() {
        if (mDownloadDialog != null) {
            if (mDownloadDialog.isShowing()) {
                mDownloadDialog.dismiss();
            }
            mDownloadDialog.setMax(0);
            mDownloadDialog.setProgress(0);
        } else {
            mDownloadDialog = new ProgressDialog(mContext);
        }

        // mDownloadDialog.setIcon(R.drawable.download_icon);
        mDownloadDialog.setTitle(R.string.package_update_dialog_title);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        if (mDisplayMode == UpdateConfig.DISPLAY_MODE_SIZE) {
            mDownloadDialog.setProgressNumberFormat("%1d kb/%2d kb");
        } else {
            mDownloadDialog.setProgressNumberFormat("%1d/%2d");
        }
//        mDownloadDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Confirm",  new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int whichButton) {
//
//            }
//        });
        mDownloadDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",  new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                interceptDownload();
            }
        });
        showValidDialog(mContext, mDownloadDialog);
    }

    private void showDialog(final Context context, final UpdateInfo info) {
        String content = info.getUpdateMsg();
        if (mUpdateDialog == null) {
            mUpdateDialog = new AlertDialog.Builder(context).create();
        }
        if (!mUpdateDialog.isShowing()) {
            mUpdateDialog.setTitle(R.string.package_update_dialog_title);
            mUpdateDialog.setMessage(Html.fromHtml(content));
            mUpdateDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.package_dialog_btn_download), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    Intent intent = new Intent(context, DownloadService.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(UpdateConfig.UPDATE_INFO, info);
                    intent.putExtras(bundle);
                    intent.putExtra(UpdateConfig.UPDATE_DISPLAY_MODE, mDisplayMode);
                    intent.putExtra(UpdateConfig.UPDATE_TYPE, UPDATE_TYPE_DIALOG);
                    mContext.startService(intent);
                    showUpdateDialog();
                }
            });
            mUpdateDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.package_dialog_btn_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            mUpdateDialog.setCanceledOnTouchOutside(false);
            showValidDialog(mContext, mUpdateDialog);
        }
    }

    private void showNotification(Context context, UpdateInfo info) {
        String content = info.getUpdateMsg();
        Intent intent = new Intent(context, DownloadService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        bundle.putSerializable(UpdateConfig.UPDATE_INFO, info);
        intent.putExtras(bundle);
        intent.putExtra(UpdateConfig.UPDATE_DISPLAY_MODE, mDisplayMode);
        intent.putExtra(UpdateConfig.UPDATE_TYPE, UPDATE_TYPE_NOTIFICATIONBAR);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        int smallIcon = context.getApplicationInfo().icon;
        Notification notify = new NotificationCompat.Builder(context)
                .setTicker(context.getString(R.string.package_update_notify_ticker))
                .setContentTitle(context.getString(R.string.package_update_notify_content))
                .setContentText(content)
                .setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent).build();

        notify.flags = android.app.Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notify);
    }

    private UpdateInfo parseJson(String result) {
        try {
            JSONObject data = new JSONObject(result);
            int code = data.getInt("code");
            if (code == 1) {
                JSONObject content = data.getJSONObject("data");
                String md5 = content.optString("md5");
                String url = content.optString("url");
                String updateMsg = content.optString("updateMsg");
                boolean isForceUpdate = content.optInt("isForceUpdate") == 1 ? true : false;
                boolean isPatch = content.optInt("isPatch") == 1 ? true : false;
                int version = content.optInt("version");
                UpdateInfo info = new UpdateInfo(version, url, md5, updateMsg, isForceUpdate, isPatch);
                return info;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void startUpdate(UpdateInfo info, int type, boolean isScheduled) {
        int currentVersion = getVersionCode();
        if (info.getVersionCode() > currentVersion) {
            if (type == UPDATE_TYPE_NOTIFICATIONBAR) {
                showNotification(mContext, info);
            } else if (type == UPDATE_TYPE_DIALOG) {
                showDialog(mContext, info);
            }
        } else if (!isScheduled){
            Toast.makeText(mContext, mContext.getString(R.string.package_toast_no_new_update), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkUpdate(String result, int type) {
        if (!TextUtils.isEmpty(result)) {
            UpdateInfo info = parseJson(result);
            if (info != null) {
                startUpdate(info, type, false);
            } else {
                Toast.makeText(mContext, mContext.getString(R.string.package_update_error), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.package_update_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkScheduledUpdate(String result, int type) {
        if (!TextUtils.isEmpty(result)) {
            UpdateInfo info = parseJson(result);
            if (info != null) {
                Message msg = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putSerializable("info", info);
                bundle.putInt("type", type);
                msg.setData(bundle);
                msg.what = HANDLER_SHCEDULE_UPDATE;
                mHandler.sendMessage(msg);
            }
        }
    }

    private class CheckUpdateTask extends AsyncTask<Void, Void, String> {

        private int mType;
        private boolean mIsShowCheckDialog;
        private Context mInnerContext;
        private ProgressDialog mCheckDialog;

        CheckUpdateTask(Context context, int type, boolean isShowCheckDialog) {
            this.mInnerContext = context;
            this.mType = type;
            this.mIsShowCheckDialog = isShowCheckDialog;
        }

        @Override
        protected void onPreExecute() {
            if (mIsShowCheckDialog) {
                if (mCheckDialog == null) {
                    mCheckDialog = new ProgressDialog(mInnerContext);
                    mCheckDialog.setIndeterminate(true);
                    mCheckDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mCheckDialog.setMessage(mInnerContext.getString(R.string.package_update_dialog_checking));
                }
                showValidDialog(mInnerContext, mCheckDialog);
            }
        }

        @Override
        protected String doInBackground(Void... args) {
            return UpdateConfig.TEST_JSON;
            //return NetworkUtils.getUpdateInfo(UpdateConfig.UPDATE_INFO_URL, mAppKey, getVersionCode());
        }

        @Override
        protected void onPostExecute(String result) {
            if (mCheckDialog != null && mCheckDialog.isShowing()) {
                mCheckDialog.dismiss();
            }
            checkUpdate(result, mType);
        }
    }
}

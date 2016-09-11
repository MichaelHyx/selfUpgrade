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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.Map;

public class DownloadService extends IntentService {

    private static final String TAG = "DownloadService";
    private static final String updateFileName = "update.apk";
    private static final int BUFFER_SIZE = 50 * 1024; //Refresh progress bar per 50 kbs.
    private static final int UPDATE_CHUNK = 200 * 1024; //Update progress when new accumulated chunk larger than 200kbs.
    private static final int NOTIFICATION_ID = 0;

    private NotificationManager mNotifyManager;
    private Builder mBuilder;
    private int mDisplayMode = 0;
    private boolean mIsIntercepted = false;
    private boolean mIsForceUpdate = false;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (UpdateConfig.INTENT_ACTION_UPDATE_INTERCEPT.equals(action) && !mIsForceUpdate) {
                mIsIntercepted = true;
            }
        }
    };

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdateConfig.INTENT_ACTION_UPDATE_INTERCEPT);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public  void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        recycleManager();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) return;
        UpdateInfo info = (UpdateInfo) intent.getSerializableExtra(UpdateConfig.UPDATE_INFO);
        if (info == null) return;

        boolean isPatch = info.isPatch(); //Is patch file, not used yet.
        String urlStr = info.getUrl();
        String md5 = info.getCheckSum();
        mIsForceUpdate = info.isForceUpdate();
        mDisplayMode = intent.getIntExtra(UpdateConfig.UPDATE_DISPLAY_MODE, 0);
        int type = intent.getIntExtra(UpdateConfig.UPDATE_TYPE, 0);
        if (type == UpdateManager.UPDATE_TYPE_NOTIFICATIONBAR) {
            initNotificationUpdate();
        }
        InputStream in = null;
        FileOutputStream out = null;
        int downloadedSize = 0; //Previous downloaded file size.
        int totalSize = 0; //End position in file bytes.
        long fileLength; //Length of the file.
        boolean isContinue = false; //Is continue previous download.
        Map<String, String> result = StorageUtils.getDownloadInfo(this);
        try {
            File dir = StorageUtils.getCacheDirectory(this);
            //String apkName = urlStr.substring(urlStr.lastIndexOf("/") + 1, urlStr.length());
            String apkName = updateFileName;
            File apkFile = new File(dir, apkName);
            if (TextUtils.equals(result.get("currentVersion"), String.valueOf(info.getVersionCode())) &&
                    TextUtils.equals(result.get("currentURL"), String.valueOf(info.getUrl())) &&
                    apkFile.length() == Long.valueOf(result.get("downloadedSize"))) {
                isContinue = true;
                downloadedSize = Integer.valueOf(result.get("downloadedSize"));
                totalSize = Integer.valueOf(result.get("totalSize"));
                if (downloadedSize == totalSize) {
                    if (!InstallUtils.checkMD5(apkFile, md5)) {
                        downloadedSize = 0;
                        isContinue = false;
                    } else {
                        updateProgress(totalSize, totalSize, type);
                        if (isPatch) {
                            InstallUtils.installAPKPatch(this, apkFile, md5); //Patched update.
                        } else {
                            InstallUtils.installAPK(this, apkFile, md5); //Normal update.
                        }
                        return;
                    }
                }
            }

            HttpURLConnection urlConnection = NetworkUtils.getConnection(urlStr, 10);
            if (isContinue) {
                urlConnection.setRequestProperty("Range", "bytes=" + downloadedSize + "-" + totalSize);
                urlConnection.connect();
                fileLength = totalSize;
            } else {
                urlConnection.connect();
                fileLength = urlConnection.getContentLength();
            }
            //NetworkUtils.printResponseHeader(urlConnection);
            in = urlConnection.getInputStream();
            RandomAccessFile randomAccessFile = new RandomAccessFile(apkFile.getAbsolutePath(), "rwd");
            Log.d(TAG, "start--: " + downloadedSize);
            randomAccessFile.seek(downloadedSize);
            //out = new FileOutputStream(apkFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int progress;
            int currentBytes;
            int oldProgress = downloadedSize;
            long totalBytes = downloadedSize;
            while (!mIsIntercepted && (currentBytes = in.read(buffer)) != -1) {
                totalBytes += currentBytes;
                randomAccessFile.write(buffer, 0, currentBytes);
                //out.write(buffer, 0, currentBytes);
                progress = (int) totalBytes;
                if (progress - oldProgress > UPDATE_CHUNK) {
                    updateProgress((int) fileLength, progress, type);
                    oldProgress = progress;
                }
            }
            Log.d(TAG, "end--: " + (int) randomAccessFile.length());
            StorageUtils.saveDownloadInfo(this, info.getVersionCode(), info.getUrl(), (int) randomAccessFile.length(), (int) fileLength);
            urlConnection.disconnect();
            if (!mIsIntercepted) {
                updateProgress((int) fileLength, (int) fileLength, type); //Finish download.
                if (isPatch) {
                    InstallUtils.installAPKPatch(this, apkFile, md5); //Patched update.
                } else {
                    InstallUtils.installAPK(this, apkFile, md5); //Normal update.
                }
            }

            if (type == UpdateManager.UPDATE_TYPE_NOTIFICATIONBAR) {
                mNotifyManager.cancel(NOTIFICATION_ID);
            }
        } catch (Exception e) {
            Log.e(TAG, "download apk file error");
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {

                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {

                }
            }
        }
    }

    private void initNotificationUpdate() {
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        String appName = getString(getApplicationInfo().labelRes);
        int icon = getApplicationInfo().icon;
        mBuilder.setContentTitle(appName).setSmallIcon(icon);
    }

    private void updateProgress(int max, int progress, int type) {
        Log.d(TAG, "---updateProgress---: " + "max: " + max + "current: " + progress);
        if (mDisplayMode == UpdateConfig.DISPLAY_MODE_PERCENTAGE) { //Show download progress in percentage.
            progress = progress * 100 / max;
            max = 100;
        } else if (mDisplayMode == UpdateConfig.DISPLAY_MODE_SIZE){ //Show download progress in size of kb.
            progress = progress / 1024;
            max = max / 1024;
        }
        if (type == UpdateManager.UPDATE_TYPE_NOTIFICATIONBAR) {
            if (mDisplayMode == UpdateConfig.DISPLAY_MODE_PERCENTAGE) {
                mBuilder.setContentText(this.getString(R.string.package_update_download_progress, progress)).setProgress(max, progress, false);
            } else {
                mBuilder.setContentText(this.getString(R.string.package_update_download_progress_size, progress, max)).setProgress(max, progress, false);
            }
            PendingIntent pendingintent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setContentIntent(pendingintent);
            mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
        } else if (type == UpdateManager.UPDATE_TYPE_DIALOG){
            if (progress != max) {
                Intent intent = new Intent(UpdateConfig.INTENT_ACTION_UPDATE_PROGRESS);
                intent.putExtra(UpdateConfig.UPDATE_PROGRESS, progress);
                intent.putExtra(UpdateConfig.UPDATE_SIZE, max);
                sendBroadcast(intent);
            } else {
                Intent intent = new Intent(UpdateConfig.INTENT_ACTION_UPDATE_FINISH);
                intent.putExtra(UpdateConfig.UPDATE_SIZE, max);
                sendBroadcast(intent);
            }
        }
    }

    private void recycleManager() {
        Intent intent = new Intent(UpdateConfig.INTENT_ACTION_MANAGER_RELEASE);
        sendBroadcast(intent);
    }
}
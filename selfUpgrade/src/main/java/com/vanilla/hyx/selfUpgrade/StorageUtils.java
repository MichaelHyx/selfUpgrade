/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Provides application storage paths
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.0.0
 */
final class StorageUtils {

    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";

    private static final String TAG = "StorageUtils";

    private StorageUtils() {
    }

    /**
     * Returns application cache directory. Cache directory will be created on SD card
     * <i>("/Android/data/[app_package_name]/cache")</i> if card is mounted and app has appropriate permission. Else -
     * Android defines cache directory on device's file system.
     *
     * @param context Application context
     * @return Cache {@link File directory}
     */
    public static File getCacheDirectory(Context context) {
        File appCacheDir = null;
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
            Log.w(TAG, "Can't define system cache directory! The app should be re-installed.");
        }
        return appCacheDir;
    }


    private static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                Log.w(TAG, "Unable to create external cache directory");
                return null;
            }
            try {
                new File(appCacheDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                Log.i(TAG, "Can't create \".nomedia\" file in application external cache directory");
            }
        }
        return appCacheDir;
    }

    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 保存已下载安装包信息
     * @param context
     * @param currentVersion 安装包版本
     * @param currentURL 安装包下载地址
     * @param downloadedSize 安装包已下载大小
     * @param totalSize 安装包大小
     */
    public static void saveDownloadInfo(Context context, int currentVersion, String currentURL, int downloadedSize, int totalSize) {
        SharedPreferences sharedPreferences= context.getSharedPreferences("updateInfo", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("currentVersion", String.valueOf(currentVersion));
        editor.putString("currentURL", currentURL);
        editor.putString("downloadedSize", String.valueOf(downloadedSize));
        editor.putString("totalSize", String.valueOf(totalSize));
        editor.commit();
    }

    /**
     *获取已下载安装包信息，只包含最新一次下载的安装包信息
     * @param context
     * @return
     */
    public static Map<String, String> getDownloadInfo(Context context) {
        Map<String, String> result = new HashMap<String, String>();
        SharedPreferences sharedPreferences= context.getSharedPreferences("updateInfo", Activity.MODE_PRIVATE);
        result.put("currentVersion", sharedPreferences.getString("currentVersion", "0"));
        result.put("currentURL", sharedPreferences.getString("currentURL", ""));
        result.put("downloadedSize", sharedPreferences.getString("downloadedSize", "0"));
        result.put("totalSize", sharedPreferences.getString("totalSize", "0"));
        return result;
    }
}

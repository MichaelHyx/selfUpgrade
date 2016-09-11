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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class InstallUtils {

    static{
        System.loadLibrary("bsdiffjni");
    }

    public static native int bsdiff(String oldFilePath, String newFilePath, String patchFilePath);

    public static native int bspatch(String oldFilePath, String newFilePath, String patchFilePath);

    /**
     * Install bsPatch file
     * @param context
     * @param patchFile
     * @param md5
     */
    public static void installAPKPatch(Context context, File patchFile, String md5) {
        String packageName = context.getPackageName();
        String path1 ="/data/app/" + packageName + "-1.apk";
        String path2 ="/data/app/" + packageName + "-2.apk";
        File file = new File(path1);
        if (!file.exists() || !file.canRead()) {
            file = new File(path2);
            if(!file.exists() || !file.canRead()) {
                return;
            }
        }

        String oldFilePath = file.getAbsolutePath();

        if(!patchFile.exists() || !patchFile.canRead()) {
            return ;
        }
        String updateFilePath = StorageUtils.getCacheDirectory(context).getAbsolutePath() + "update.apk";
        if (bsdiff(oldFilePath, patchFile.getAbsolutePath(), updateFilePath ) != 0) {
            return;
        }
        installAPK(context, new File(updateFilePath), md5);
    }

    /**
     * Install package
     * @param context
     * @param apkFile
     * @param md5
     */
    public static void installAPK(Context context, File apkFile, String md5) {
        if (apkFile != null && apkFile.exists() && !checkMD5(apkFile, md5)) {
            Intent intent = new Intent(UpdateConfig.INTENT_ACTION_UPDATE_CHECK_ERROR);
            context.sendBroadcast(intent);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            String[] command = {"chmod", "777", apkFile.toString()};
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
        } catch (IOException ignored) {
        }
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean checkMD5(File file, String md5) {
        try {
            InputStream stream = new FileInputStream(file);
            if (!TextUtils.isEmpty(md5) && md5.equalsIgnoreCase(calculateMD5(stream))) return true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
       return false;
    }

    public static String calculateMD5(InputStream is) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(is.hashCode());
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}

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

import java.io.Serializable;

class UpdateInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * APK versionCode code
     */
    private int versionCode;

    /**
     * APK versionCode name
     */
    private String versionName;

    /**
     * APK checkSum
     */
    private String checkSum;

    /**
     * Download URL of the package
     */
    private String url;

    /**
     * Update message
     */
    private String updateMsg;

    /**
     * Is force update which download packages in background without notice
     */
    private boolean isForceUpdate;

    /**
     * Is bsPatch file
     */
    private boolean isPatch;

    public UpdateInfo(int versionCode, String url, String checkSum, String updateMsg, boolean isForceUpdate, boolean isPatch) {
        this.versionCode = versionCode;
        this.url = url;
        this.checkSum = checkSum;
        this.updateMsg = updateMsg;
        this.isForceUpdate = isForceUpdate;
        this.isPatch = isPatch;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getUpdateMsg() {
        return updateMsg;
    }

    public void setUpdateMsg(String updateMsg) {
        this.updateMsg = updateMsg;
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        isForceUpdate = forceUpdate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isPatch() {
        return isPatch;
    }

    public void setPatch(boolean patch) {
        isPatch = patch;
    }
}

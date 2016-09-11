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

public class UpdateConfig {

    /**
     * Show download progress in percentage(0% to 100%)
     */
    public static final int DISPLAY_MODE_PERCENTAGE = 0;

    /**
     * Show download progress in downloaded size(kb)
     */
    public static final int DISPLAY_MODE_SIZE = 1;

    static final String UPDATE_INFO = "update_info";
    static final String UPDATE_TYPE = "type";
    static final String UPDATE_PROGRESS = "progress";
    static final String UPDATE_DISPLAY_MODE = "display_mode";
    static final String UPDATE_SIZE = "apk_size";
    static final String INTENT_ACTION_UPDATE_PROGRESS = "update_progress";
    static final String INTENT_ACTION_UPDATE_FINISH = "update_finish";
    static final String INTENT_ACTION_UPDATE_INTERCEPT = "update_intercept";
    static final String INTENT_ACTION_UPDATE_CHECK_ERROR = "md5_error";
    static final String INTENT_ACTION_MANAGER_RELEASE = "manager_release";

    static final String UPDATE_INFO_URL = "http://10.1.4.128:8080/check_update/v1/auto_update_cashier_version";

    static final String TEST_JSON = "{\"code\":1,\"data\":{\"url\":\"http://s1.music.126.net/download/android/CloudMusic_official_3.6.0.143641.apk\",\"version\":2,\"updateMsg\":\"网易云音乐 听见好时光\",\"md5\":\"51761ec1fb8656a2a5919ed92b8cc88c\",\"isForceUpdate\":false}}";
}

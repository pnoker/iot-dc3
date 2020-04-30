/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.dc3.transfer.rtmp.bean;

/**
 * @author pnoker
 */
public class RtmpProperty {
    public static String FFMPEG;
    public static int RECONNECT_INTERVAL;
    public static int RECONNECT_MAX_TIMES;
    public static int CORE_POOL_SIZE;
    public static int MAX_POOL_SIZE;
    public static int KEEP_ALIVE_TIME;

    public static void initial(String ffmpeg, int reconnectInterval, int reconnectMaxTimes, int corePoolSize, int maximumPoolSize, int keepAliveTime) {
        RtmpProperty.FFMPEG = ffmpeg;

        RtmpProperty.RECONNECT_INTERVAL = reconnectInterval;
        RtmpProperty.RECONNECT_MAX_TIMES = reconnectMaxTimes;

        RtmpProperty.CORE_POOL_SIZE = corePoolSize;
        RtmpProperty.MAX_POOL_SIZE = maximumPoolSize;
        RtmpProperty.KEEP_ALIVE_TIME = keepAliveTime;
    }
}

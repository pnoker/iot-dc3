/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.transfer.rtmp.bean;

import io.github.pnoker.common.model.Rtmp;
import io.github.pnoker.common.utils.Dc3Util;
import io.github.pnoker.transfer.rtmp.init.TranscodeRunner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Command 指令执行任务信息类
 *
 * @author pnoker
 */
@Data
@Slf4j
public class Transcode {
    private String id;

    private boolean run;
    private String command;
    private Process process;
    public final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Transcode(Rtmp rtmp) {
        this.id = rtmp.getId();
        this.run = false;
        this.command = rtmp.getCommand()
                .replace("{exe}", TranscodeRunner.ffmpeg)
                .replace("{rtsp_url}", rtmp.getRtspUrl())
                .replace("{rtmp_url}", rtmp.getRtmpUrl());
    }

    public boolean isRun() {
        boolean result;
        this.lock.readLock().lock();
        result = run;
        this.lock.readLock().unlock();
        return result;
    }

    public void setRun(boolean run) {
        this.lock.writeLock().lock();
        this.run = run;
        this.lock.writeLock().unlock();
    }

    public void quit() {
        Dc3Util.destroyProcessWithCmd(process, "q");
        process = null;
        setRun(false);
    }

}

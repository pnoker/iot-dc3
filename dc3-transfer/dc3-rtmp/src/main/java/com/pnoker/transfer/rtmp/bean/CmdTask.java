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
package com.pnoker.transfer.rtmp.bean;

import com.pnoker.common.utils.Tools;
import com.pnoker.transfer.rtmp.handle.OutputHandle;
import lombok.Data;

import java.io.IOException;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Command 指令执行任务信息类
 */
@Data
public class CmdTask {
    /**
     * Command 指令任务 UUID
     */
    private String id;

    /**
     * 任务运行状态
     * <p>
     * 0：初始化完成，等待被启动 <br>
     * 1：启动完毕，正在运行中 <br>
     * 2：任务错误，等待被重启 <br>
     * 3：多次重启失败，任务已停止 <br>
     */
    private int status = 0;

    /**
     * 任务累计被启动次数
     */
    private int startTimes = 0;

    /**
     * Cmd 命令内容
     */
    private String command;

    private Process process;
    private OutputHandle outputHandle;

    public CmdTask(String command) {
        this.id = Tools.uuid();
        this.command = command;
    }

    public void start() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        process = runtime.exec(command);
        outputHandle = new OutputHandle(id, process);
        new Thread(outputHandle).start();
    }
}

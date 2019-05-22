package com.pnoker.rtmp.bean;

import com.pnoker.rtmp.handle.OutputHandle;
import lombok.Data;

import java.io.IOException;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 任务描述实体类
 */
@Data
public class Tasker {
    private String taskId;
    /**
     * 任务运行状态
     * <p>
     * 0：初始化完成，等待被启动 <br>
     * 1：启动完毕，正在运行中 <br>
     * 2：任务错误，等待被重启 <br>
     * 3：多次重启失败，任务已停止 <br>
     */
    private Integer status;
    /**
     * 任务被启动次数
     * <p>
     * 启动失败，会累计次数 <br>
     * 重启成功，会清除计数 <br>
     */
    private Integer times;
    private String command;
    private Process process;
    private OutputHandle outputHandle;

    public Tasker(String taskId, String command) {
        this.taskId = taskId;
        this.command = command;
    }

    public void start() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        process = runtime.exec(command);
        outputHandle = new OutputHandle(taskId, process);
        new Thread(outputHandle).start();
    }
}

package com.pnoker.rtsp.bean;

import com.pnoker.rtsp.handle.OutputHandle;
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
    private String id;
    private String command;
    private Process process;
    private OutputHandle outputHandle;

    public Tasker(String id, String command) {
        this.id = id;
        this.command = command;
    }

    public void start() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        this.process = runtime.exec(this.command);
        this.outputHandle = new OutputHandle(this.process);
        new Thread(this.outputHandle).start();
    }
}

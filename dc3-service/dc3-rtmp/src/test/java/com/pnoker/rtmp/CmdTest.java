package com.pnoker.rtmp;

import com.pnoker.common.utils.uid.UidTools;
import com.pnoker.rtmp.bean.Cmd;
import com.pnoker.rtmp.bean.Task;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;


/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
public class CmdTest {
    @Test
    public void createCmdTask() {
        Cmd cmd = new Cmd("");
        cmd.create("ffmpeg")
                .add("-rtsp_transport", "tcp")
                .add("-i", "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov")
                .add("-vcodec", "copy")
                .add("-acodec", "copy")
                .add("-f", "flv")
                .add("-y", "rtmp://114.116.9.76:1935/rtmp/bigbuckbunny_175k").build();
        Task task = new Task(new UidTools().guid(), cmd.getCmd());
        try {
            task.start();
            log.info("Cmd任务创建成功");
            log.info(task.getCommand());
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        task.getProcess().destroyForcibly();
        log.info("Cmd任务关闭成功");
    }
}

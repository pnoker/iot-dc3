package com.pnoker.rtmp.runner;

import com.pnoker.rtmp.bean.CommandTask;
import com.pnoker.rtmp.bean.Tasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@Order(1)
@Component
public class TeskRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
        try {
            while (true) {
                Tasker tasker = CommandTask.taskQueue.take();
                log.info("starting task {} , command {}", tasker.getTaskId(), tasker.getCommand());
                tasker.start();
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}

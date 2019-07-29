package com.pnoker.device.virtual.client;

import com.pnoker.device.virtual.model.ClientSocket;
import com.pnoker.device.virtual.util.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Copyright(c) 2019. Pnoker All Rights Reserved.
 *
 * <p>Author : Charles
 *
 * <p>Email : xinguangduan@163.com
 *
 * <p>Description: 消息发送者
 */
@Slf4j
@Component
@Order(2)
public class MessageSender implements ApplicationRunner {

  private static final String FILE_PATH = "classpath:messages";

  @Value("${remote.server.address}")
  private String serverAddress;

  @Value("${remote.server.port}")
  private Integer serverPort;

  @Value("${virtual.device.send-interval}")
  private Integer sendInterval;

  private volatile boolean isConnected;

  private BlockingQueue<String> messageQueue = new ArrayBlockingQueue<>(100000);

  /** 会在服务启动完成后立即执行 */
  @Override
  public void run(ApplicationArguments args) {
    initializedMockMessage();
    startMessageSender();
  }

  public void startMessageSender() {
    ClientSocket client = new ClientSocket();
    client.setMessageQueue(messageQueue);
    client.setServerAddress(serverAddress);
    client.setServerPort(serverPort);
    client.setSendInterval(sendInterval);
    new Thread(new MessageSenderThread(client)).start();
  }

  /** read mock data and send to queue */
  private void initializedMockMessage() {
    final File[] files = FileReaderUtils.getFiles(FILE_PATH);
    if (ArrayUtils.isEmpty(files)) {
      log.warn("mock data files is empty!");
      return;
    }

    for (File file : files) {
      try {
        final List<String> lines = FileUtils.readLines(file, "UTF-8");
        lines.forEach(
            s -> {
              int startStr = "报文是:".length();
              int endStr = s.indexOf(" ");
              final String mockData = s.substring(startStr, endStr);
              if (log.isDebugEnabled()) {
                log.debug("read the mock data :{}", mockData);
              }
              // add mock data to queue
              messageQueue.add(mockData);
            });
      } catch (IOException e) {
        log.error("read and deal line string error", e);
      }
    }
  }
}

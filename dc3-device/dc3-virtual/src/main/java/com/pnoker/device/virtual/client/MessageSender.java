package com.pnoker.device.virtual.client;

import com.pnoker.device.virtual.util.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

@Slf4j
@Component
@Order(1)
public class MessageSender implements ApplicationRunner {

  @Autowired private FileReaderUtils fileReaderUtils;

  private String filePathRoot = "classpath:messages";

  @Value("${remote.server.address}")
  private String remoteServerAddress;

  @Value("${remote.server.port}")
  private Integer remoteServerPort;

  @Value("${virtual.device.send-interval}")
  private Integer sendInterval;

  private boolean isConnected;

  private BlockingQueue<String> messageQueue;

  {
    final ArrayBlockingQueue<String> strings = new ArrayBlockingQueue<>(100000);
    messageQueue = strings;
  }

  /** 会在服务启动完成后立即执行 */
  @Override
  public void run(ApplicationArguments args) {
    initializedMockMessage();
    runMessageSender();
  }

  public void runMessageSender() {
    ClientDTO clientDTO = new ClientDTO();
    clientDTO.setMessageQueue(messageQueue);
    clientDTO.setName("client");
    clientDTO.setServerAddress(remoteServerAddress);
    clientDTO.setServerPort(remoteServerPort);
    clientDTO.setSendInterval(sendInterval);
    new Thread(new MessageSenderThread(clientDTO)).start();
  }

  /** read mock data and send to queue */
  private void initializedMockMessage() {
    File[] files = fileReaderUtils.getFiles(filePathRoot);

    if (ArrayUtils.isEmpty(files)) {
      log.warn("mock data files is empty!");
      return;
    }

    for (File file : files) {
      try {
        List<String> lines = FileUtils.readLines(file, "UTF-8");

        lines.forEach(
            s -> {
              int startStr = "报文是:".length();
              int endStr = s.indexOf(" ");
              final String line = s.substring(startStr, endStr);

              log.debug("read data :{}", line);
              messageQueue.add(line);
            });

      } catch (IOException e) {
        log.error("read and deal line string error", e);
      }
    }
  }
  /**
   * 向服务器端发送信息
   *
   * @param message
   */
  public void sendMessage(String message) {
    //    try {
    //
    //      clientSocket.getOutputStream().write(message.getBytes("utf-8"));
    //      // clientSocket.getOutputStream().writeUTF(message);
    //    } catch (IOException e) {
    //      log.error("发送信息异常：{}", e);
    //      close(clientSocket);
    //    }
  }
}

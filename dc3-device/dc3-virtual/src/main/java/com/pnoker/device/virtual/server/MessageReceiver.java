package com.pnoker.device.virtual.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(2)
public class MessageReceiver implements ApplicationRunner {

  @Value("${remote.server.port}")
  private Integer port = null;

  /** 会在服务启动完成后立即执行 */
  @Override
  public void run(ApplicationArguments args) {
    new Thread(new MessageReceiverThread(port)).start();
  }
}

package com.pnoker.device.virtual.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Copyright(c) 2019. Pnoker All Rights Reserved.
 *
 * <p>Author : Charles
 *
 * <p>Email : xinguangduan@163.com
 *
 * <p>Description: 消息接收者
 */
@Component
@Slf4j
@Order(1)
public class MessageReceiver implements ApplicationRunner {

  @Value("${remote.server.port}")
  private Integer port = null;

  /** 会在服务启动完成后立即执行 */
  @Override
  public void run(ApplicationArguments args) {
    new Thread(new MessageReceiverThread(port)).start();
  }
}

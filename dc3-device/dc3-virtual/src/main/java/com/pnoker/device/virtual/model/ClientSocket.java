package com.pnoker.device.virtual.model;

import lombok.Data;

import java.util.concurrent.BlockingQueue;
/**
 * Copyright(c) 2019. Pnoker All Rights Reserved.
 *
 * <p>Author : Charles
 *
 * <p>Email : xinguangduan@163.com
 *
 * <p>Description: 客户端连接服务器端信息
 */
@Data
public class ClientSocket {
  private String serverAddress;
  private int serverPort;
  private int sendInterval;
  private BlockingQueue<String> messageQueue;
}

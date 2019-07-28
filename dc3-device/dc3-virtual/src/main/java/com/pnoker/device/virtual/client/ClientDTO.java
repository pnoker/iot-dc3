package com.pnoker.device.virtual.client;

import lombok.Data;

import java.util.concurrent.BlockingQueue;

@Data
public class ClientDTO {
  private String name;
  private String serverAddress;
  private int serverPort;
  private int sendInterval;
  private BlockingQueue<String> messageQueue;
}

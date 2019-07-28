package com.pnoker.device.virtual.client;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static com.pnoker.device.virtual.constant.ProtocolConstant.MSG_BEGIN;
import static com.pnoker.device.virtual.constant.ProtocolConstant.MSG_END;

/**
 * Copyright(c) 2019. Pnoker All Rights Reserved.
 *
 * <p>Author : Charles
 *
 * <p>Email : dxgvip@gmail.com
 *
 * <p>Description: 消息发送线程
 */
@Slf4j
public class MessageSenderThread implements Runnable {

  private ClientDTO client;
  private boolean isConnected;
  private Socket socket = null;

  public MessageSenderThread(ClientDTO clientDTO) {
    this.client = clientDTO;
  }

  public void connectToServer() throws IOException {
    socket = new Socket(client.getServerAddress(), client.getServerPort());
    log.info("virtual device client is started ...");
    isConnected = true;
  }

  public void startMessageSender() {
    while (isConnected == false) {
      try {
        connectToServer();
        log.debug("virtual device client is started ...");
      } catch (Exception e) {
        log.error(e.getMessage(), e.getCause());
        try {
          TimeUnit.SECONDS.sleep(3);
          connectToServer();
        } catch (IOException | InterruptedException ex) {
          log.error(ex.getMessage(), ex.getCause());
        }
      }
    }
  }

  public void run() {
    PrintWriter pWriter = null;
    Scanner scanner = null;
    try {
      startMessageSender();

      if (socket == null) {
        startMessageSender();
      }
      pWriter = new PrintWriter(socket.getOutputStream());

      while (true) {
        final String message = client.getMessageQueue().take();
        // send message to  server side
        log.info("send message to server:{}", message);
        pWriter.write(MSG_BEGIN +message+ MSG_END);
        pWriter.flush();
        TimeUnit.SECONDS.sleep(client.getSendInterval());
      }

    } catch (InterruptedException | IOException e) {
      log.error(e.getMessage(), e);
    } finally {
      scanner.close();
      pWriter.close();
    }
  }
}

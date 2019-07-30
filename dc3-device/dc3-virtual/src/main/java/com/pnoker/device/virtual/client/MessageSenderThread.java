package com.pnoker.device.virtual.client;

import com.pnoker.device.virtual.model.ClientSocket;
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
 * <p>Author : Charles
 * <p>Email : xinguangduan@163.com
 * <p>Description: 消息发送线程
 */
@Slf4j
public class MessageSenderThread implements Runnable {

    private ClientSocket client;
    private volatile boolean isConnected;
    private Socket socket = null;

    public MessageSenderThread(ClientSocket clientDTO) {
        this.client = clientDTO;
    }

    public void connectToServer() throws IOException {
        socket = new Socket(client.getServerAddress(), client.getServerPort());
        log.info("virtual device client is started ...");
        isConnected = true;
    }

    public void connectToServerManagement() {
        // Keep trying to connect to the server until it is connected
        while (isConnected == false) {
            try {
                connectToServer();
                log.info("virtual device client is connected ...");
            } catch (Exception e) {
                log.error(e.getMessage(), e.getCause());
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void run() {
        PrintWriter pWriter = null;
        Scanner scanner = null;
        try {
            connectToServerManagement();
            if (socket == null) {
                connectToServerManagement();
            }
            pWriter = new PrintWriter(socket.getOutputStream());
            while (true) {
                final String message = buildMessage();
                pWriter.write(message);
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

    private String buildMessage() throws InterruptedException {
        // take message from queue
        final String message = client.getMessageQueue().take();
        // send message to server side
        log.info("send message to server:{}", message);
        return MSG_BEGIN + message + MSG_END;
    }
}

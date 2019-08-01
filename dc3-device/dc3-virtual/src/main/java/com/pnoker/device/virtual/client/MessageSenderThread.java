package com.pnoker.device.virtual.client;

import com.pnoker.device.virtual.constant.ProtocolConstant;
import com.pnoker.device.virtual.model.ClientSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.nio.CharBuffer;
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
    private volatile boolean canSendMsg;
    private Socket socket = null;

    public MessageSenderThread(ClientSocket client) {
        this.client = client;
    }

    public void connectToServer() throws IOException {
        socket = new Socket(client.getServerAddress(), client.getServerPort());
        log.info("virtual device client is started ...");
        isConnected = true;
        if (socket.isConnected()) {
            new ReceiveMessageKeyThread(socket).start();
        }
    }

    public void connectToServerManagement() {
        // Keep trying to connect to the server until it is connected
        while (isConnected == false) {
            try {
                connectToServer();
                log.info("virtual device client is connected ...");
            } catch (Exception e) {
                if (e instanceof IOException) {
                    try {
                        log.error("Please check if the server is available,try again after 3 seconds.", e.getCause());
                        TimeUnit.SECONDS.sleep(3);
                    } catch (InterruptedException ex) {
                    }
                } else {
                    log.error(e.getMessage(), e.getCause());
                }
            }
        }
    }

    private class ReceiveMessageKeyThread extends Thread {
        private Socket socket;

        public ReceiveMessageKeyThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                StringBuffer temp = new StringBuffer();
                Reader reader = new InputStreamReader(socket.getInputStream());
                CharBuffer charbuffer = CharBuffer.allocate(1024);
                while (reader.read(charbuffer) != -1) {
                    charbuffer.flip();
                    temp.append(charbuffer.toString());
                    log.info("received msg Key :{}", temp);
                    if (ProtocolConstant.MSG_KEY.equalsIgnoreCase(temp.toString())) {
                        canSendMsg = true;
                        break;
                    }
                    log.info("wait for server side message key...");
                    TimeUnit.SECONDS.sleep(3);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
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
                if (canSendMsg) {
                    final String message = buildMessage();
                    pWriter.write(message);
                    pWriter.flush();
                }
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

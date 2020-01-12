/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.device.virtual.server;

import com.pnoker.device.virtual.constant.ProtocolConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.CharBuffer;

import static com.pnoker.device.virtual.constant.ProtocolConstant.MSG_BEGIN;
import static com.pnoker.device.virtual.constant.ProtocolConstant.MSG_END;

/**
 *
 *
 * @author pnoker
 */
@Slf4j
public class MessageReceiverThread implements Runnable {
    private Socket socket;
    private boolean started;
    private Integer port;
    private String temp = "";
    private Integer defaultPort = 8765;

    public MessageReceiverThread(int port) {
        this.port = port;
    }

    public void initServer() {
        log.info("configuration port: {},default port:{} ", port, defaultPort);
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port == null ? defaultPort : port);
            started = true;
            log.info("Mock Server service started，use port： {}", serverSocket.getLocalPort());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            System.exit(0);
        }

        try {
            log.info("waiting for message...");
            socket = serverSocket.accept();
            socket.setKeepAlive(true);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        initServer();
        sendMessageKey();
        receiveWithByte();
    }

    public void sendMessageKey() {
        try {
            Writer writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            writer.write(ProtocolConstant.MSG_KEY);
            writer.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void receiveWithByte() {
        try {
            StringBuffer temp = new StringBuffer();
            Reader reader = new InputStreamReader(socket.getInputStream());
            CharBuffer charbuffer = CharBuffer.allocate(8192);
            while (reader.read(charbuffer) != -1) {
                charbuffer.flip();
                temp.append(charbuffer.toString());
                if (temp.indexOf(MSG_BEGIN) != -1 && temp.indexOf(MSG_END) != -1) {
                    log.info("received :{}", temp);
                    temp.setLength(0);
                }
                if (temp.length() > 1024 * 16) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (socket != null) {
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}

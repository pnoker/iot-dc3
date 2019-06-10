package com.pnoker.plcs7.websocket;

import com.alibaba.fastjson.JSON;
import com.github.s7connector.api.S7Connector;
import com.github.s7connector.api.S7Serializer;
import com.github.s7connector.api.factory.S7ConnectorFactory;
import com.github.s7connector.api.factory.S7SerializerFactory;
import com.pnoker.plcs7.block.PunchDb;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Websocket 控制类
 */
@Slf4j
@Component
@ServerEndpoint(value = "/ws/pubnch")
public class WsController {
    private static final String HOST = "192.168.0.20";
    private static final int DB_NUM = 7;
    private static final int BYTE_OFFSET = 0;

    private Session session;
    private volatile boolean isRun = false;
    private S7Connector connector = null;

    @OnOpen
    public void onOpen(Session session) {
        log.info("open websocket {}", session.getId());
        this.session = session;
        if (null == connector) {
            try {
                isRun = true;
                connector = S7ConnectorFactory.buildTCPConnector().withHost(HOST).build();
            } catch (Exception e) {
                connector = null;
                log.error("new s7connector fail {}", e.getMessage(), e);
            }
        }

        if (null != connector) {
            S7Serializer serializer = S7SerializerFactory.buildSerializer(connector);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (isRun) {
                            PunchDb punchDb = serializer.dispense(PunchDb.class, DB_NUM, BYTE_OFFSET);
                            sendMessage(JSON.toJSONString(punchDb));
                        } else {
                            cancel();
                            log.info("stop send message to session {}", session.getId());
                        }
                    } catch (Exception e) {
                        log.error("send message timer fail {}", e.getMessage(), e);
                    }
                }
            }, 5000, 200);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("receive message '{}' from session id '{}' ", message, session.getId());
    }

    @OnClose
    public void onClose() {
        log.info("close websocket {}", session.getId());
        isRun = false;
    }

    public void onError(Session session, Throwable error) {
        log.error(error.getMessage());
    }

    public void sendMessage(String message) {
        try {
            log.info("send message {} ", message);
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("sendMessage fail {}", e.getMessage(), e);
        }
    }
}
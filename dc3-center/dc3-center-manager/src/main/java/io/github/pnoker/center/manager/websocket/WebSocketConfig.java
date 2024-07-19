package io.github.pnoker.center.manager.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocketConfig TODO
 *
 * @author SQ
 * @since 2024.07.15
 */
@Configuration
public class WebSocketConfig {
    @Bean(name = "customWebSocketHandler")
    public CustomWebSocketHandler getMyWebsocketHandler() {
        return new CustomWebSocketHandler();
    }
    @Bean
    public HandlerMapping handlerMapping() {
        // 对相应的URL进行添加处理器
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/mqtt", getMyWebsocketHandler());

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}

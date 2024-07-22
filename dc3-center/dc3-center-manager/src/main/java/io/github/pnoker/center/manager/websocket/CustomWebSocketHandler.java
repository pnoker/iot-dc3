package io.github.pnoker.center.manager.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pnoker.center.manager.entity.vo.MQTTtopicVO;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.socket.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class CustomWebSocketHandler implements WebSocketHandler, CorsConfigurationSource {
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private WebSocketSession currentSession;
     private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        this.currentSession = session;
        return session.send(Flux.create(sink -> {
            new Thread(() -> {
                while (session.isOpen()) {
                    try {
                        String message = messageQueue.take();
                        sink.next(session.textMessage(message));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }
                sink.complete();
            }).start();
        })).and(session.receive().doFinally(signalType -> this.currentSession = null));
    }

    public void handleMqttMessage(String topic, MqttMessage message) {
        try {
            MQTTtopicVO mqttTopicVO = new MQTTtopicVO();
            mqttTopicVO.setTopic(topic);
            JsonNode messageJson = objectMapper.readTree(message.getPayload());
            mqttTopicVO.setMessage(messageJson);
            String payload = objectMapper.writeValueAsString(mqttTopicVO);
            messageQueue.put(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        return configuration;
    }
}


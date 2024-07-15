package io.github.pnoker.center.manager.websocket;

import org.apache.commons.collections4.MultiMap;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.socket.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Component
public class CustomWebSocketHandler implements WebSocketHandler, CorsConfigurationSource {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // 在生产环境中，需对url中的参数进行检验，如token，不符合要求的连接的直接关闭
        HandshakeInfo handshakeInfo = session.getHandshakeInfo();
//        if (handshakeInfo.getUri().getQuery() == null) {
//            return session.close(CloseStatus.REQUIRED_EXTENSION);
//        } else {
            // 对参数进行解析，在些使用的是jetty-util包
//            MultiMap<String> values = new MultiMap<String>();
//            UrlEncoded.decodeTo(handshakeInfo.getUri().getQuery(), values, "UTF-8");
//            String token = values.getString("token");
//            boolean isValidate = tokenService.validate(token);
//            if (!isValidate) {
//                return session.close();
//            }
//        }
        Flux<WebSocketMessage> output = session.receive()
                .concatMap(mapper -> {
                    String msg = mapper.getPayloadAsText();
                    System.out.println("mapper: " + msg);
                    return Flux.just(msg);
                }).map(value -> {
                    System.out.println("value: " + value);
                    return session.textMessage("Echo " + value);
                });
        return session.send(output);
    }
    @Override
    public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        return configuration;
    }
}


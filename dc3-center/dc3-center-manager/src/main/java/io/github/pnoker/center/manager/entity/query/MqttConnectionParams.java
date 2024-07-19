package io.github.pnoker.center.manager.entity.query;

import lombok.Data;

/**
 * @version 1.0
 * @Author 嘉平十二
 * @Date 2024/7/15 16:10
 * @注释
 */
@Data
public class MqttConnectionParams {
    private String broker;
    private String clientId;
    private String username;
    private String password;
    private int connectionTimeout;
    private int keepAliveInterval;
    private boolean automaticReconnect;
    private int maxInflight;
    private String mqttVersion;
}

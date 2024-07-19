package io.github.pnoker.center.manager.service;


import io.github.pnoker.center.manager.entity.model.ConnectionProfile;
import io.github.pnoker.common.entity.R;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * @version 1.0
 * @Author 嘉平十二
 * @Date 2024/7/15 16:11
 * @注释
 */

public interface MqttService {
    String connect(ConnectionProfile request) throws MqttException;
    String subscribe(String topic) throws MqttException;
    String disconnect() throws MqttException;
    boolean isConnected();
    String unsubscribe(String topic) throws MqttException;
}
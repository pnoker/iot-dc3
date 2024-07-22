package io.github.pnoker.center.manager.service.impl;


import io.github.pnoker.center.manager.entity.model.ConnectionProfile;
import io.github.pnoker.center.manager.service.MqttService;
import io.github.pnoker.center.manager.websocket.CustomWebSocketHandler;
import io.github.pnoker.common.entity.R;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;/**
 * @version 1.0
 * @Author 嘉平十二
 * @Date 2024/7/15 16:14
 * @注释
 */
@Service
public class MqttServiceImpl implements MqttService {
    private MqttClient mqttClient;
    private final CustomWebSocketHandler webSocketHandler;

    @Autowired
    public MqttServiceImpl(CustomWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public String connect(ConnectionProfile request) throws MqttException {
        String broker = "tcp://"+request.getBrokerAddress()+":"+request.getBrokerPort();
        mqttClient = new MqttClient(broker, request.getClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(request.getUsername());
        options.setPassword(request.getPassword().toCharArray());
        options.setConnectionTimeout(request.getConnectionTimeout());
        options.setKeepAliveInterval(request.getKeepAliveInterval());
        options.setMaxInflight(request.getMaxInflight());

        if ("3.1".equals(request.getMqttVersion())) {
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        } else if ("3.1.1".equals(request.getMqttVersion())) {
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        } else {
            return "Unsupported MQTT version: " + request.getMqttVersion();
        }

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                webSocketHandler.handleMqttMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        mqttClient.connect(options);
        return "Connected to MQTT broker at " + broker;
    }

    @Override
    public String subscribe(String topic) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.subscribe(topic);
            return topic;
        } else {
            return "MQTT client is not connected.";
        }
    }

    @Override
    public String disconnect() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
            return "Disconnected from MQTT broker.";
        } else {
            return "MQTT client is not connected.";
        }
    }

    @Override
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    @Override
    public String unsubscribe(String topic) throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.unsubscribe(topic);
            return "Unsubscribed from topic: " + topic;
        } else {
            return "MQTT client is not connected.";
        }
    }


}
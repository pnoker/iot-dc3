package com.dc3.driver.service.impl;

import com.dc3.driver.mqtt.handler.MqttSendHandler;
import com.dc3.driver.mqtt.service.MqttSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class MqttSendServiceImpl implements MqttSendService {

    @Resource
    private MqttSendHandler mqttSendHandler;

    @Override
    public void sendToMqtt(String data) {
        mqttSendHandler.sendToMqtt(data);
    }

    @Override
    public void sendToMqtt(Integer qos, String data) {
        mqttSendHandler.sendToMqtt(qos, data);
    }

    @Override
    public void sendToMqtt(String topic, String data) {
        mqttSendHandler.sendToMqtt(topic, data);
    }

    @Override
    public void sendToMqtt(String topic, Integer qos, String data) {
        mqttSendHandler.sendToMqtt(topic, qos, data);
    }
}

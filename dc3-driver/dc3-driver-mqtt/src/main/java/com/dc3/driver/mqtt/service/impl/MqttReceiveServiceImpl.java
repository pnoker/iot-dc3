package com.dc3.driver.mqtt.service.impl;

import com.dc3.common.sdk.bean.mqtt.MqttMessage;
import com.dc3.driver.mqtt.service.MqttReceiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class MqttReceiveServiceImpl implements MqttReceiveService {

    @Override
    public void receiveValue(MqttMessage mqttMessage) {
    }

    @Override
    public void receiveValues(List<MqttMessage> mqttMessageList) {

    }
}

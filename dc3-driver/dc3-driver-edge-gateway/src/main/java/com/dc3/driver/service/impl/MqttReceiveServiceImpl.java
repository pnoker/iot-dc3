package com.dc3.driver.service.impl;

import com.dc3.common.sdk.bean.mqtt.MessageHeader;
import com.dc3.driver.mqtt.service.MqttReceiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class MqttReceiveServiceImpl implements MqttReceiveService {

    @Override
    public void receiveValue(String client, MessageHeader messageHeader, String data) {
    }
}

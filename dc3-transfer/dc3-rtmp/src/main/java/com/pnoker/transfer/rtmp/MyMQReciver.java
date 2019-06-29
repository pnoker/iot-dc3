package com.pnoker.transfer.rtmp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

/**
 * @Author: lyang
 * @Date: 2018/12/29 16:35
 */
@Component
@Slf4j
@EnableBinding(MyProcessor.class)
public class MyMQReciver {

    @StreamListener(MyProcessor.CALLBACKINPUT)
    public void callback(String myGirl){
        log.info("collect has recived : {} ", myGirl);
    }
}
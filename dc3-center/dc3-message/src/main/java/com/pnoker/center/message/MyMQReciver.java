package com.pnoker.center.message;

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

    @StreamListener(MyProcessor.INPUT)
    @SendTo(MyProcessor.CALLBACKINPUT)
    public String process(MyGirl myGirl){
        log.info("message comming : {} ", myGirl.toString());
        return myGirl.toString();
    }

    @StreamListener(MyProcessor.CALLBACKINPUT)
    public void callback(String myGirl){
        log.info("message has recived : {} ", myGirl);
    }
}

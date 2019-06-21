package com.pnoker.center.message;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * @Author: lyang
 * @Date: 2019/1/1 18:18
 */
public interface MyInput {
    String INPUT = "myinput";

    @Input(MyInput.INPUT)
    SubscribableChannel input();
}

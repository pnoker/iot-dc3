package com.pnoker.center.collect;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * @Author: lyang
 * @Date: 2019/1/1 18:46
 */
public interface MyCallback {
    String CALLBACKINPUT = "mycallback";

    @Input(MyCallback.CALLBACKINPUT)
    SubscribableChannel callback();
}

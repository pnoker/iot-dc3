package com.pnoker.center.message.receiver;

import com.pnoker.center.message.input.DriverInput;
import com.pnoker.common.constant.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

/**
 * @author pnoker
 */
@Slf4j
@EnableBinding(DriverInput.class)
public class DriverReceiver {

    @StreamListener(Common.Msg.DRIVER_CHANNEL)
    public void receive(String msg) {
        log.info("receiver {}", msg);
    }
}

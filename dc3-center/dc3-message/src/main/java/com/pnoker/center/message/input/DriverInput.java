package com.pnoker.center.message.input;

import com.pnoker.common.constant.Common;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author pnoker
 */
public interface DriverInput {

    @Input(Common.Msg.DRIVER_CHANNEL)
    SubscribableChannel input();
}

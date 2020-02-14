package com.pnoker.common.sdk.message;

import com.pnoker.common.constant.Common;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.MessageChannel;

/**
 * @author pnoker
 */
public interface DriverInput {

    @Input(Common.Msg.DRIVER_CHANNEL)
    MessageChannel input();
}

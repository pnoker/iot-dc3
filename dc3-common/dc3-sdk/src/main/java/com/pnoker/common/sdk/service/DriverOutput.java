package com.pnoker.common.sdk.service;

import com.pnoker.common.constant.Common;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

/**
 * @author pnoker
 */
@Component
public interface DriverOutput {

    @Output(Common.Msg.DRIVER_CHANNEL)
    MessageChannel output();
}

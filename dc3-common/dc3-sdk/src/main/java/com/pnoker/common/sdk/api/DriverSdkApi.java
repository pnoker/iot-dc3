package com.pnoker.common.sdk.api;

import com.pnoker.common.constant.Common;
import com.pnoker.common.sdk.message.DriverInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@RestController
@EnableBinding(DriverInput.class)
@RequestMapping(Common.Service.DC3_DRIVER_URL_PREFIX)
public class DriverSdkApi {
    @Resource
    private DriverInput driverInput;

    @GetMapping("/msg")
    public void msg() {
        driverInput.input().send(MessageBuilder.withPayload("hello 123").build());
    }
}

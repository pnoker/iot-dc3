package com.pnoker.driver.service.impl;

import com.pnoker.common.sdk.bean.AttributeInfo;
import com.pnoker.common.sdk.service.CustomizersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class CustomizersServiceImpl implements CustomizersService {

    @Override
    public void initial() {
        log.info("hello");
    }

    @Override
    public void schedule() {

    }

    @Override
    public void read(Map<String, AttributeInfo> driverInfo, Map<String, AttributeInfo> pointInfo) {

    }

    @Override
    public void write() {

    }

    @Override
    public void receive() {

    }

    @Override
    public void status() {

    }
}

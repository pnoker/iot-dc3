package com.pnoker.driver.service;

import com.pnoker.common.model.Point;
import com.pnoker.common.sdk.init.DeviceDriver;
import com.pnoker.common.sdk.service.DriverCustomizersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverService implements DriverCustomizersService {

    @Override
    public void initial(DeviceDriver deviceDriver) {
        log.info("hello");
    }

    @Override
    public void schedule() {

    }

    @Override
    public void read(Map<String, String> connectInfo, Map<String, String> pointInfo, Point point) {

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

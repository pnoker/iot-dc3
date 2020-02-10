package com.pnoker.common.sdk.service.impl;

import com.pnoker.api.center.device.feign.DriverClient;
import com.pnoker.common.bean.R;
import com.pnoker.common.model.Driver;
import com.pnoker.common.sdk.service.DriverService;
import com.pnoker.common.utils.Dc3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverClient driverClient;

    @Override
    public boolean register(String name, String serviceName, String description) {
        if (!Dc3Util.isName(name) || !Dc3Util.isName(serviceName)) {
            log.error("driver name or driver service name is invalid");
            return false;
        }
        Driver driver = new Driver();
        driver.setName(name).setServiceName(serviceName).setDescription(description);
        R<Driver> select = driverClient.selectByServiceName(driver.getServiceName());
        if (select.isOk()) {
            driver.setId(select.getData().getId());
            return driverClient.update(driver).isOk();
        } else {
            R<Driver> add = driverClient.add(driver);
            return add.isOk();
        }
    }

}

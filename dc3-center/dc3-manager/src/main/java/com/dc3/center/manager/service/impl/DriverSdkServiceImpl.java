/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.service.impl;

import com.dc3.center.manager.service.*;
import com.dc3.common.bean.driver.DriverRegister;
import com.dc3.common.exception.NotFoundException;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Driver;
import com.dc3.common.model.DriverAttribute;
import com.dc3.common.model.PointAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>DriverService Impl
 *
 * @author pnoker
 */
@Slf4j
@Service
public class DriverSdkServiceImpl implements DriverSdkService {

    @Resource
    private DriverService driverService;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverInfoService driverInfoService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointInfoService pointInfoService;

    @Override
    public void driverRegister(DriverRegister driverRegister) {
        // register driver
        Driver driver = driverRegister.getDriver();
        log.info("Register driver {}", driver);
        try {
            Driver byServiceName = driverService.selectByServiceName(driver.getServiceName());
            log.debug("Driver already registered, updating {} ", driver);
            driver.setId(byServiceName.getId());
            driver = driverService.update(driver);
        } catch (NotFoundException notFoundException1) {
            log.debug("Driver does not registered, adding {} ", driver);
            try {
                Driver byHostPort = driverService.selectByHostPort(driver.getHost(), driver.getPort());
                throw new ServiceException("The port(" + driver.getPort() + ") is already occupied by driver(" + byHostPort.getName() + "/" + byHostPort.getServiceName() + ")");
            } catch (NotFoundException notFoundException2) {
                driver = driverService.add(driver);
            }
        }

        //register driver attribute
        Map<String, DriverAttribute> newDriverAttributeMap = new HashMap<>(8);
        if (null != driverRegister.getDriverAttributes() && driverRegister.getDriverAttributes().size() > 0) {
            driverRegister.getDriverAttributes().forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getName(), driverAttribute));
        }

        Map<String, DriverAttribute> oldDriverAttributeMap = new HashMap<>(8);
        try {
            List<DriverAttribute> byDriverId = driverAttributeService.selectByDriverId(driver.getId());
            byDriverId.forEach(driverAttribute -> oldDriverAttributeMap.put(driverAttribute.getName(), driverAttribute));
        } catch (NotFoundException ignored) {
        }

        for (String name : newDriverAttributeMap.keySet()) {
            DriverAttribute info = newDriverAttributeMap.get(name).setDriverId(driver.getId());
            if (oldDriverAttributeMap.containsKey(name)) {
                info.setId(oldDriverAttributeMap.get(name).getId());
                log.debug("Driver attribute registered, updating: {}", info);
                driverAttributeService.update(info);
            } else {
                log.debug("Driver attribute does not registered, adding: {}", info);
                driverAttributeService.add(info);
            }
        }

        for (String name : oldDriverAttributeMap.keySet()) {
            if (!newDriverAttributeMap.containsKey(name)) {
                try {
                    driverInfoService.selectByAttributeId(oldDriverAttributeMap.get(name).getId());
                    throw new ServiceException("The driver attribute(" + name + ") used by driver info and cannot be deleted");
                } catch (NotFoundException notFoundException) {
                    log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
                    driverAttributeService.delete(oldDriverAttributeMap.get(name).getId());
                }
            }
        }

        // register point attribute
        Map<String, PointAttribute> newPointAttributeMap = new HashMap<>(8);
        if (null != driverRegister.getPointAttributes() && driverRegister.getPointAttributes().size() > 0) {
            driverRegister.getPointAttributes().forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getName(), pointAttribute));
        }

        Map<String, PointAttribute> oldPointAttributeMap = new HashMap<>(8);
        try {
            List<PointAttribute> byDriverId = pointAttributeService.selectByDriverId(driver.getId());
            byDriverId.forEach(pointAttribute -> oldPointAttributeMap.put(pointAttribute.getName(), pointAttribute));
        } catch (NotFoundException ignored) {
        }

        for (String name : newPointAttributeMap.keySet()) {
            PointAttribute attribute = newPointAttributeMap.get(name).setDriverId(driver.getId());
            if (oldPointAttributeMap.containsKey(name)) {
                attribute.setId(oldPointAttributeMap.get(name).getId());
                log.debug("Point attribute registered, updating: {}", attribute);
                pointAttributeService.update(attribute);
            } else {
                log.debug("Point attribute registered, adding: {}", attribute);
                pointAttributeService.add(attribute);
            }
        }

        for (String name : oldPointAttributeMap.keySet()) {
            if (!newPointAttributeMap.containsKey(name)) {
                try {
                    pointInfoService.selectByAttributeId(oldPointAttributeMap.get(name).getId());
                    throw new ServiceException("The point attribute(" + name + ") used by point info and cannot be deleted");
                } catch (NotFoundException notFoundException1) {
                    log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                    pointAttributeService.delete(oldPointAttributeMap.get(name).getId());
                }
            }
        }
    }

}

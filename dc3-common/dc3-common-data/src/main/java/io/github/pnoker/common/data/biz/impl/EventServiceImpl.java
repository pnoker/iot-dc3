/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.data.biz.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.biz.EventService;
import io.github.pnoker.common.data.entity.DeviceEvent;
import io.github.pnoker.common.data.entity.DriverEvent;
import io.github.pnoker.common.data.entity.query.DeviceEventQuery;
import io.github.pnoker.common.data.entity.query.DriverEventQuery;
import io.github.pnoker.common.entity.common.Pages;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class EventServiceImpl implements EventService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void addDriverEvent(DriverEvent driverEvent) {
        if (Objects.nonNull(driverEvent)) {
            mongoTemplate.insert(driverEvent);
        }
    }

    @Override
    public void addDriverEvents(List<DriverEvent> driverEvents) {
        if (Objects.nonNull(driverEvents) && !driverEvents.isEmpty()) {
            mongoTemplate.insert(driverEvents, DriverEvent.class);
        }
    }

    @Override
    public void addDeviceEvent(DeviceEvent deviceEvent) {
        if (Objects.nonNull(deviceEvent)) {
            mongoTemplate.insert(deviceEvent);
        }
    }

    @Override
    public void addDeviceEvents(List<DeviceEvent> deviceEvents) {
        if (Objects.nonNull(deviceEvents) && !deviceEvents.isEmpty()) {
            mongoTemplate.insert(deviceEvents, DeviceEvent.class);
        }
    }

    @Override
    public Page<DriverEvent> driverEvent(DriverEventQuery driverEventQuery) {
        return null;
    }

    @Override
    public Page<DeviceEvent> deviceEvent(DeviceEventQuery deviceEventQuery) {
        Criteria criteria = new Criteria();
        if (Objects.isNull(deviceEventQuery)) {
            deviceEventQuery = new DeviceEventQuery();
        }
        if (Objects.nonNull(deviceEventQuery.getDeviceId())) {
            criteria.and("deviceId").is(deviceEventQuery.getDeviceId());
        }
        if (Objects.nonNull(deviceEventQuery.getPointId())) {
            criteria.and("pointId").is(deviceEventQuery.getPointId());
        }

        Pages pages = Objects.isNull(deviceEventQuery.getPage()) ? new Pages() : deviceEventQuery.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and("createTime").gte(pages.getStartTime()).lte(pages.getEndTime());
        }

        Query query = new Query(criteria);
        long count = mongoTemplate.count(query, DeviceEvent.class);

        query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        long size = pages.getSize();
        long page = pages.getCurrent();
        query.limit((int) size).skip(size * (page - 1));

        List<DeviceEvent> deviceEvents = mongoTemplate.find(query, DeviceEvent.class);

        return (new Page<DeviceEvent>()).setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count).setRecords(deviceEvents);
    }

}

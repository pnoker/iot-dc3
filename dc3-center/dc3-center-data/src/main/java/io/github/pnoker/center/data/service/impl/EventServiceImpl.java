/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.data.entity.vo.query.DeviceEventPageQuery;
import io.github.pnoker.center.data.entity.vo.query.DriverEventPageQuery;
import io.github.pnoker.center.data.service.EventService;
import io.github.pnoker.common.entity.DeviceEvent;
import io.github.pnoker.common.entity.DriverEvent;
import io.github.pnoker.common.entity.common.Pages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class EventServiceImpl implements EventService {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDriverEvent(DriverEvent driverEvent) {
        if (ObjectUtil.isNotNull(driverEvent)) {
            mongoTemplate.insert(driverEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDriverEvents(List<DriverEvent> driverEvents) {
        if (ObjectUtil.isNotNull(driverEvents) && !driverEvents.isEmpty()) {
            mongoTemplate.insert(driverEvents, DriverEvent.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDeviceEvent(DeviceEvent deviceEvent) {
        if (ObjectUtil.isNotNull(deviceEvent)) {
            mongoTemplate.insert(deviceEvent);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDeviceEvents(List<DeviceEvent> deviceEvents) {
        if (ObjectUtil.isNotNull(deviceEvents) && !deviceEvents.isEmpty()) {
            mongoTemplate.insert(deviceEvents, DeviceEvent.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverEvent> driverEvent(DriverEventPageQuery driverEventPageQuery) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DeviceEvent> deviceEvent(DeviceEventPageQuery deviceEventPageQuery) {
        Criteria criteria = new Criteria();
        if (ObjectUtil.isNull(deviceEventPageQuery)) {
            deviceEventPageQuery = new DeviceEventPageQuery();
        }
        if (CharSequenceUtil.isNotEmpty(deviceEventPageQuery.getDeviceId())) {
            criteria.and("deviceId").is(deviceEventPageQuery.getDeviceId());
        }
        if (CharSequenceUtil.isNotEmpty(deviceEventPageQuery.getPointId())) {
            criteria.and("pointId").is(deviceEventPageQuery.getPointId());
        }

        Pages pages = ObjectUtil.isNull(deviceEventPageQuery.getPage()) ? new Pages() : deviceEventPageQuery.getPage();
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

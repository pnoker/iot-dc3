/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DeviceClient;
import com.dc3.center.data.service.PointValueService;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;
import com.dc3.common.constant.Common;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.utils.RedisUtil;
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
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private DeviceClient deviceClient;

    @Override
    public String status(Long deviceId) {
        String key = Common.Cache.DEVICE_STATUS_KEY_PREFIX + deviceId;
        String status = redisUtil.getKey(key);
        return null != status ? status : Common.Device.OFFLINE;
    }

    @Override
    public List<PointValue> realtime(Long deviceId) {
        String key = Common.Cache.REAL_TIME_VALUES_KEY_PREFIX + deviceId;
        List<PointValue> pointValues = redisUtil.getKey(key);
        if (null == pointValues) {
            throw new ServiceException("No realtime value, Please use '/latest' to get the final data");
        }
        return pointValues;
    }

    @Override
    public PointValue realtime(Long deviceId, Long pointId) {
        String key = Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + deviceId + "_" + pointId;
        PointValue pointValue = redisUtil.getKey(key);
        if (null == pointValue) {
            throw new ServiceException("No realtime value, Please use '/latest' to get the final data");
        }
        return pointValue;
    }

    @Override
    public PointValue latest(Long deviceId, Long pointId) {
        R<Device> r = deviceClient.selectById(deviceId);
        if (!r.isOk()) {
            return null;
        }

        Criteria criteria = new Criteria();
        criteria.and("deviceId").is(deviceId);
        if (r.getData().getMulti()) {
            criteria.and("multi").is(true);
        }
        if (null != pointId) {
            criteria.and("pointId").is(pointId);
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        return mongoTemplate.findOne(query, PointValue.class);
    }

    @Override
    public void addPointValue(PointValue pointValue) {
        if (null != pointValue) {
            mongoTemplate.insert(pointValue.setCreateTime(System.currentTimeMillis()));
        }
    }

    @Override
    public void addPointValues(List<PointValue> pointValues) {
        pointValues.forEach(pointValue -> pointValue.setCreateTime(System.currentTimeMillis()));
        mongoTemplate.insert(pointValues, PointValue.class);
    }

    @Override
    public Page<PointValue> list(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        if (null == pointValueDto) {
            pointValueDto = new PointValueDto();
        }
        if (null != pointValueDto.getDeviceId()) {
            criteria.and("deviceId").is(pointValueDto.getDeviceId());
            R<Device> r = deviceClient.selectById(pointValueDto.getDeviceId());
            if (r.isOk()) {
                if (r.getData().getMulti()) {
                    criteria.and("multi").is(true);
                }
            }
        }
        if (null != pointValueDto.getPointId()) {
            criteria.and("pointId").is(pointValueDto.getPointId());
        }

        if (null == pointValueDto.getPage()) {
            pointValueDto.setPage(new Pages());
        }
        Pages pages = pointValueDto.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and("originTime").gte(pages.getStartTime()).lte(pages.getEndTime());
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        int size = (int) pages.getSize();
        long page = pages.getCurrent();
        query.limit(size).skip(size * (page - 1));

        long count = mongoTemplate.count(query, PointValue.class);
        List<PointValue> pointValues = mongoTemplate.find(query, PointValue.class);

        Long id = 0L;
        for (PointValue pointValue1 : pointValues) {
            pointValue1.setId(id);
            id++;
            for (PointValue pointValue2 : pointValue1.getChildren()) {
                pointValue2.setId(id);
                id++;
            }
        }
        return (new Page<PointValue>()).setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count).setRecords(pointValues);
    }

}

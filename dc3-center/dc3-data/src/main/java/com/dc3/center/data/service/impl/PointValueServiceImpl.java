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

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DeviceClient;
import com.dc3.api.center.manager.feign.DriverClient;
import com.dc3.api.center.manager.feign.ProfileClient;
import com.dc3.center.data.service.PointValueService;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.bean.driver.DeviceEvent;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.DeviceDto;
import com.dc3.common.dto.ProfileDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Driver;
import com.dc3.common.model.Profile;
import com.dc3.common.utils.ArithmeticUtil;
import com.dc3.common.utils.Dc3Util;
import com.dc3.common.utils.RedisUtil;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

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
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private DeviceClient deviceClient;
    @Resource
    private DriverClient driverClient;
    @Resource
    private ProfileClient profileClient;

    @Override
    public Boolean correct(String serviceName) {
        // TODO
        // 数据纠正接口，可自定义拓展
        // 这里给个参考的例子，仅供参考

        // select driver by driver service name
        R<Driver> r = driverClient.selectByServiceName(serviceName);
        if (!r.isOk()) {
            return false;
        }

        // select profile list by driver id
        ProfileDto profileDto = new ProfileDto();
        profileDto.setPage(new Pages().setSize(-1L)).setDriverId(r.getData().getId());
        R<Page<Profile>> rp = profileClient.list(profileDto);
        if (!rp.isOk()) {
            return false;
        }

        for (Profile profile : rp.getData().getRecords()) {
            // select device list by profile id
            DeviceDto deviceDto = new DeviceDto();
            deviceDto.setPage(new Pages().setSize(-1L)).setProfileId(profile.getId());
            R<Page<Device>> rpd = deviceClient.list(deviceDto);
            if (!rpd.isOk()) {
                return false;
            }

            switch (serviceName) {
                case "dc3-driver-listening-virtual":
                    // todo
                    break;
                case "dc3-driver-modbus-tcp":
                    // todo
                    break;
                case "dc3-driver-mqtt":
                    // todo
                    break;
                case "dc3-driver-opc-da":
                    // todo
                    break;
                case "dc3-driver-opc-ua":
                    // todo
                    break;
                case "dc3-driver-plcs7":
                    // todo
                    break;
                case "dc3-driver-virtual":
                    // todo
                    break;
                case "dc3-driver-water-188b":
                    for (Device device : rpd.getData().getRecords()) {
                        Criteria pointValueCriteria = new Criteria();
                        pointValueCriteria.and("deviceId").is(device.getId());
                        Query pointValueQuery = new Query(pointValueCriteria);
                        List<PointValue> pointValues = mongoTemplate.find(pointValueQuery, PointValue.class);
                        if (pointValues.size() > 0) {
                            for (PointValue pv : pointValues) {
                                Map<Long, PointValue> pointValueMap = new HashMap<>(64);
                                for (PointValue pointValue : pv.getChildren()) {
                                    pointValueMap.put(pointValue.getPointId(), pointValue);
                                }

                                for (PointValue pointValue : pv.getChildren()) {
                                    BigDecimal subtract = null;
                                    Long id = pointValue.getPointId();
                                    if (id == 50L) {
                                        subtract = ArithmeticUtil.subtract(pointValueMap.get(id).getValue(), pointValueMap.get(73L).getValue());
                                    }
                                    if (id > 50L && id < 74L) {
                                        subtract = ArithmeticUtil.subtract(pointValueMap.get(id).getValue(), pointValueMap.get(id - 1).getValue());
                                    }

                                    pointValue.setCustomValue(null);
                                    if (null != subtract && subtract.doubleValue() >= 0) {
                                        pointValue.setCustomValue(subtract);
                                    }

                                    if (null == pointValue.getCustomValue() && (id > 49L && id < 74L)) {
                                        pointValue.setCustomValue("-");
                                    }
                                }

                                Criteria criteria = new Criteria();
                                criteria.and("_id").is(pv.getObjectId());
                                Query query = new Query(criteria);

                                Update update = new Update();
                                update.set("children", pv.getChildren());

                                UpdateResult updateResult = mongoTemplate.upsert(query, update, PointValue.class);
                                log.info("correct originTime: {}, pointValue: {}, updateResult: {}", Dc3Util.formatData(new Date(pv.getOriginTime())), JSON.toJSONString(pv), updateResult);
                            }
                        }
                    }
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    @Override
    public String status(Long deviceId) {
        String key = Common.Cache.DEVICE_STATUS_KEY_PREFIX + deviceId;
        String status = redisUtil.getKey(key);
        return null != status ? status : Common.Device.Status.OFFLINE;
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
            if (null != pointId) {
                criteria.and("children").elemMatch(
                        (new Criteria()).and("pointId").is(pointId)
                );
            }
        } else if (null != pointId) {
            criteria.and("pointId").is(pointId);
        }

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        return mongoTemplate.findOne(query, PointValue.class);
    }

    @Override
    public void addDeviceEvent(DeviceEvent deviceEvent) {
        if (null != deviceEvent) {
            mongoTemplate.insert(deviceEvent);
        }
    }

    @Override
    public void addPointValue(PointValue pointValue) {
        if (null != pointValue) {
            savePointValueToRedis(pointValue.setCreateTime(System.currentTimeMillis()));
            mongoTemplate.insert(pointValue);
        }
    }

    @Override
    public void addPointValues(List<PointValue> pointValues) {
        if (null != pointValues) {
            if (pointValues.size() > 0) {
                pointValues.forEach(pointValue -> pointValue.setCreateTime(System.currentTimeMillis()));
                savePointValuesToRedis(pointValues);
                mongoTemplate.insert(pointValues, PointValue.class);
            }
        }
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
                    if (null != pointValueDto.getPointId()) {
                        criteria.and("children").elemMatch(
                                (new Criteria()).and("pointId").is(pointValueDto.getPointId())
                        );
                    }
                } else if (null != pointValueDto.getPointId()) {
                    criteria.and("pointId").is(pointValueDto.getPointId());
                }
            }
        } else if (null != pointValueDto.getPointId()) {
            criteria.orOperator(
                    (new Criteria()).and("pointId").is(pointValueDto.getPointId()),
                    (new Criteria()).and("children").elemMatch((new Criteria()).and("pointId").is(pointValueDto.getPointId()))
            );
        }

        if (null == pointValueDto.getPage()) {
            pointValueDto.setPage(new Pages());
        }
        Pages pages = pointValueDto.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and("originTime").gte(pages.getStartTime()).lte(pages.getEndTime());
        }

        Query query = new Query(criteria);
        long count = mongoTemplate.count(query, PointValue.class);

        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        int size = (int) pages.getSize();
        long page = pages.getCurrent();
        query.limit(size).skip(size * (page - 1));

        List<PointValue> pointValues = mongoTemplate.find(query, PointValue.class);

        long id = 0L;
        for (PointValue pointValue1 : pointValues) {
            pointValue1.setId(id);
            id++;
            if (null != pointValue1.getChildren()) {
                for (PointValue pointValue2 : pointValue1.getChildren()) {
                    pointValue2.setId(id);
                    id++;
                }
            }
        }
        return (new Page<PointValue>()).setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count).setRecords(pointValues);
    }

    /**
     * Save point value to redis
     *
     * @param pointValue Point Value
     */
    private void savePointValueToRedis(final PointValue pointValue) {
        threadPoolExecutor.execute(() -> {
            String pointIdKey = pointValue.getPointId() != null ? String.valueOf(pointValue.getPointId()) : Common.Cache.ASTERISK;
            // Save point value to Redis
            redisUtil.setKey(
                    Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + Common.Cache.DOT + pointIdKey,
                    pointValue,
                    pointValue.getTimeOut(),
                    pointValue.getTimeUnit()
            );
        });
    }

    /**
     * Save point value to redis
     *
     * @param pointValues Point Value Array
     */
    private void savePointValuesToRedis(final List<PointValue> pointValues) {
        threadPoolExecutor.execute(() -> pointValues.forEach(pointValue -> {
            String pointIdKey = pointValue.getPointId() != null ? String.valueOf(pointValue.getPointId()) : Common.Cache.ASTERISK;
            // Save point value to Redis
            redisUtil.setKey(
                    Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + Common.Cache.DOT + pointIdKey,
                    pointValue,
                    pointValue.getTimeOut(),
                    pointValue.getTimeUnit()
            );
        }));
    }

}

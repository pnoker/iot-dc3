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

package com.dc3.center.data.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.DeviceClient;
import com.dc3.api.center.manager.feign.PointClient;
import com.dc3.center.data.service.DataCustomService;
import com.dc3.center.data.service.PointValueService;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.PointValueDto;
import com.dc3.common.exception.ServiceException;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.model.PointValue;
import com.dc3.common.utils.RedisUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private PointClient pointClient;
    @Resource
    private DeviceClient deviceClient;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private DataCustomService dataCustomService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    @Override
    public List<PointValue> realtime(Long deviceId) {
        R<List<Point>> listR = pointClient.selectByDeviceId(deviceId);
        if (listR.isOk()) {
            List<String> keys = new ArrayList<>();
            String prefix = Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + deviceId + "_";

            List<Point> points = listR.getData();
            for (Point point : points) {
                keys.add(prefix + point.getId());
            }
            if (keys.size() > 0) {
                List<PointValue> pointValues = redisUtil.getKeys(keys, PointValue.class);
                pointValues = pointValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
                if (pointValues.size() > 0) {
                    return pointValues;
                }
            }
        } else {
            throw new ServiceException(listR.getMessage());
        }
        throw new ServiceException("No realtime value, Please use '/latest' to get the final data");
    }

    @Override
    public PointValue realtime(Long deviceId, Long pointId) {
        String key = Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + deviceId + "_" + pointId;
        PointValue pointValue = redisUtil.getKey(key, PointValue.class);
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
    public void savePointValue(PointValue pointValue) {
        if (null != pointValue) {
            pointValue.setCreateTime(System.currentTimeMillis());
            threadPoolExecutor.execute(() -> dataCustomService.postHandle(pointValue));
            threadPoolExecutor.execute(() -> savePointValueToMongo(pointValue));
            threadPoolExecutor.execute(() -> savePointValueToRedis(pointValue));
        }
    }

    @Override
    public void savePointValues(List<PointValue> pointValues) {
        if (null != pointValues) {
            if (pointValues.size() > 0) {
                Future<List<PointValue>> future = threadPoolExecutor.submit(() -> {
                    pointValues.forEach(pointValue -> pointValue.setCreateTime(System.currentTimeMillis()));
                    return pointValues;
                });

                threadPoolExecutor.execute(() -> {
                    try {
                        dataCustomService.postHandle(future.get());
                    } catch (Exception e) {
                        log.error("Save point values to post handle error {}", e.getMessage());
                    }
                });
                threadPoolExecutor.execute(() -> {
                    try {
                        savePointValuesToMongo(future.get());
                    } catch (Exception e) {
                        log.error("Save point values to mongo error {}", e.getMessage());
                    }
                });
                threadPoolExecutor.execute(() -> {
                    try {
                        savePointValuesToRedis(future.get());
                    } catch (Exception e) {
                        log.error("Save point values to redis error {}", e.getMessage());
                    }
                });
            }
        }
    }

    @Override
    @SneakyThrows
    public Page<PointValue> list(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        pointValueDto = Optional.ofNullable(pointValueDto).orElse(new PointValueDto());

        if (null != pointValueDto.getDeviceId()) {
            R<Device> r = deviceClient.selectById(pointValueDto.getDeviceId());
            if (r.isOk()) {
                Device device = r.getData();
                criteria.and("deviceId").is(pointValueDto.getDeviceId());
                if (!device.getMulti()) {
                    if (null != pointValueDto.getPointId()) {
                        criteria.and("pointId").is(pointValueDto.getPointId());
                    }
                } else if (null != pointValueDto.getPointId()) {
                    criteria.and("multi").is(true);
                    if (null != pointValueDto.getPointId()) {
                        criteria.and("children").elemMatch((new Criteria()).and("pointId").is(pointValueDto.getPointId()));
                    }
                }
            }
        } else if (null != pointValueDto.getPointId()) {
            R<Point> r = pointClient.selectById(pointValueDto.getPointId());
            if (r.isOk()) {
                criteria.orOperator(
                        (new Criteria()).and("pointId").is(pointValueDto.getPointId()),
                        (new Criteria()).and("children").elemMatch((new Criteria()).and("pointId").is(pointValueDto.getPointId()))
                );
            }
        }

        Pages pages = Optional.ofNullable(pointValueDto.getPage()).orElse(new Pages());
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and("originTime").gte(pages.getStartTime()).lte(pages.getEndTime());
        }

        Future<Long> count = threadPoolExecutor.submit(() -> {
            Query query = new Query(criteria);
            return mongoTemplate.count(query, PointValue.class);
        });

        Future<List<PointValue>> pointValues = threadPoolExecutor.submit(() -> {
            Query query = new Query(criteria);
            query.limit((int) pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
            query.with(Sort.by(Sort.Direction.DESC, "originTime"));
            return mongoTemplate.find(query, PointValue.class);
        });

        return (new Page<PointValue>()).setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count.get()).setRecords(pointValues.get());
    }

    /**
     * Save point value to mongo
     *
     * @param pointValue Point Value
     */
    private void savePointValueToMongo(final PointValue pointValue) {
        mongoTemplate.insert(pointValue);
    }

    /**
     * Save point value array to mongo
     *
     * @param pointValues Point Value Array
     */
    private void savePointValuesToMongo(final List<PointValue> pointValues) {
        mongoTemplate.insert(pointValues, PointValue.class);
    }

    /**
     * Save point value to redis
     *
     * @param pointValue Point Value
     */
    private void savePointValueToRedis(final PointValue pointValue) {
        String pointIdKey = pointValue.getPointId() != null ? String.valueOf(pointValue.getPointId()) : Common.Cache.ASTERISK;
        redisUtil.setKey(
                Common.Cache.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + Common.Cache.DOT + pointIdKey,
                pointValue,
                pointValue.getTimeOut(),
                pointValue.getTimeUnit()
        );
    }

    /**
     * Save point value array to redis
     *
     * @param pointValues Point Value Array
     */
    private void savePointValuesToRedis(final List<PointValue> pointValues) {
        pointValues.forEach(this::savePointValueToRedis);
    }

}

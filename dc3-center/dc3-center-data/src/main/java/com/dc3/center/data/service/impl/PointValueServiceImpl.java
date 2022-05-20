/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
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
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.constant.CacheConstant;
import com.dc3.common.constant.CommonConstant;
import com.dc3.common.dto.PointValueDto;
import com.dc3.common.model.Device;
import com.dc3.common.model.Point;
import com.dc3.common.utils.RedisUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
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
    public void savePointValue(PointValue pointValue) {
        if (null != pointValue) {
            pointValue.setCreateTime(new Date());
            threadPoolExecutor.execute(() -> {
                try {
                    dataCustomService.postHandle(pointValue);
                } catch (Exception e) {
                    log.error("Save point values to post handle error {}", e.getMessage());
                }
            });
            threadPoolExecutor.execute(() -> {
                try {
                    savePointValueToMongo(pointValue.getDeviceId(), pointValue);
                } catch (Exception e) {
                    log.error("Save point values to mongo error {}", e.getMessage());
                }
            });
            threadPoolExecutor.execute(() -> {
                try {
                    savePointValueToRedis(pointValue);
                } catch (Exception e) {
                    log.error("Save point values to redis error {}", e.getMessage());
                }
            });
        }
    }

    @Override
    public void savePointValues(List<PointValue> pointValues) {
        if (null != pointValues) {
            if (pointValues.size() > 0) {

                final List<PointValue> saveValues = pointValues.stream().map(pointValue -> pointValue.setCreateTime(new Date())).collect(Collectors.toList());
                final Map<Long, List<PointValue>> listMap = saveValues.stream().collect(Collectors.groupingBy(PointValue::getDeviceId));

                threadPoolExecutor.execute(() -> {
                    try {
                        listMap.forEach((deviceId, list) -> dataCustomService.postHandle(deviceId, list));
                    } catch (Exception e) {
                        log.error("Save point values to post handle error {}", e.getMessage());
                    }
                });
                threadPoolExecutor.execute(() -> {
                    try {
                        listMap.forEach(this::savePointValuesToMongo);
                    } catch (Exception e) {
                        log.error("Save point values to mongo error {}", e.getMessage());
                    }
                });
                threadPoolExecutor.execute(() -> {
                    try {
                        List<PointValue> list = listMap.values().stream()
                                .reduce(new ArrayList<>(), (first, second) -> {
                                    first.add(second.get(second.size() - 1));
                                    return first;
                                });
                        savePointValuesToRedis(list);
                    } catch (Exception e) {
                        log.error("Save point values to redis error {}", e.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public List<PointValue> realtime(Long deviceId) {
        R<List<Point>> listR = pointClient.selectByDeviceId(deviceId);
        if (!listR.isOk()) {
            String prefix = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + deviceId + CommonConstant.Symbol.UNDERSCORE;
            List<String> keys = listR.getData().stream().map(point -> prefix + point.getId()).collect(Collectors.toList());
            if (keys.size() > 0) {
                List<PointValue> pointValues = redisUtil.getKey(keys, PointValue.class);
                pointValues = pointValues.stream().filter(Objects::nonNull).map(pointValue -> pointValue.setTimeOut(null).setTimeUnit(null)).collect(Collectors.toList());
                if (pointValues.size() > 0) {
                    return pointValues;
                }
            }
        }
        return null;
    }

    @Override
    public PointValue realtime(Long deviceId, Long pointId) {
        String key = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + deviceId + CommonConstant.Symbol.UNDERSCORE + pointId;
        PointValue pointValue = redisUtil.getKey(key, PointValue.class);
        if (null != pointValue) {
            pointValue.setTimeOut(null).setTimeUnit(null);
        }
        return pointValue;
    }

    @Override
    public List<PointValue> latest(Long deviceId) {
        List<PointValue> pointValues = new ArrayList<>();

        R<Device> deviceR = deviceClient.selectById(deviceId);
        if (!deviceR.isOk()) {
            return pointValues;
        }

        R<List<Point>> pointsR = pointClient.selectByDeviceId(deviceId);
        if (!pointsR.isOk()) {
            return pointValues;
        }

        pointsR.getData().forEach(point -> {
            PointValue pointValue = latestPointValue(deviceId, point.getId());
            if (null != pointValue) {
                pointValues.add(pointValue.setRw(point.getRw()).setType(point.getType()).setUnit(point.getUnit()));
            }
        });
        return pointValues.stream().filter(Objects::nonNull).map(pointValue -> pointValue.setTimeOut(null).setTimeUnit(null)).collect(Collectors.toList());
    }

    @Override
    public PointValue latest(Long deviceId, Long pointId) {
        R<Device> deviceR = deviceClient.selectById(deviceId);
        if (!deviceR.isOk()) {
            return null;
        }

        R<Point> pointR = pointClient.selectById(pointId);
        if (!pointR.isOk()) {
            return null;
        }

        Point point = pointR.getData();
        PointValue pointValue = latestPointValue(deviceId, point.getId());
        if (null != pointValue) {
            pointValue.setRw(point.getRw()).setType(point.getType()).setUnit(point.getUnit());
            pointValue.setTimeOut(null).setTimeUnit(null);
        }

        return pointValue;
    }

    @Override
    @SneakyThrows
    public Page<PointValue> list(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        pointValueDto = Optional.ofNullable(pointValueDto).orElse(new PointValueDto());

        if (null != pointValueDto.getDeviceId()) {
            R<Device> deviceR = deviceClient.selectById(pointValueDto.getDeviceId());
            if (deviceR.isOk()) {
                Device device = deviceR.getData();
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
            R<Point> pointR = pointClient.selectById(pointValueDto.getPointId());
            if (pointR.isOk()) {
                criteria.orOperator(
                        (new Criteria()).and("pointId").is(pointValueDto.getPointId()),
                        (new Criteria()).and("children").elemMatch((new Criteria()).and("pointId").is(pointValueDto.getPointId()))
                );
            }
        }

        Pages pages = Optional.ofNullable(pointValueDto.getPage()).orElse(new Pages());
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and("originTime").gte(new Date(pages.getStartTime())).lte(new Date(pages.getEndTime()));
        }

        final String collection = null != pointValueDto.getDeviceId() ? pointValueDto.getDeviceId().toString() : "pointValue";
        Future<Long> count = threadPoolExecutor.submit(() -> {
            Query query = new Query(criteria);
            return mongoTemplate.count(query, PointValue.class, collection);
        });

        Future<List<PointValue>> pointValues = threadPoolExecutor.submit(() -> {
            Query query = new Query(criteria);
            query.limit(pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
            query.with(Sort.by(Sort.Direction.DESC, "originTime"));
            return mongoTemplate.find(query, PointValue.class, collection);
        });

        Page<PointValue> pointValuePage = new Page<>();
        List<PointValue> values = pointValues.get().stream().filter(Objects::nonNull).map(pointValue -> pointValue.setTimeOut(null).setTimeUnit(null)).collect(Collectors.toList());
        pointValuePage.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count.get()).setRecords(values);

        return pointValuePage;
    }

    /**
     * Ensure device point & time index
     *
     * @param collection Collection Name
     */
    public void ensurePointValueIndex(String collection) {
        // ensure point index
        Index pointIndex = new Index();
        pointIndex.background().on("pointId", Sort.Direction.DESC).named("IX_point");
        mongoTemplate.indexOps(collection).ensureIndex(pointIndex);

        // ensure time index
        Index timeIndex = new Index();
        timeIndex.background().on("originTime", Sort.Direction.DESC).named("IX_time");
        mongoTemplate.indexOps(collection).ensureIndex(timeIndex);
    }

    /**
     * Save point value to mongo
     *
     * @param deviceId   Device Id
     * @param pointValue Point Value
     */
    private void savePointValueToMongo(final Long deviceId, final PointValue pointValue) {
        String collection = deviceId.toString();
        ensurePointValueIndex(collection);
        mongoTemplate.insert(pointValue, collection);
    }

    /**
     * Save point value array to mongo
     *
     * @param deviceId    Device Id
     * @param pointValues Point Value Array
     */
    private void savePointValuesToMongo(final Long deviceId, final List<PointValue> pointValues) {
        String collection = deviceId.toString();
        ensurePointValueIndex(collection);
        mongoTemplate.insert(pointValues, collection);
    }

    /**
     * Save point value to redis
     *
     * @param pointValue Point Value
     */
    private void savePointValueToRedis(final PointValue pointValue) {
        String pointIdKey = pointValue.getPointId() != null ? String.valueOf(pointValue.getPointId()) : CommonConstant.Symbol.ASTERISK;
        redisUtil.setKey(
                CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + CommonConstant.Symbol.DOT + pointIdKey,
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
        Map<String, Object> valueMap = new HashMap<>(16);
        Map<String, Long> expireMap = new HashMap<>(16);
        for (PointValue pointValue : pointValues) {
            String pointIdKey = pointValue.getPointId() != null ? String.valueOf(pointValue.getPointId()) : CommonConstant.Symbol.ASTERISK;
            String key = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + pointValue.getDeviceId() + CommonConstant.Symbol.DOT + pointIdKey;
            valueMap.put(key, pointValue);
            expireMap.put(key, pointValue.getTimeUnit().toMillis(pointValue.getTimeOut()));
        }
        redisUtil.setKey(valueMap, expireMap);
    }

    private PointValue latestPointValue(Long deviceId, Long pointId) {
        PointValue pointValue;

        Criteria criteria = new Criteria();
        criteria.and("pointId").is(pointId);

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        pointValue = mongoTemplate.findOne(query, PointValue.class, deviceId.toString());

        if (null != pointValue) {
            pointValue.setTimeOut(null).setTimeUnit(null);
        }

        return pointValue;
    }

}

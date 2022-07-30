/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.DeviceClient;
import io.github.pnoker.api.center.manager.feign.PointClient;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.center.data.service.RepositoryHandleService;
import io.github.pnoker.common.bean.Pages;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.CacheConstant;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.dto.PointValueDto;
import io.github.pnoker.common.model.Device;
import io.github.pnoker.common.model.Point;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private PointClient pointClient;
    @Resource
    private DeviceClient deviceClient;

    @Resource
    private RepositoryHandleService repositoryHandleService;

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void savePointValue(PointValue pointValue) {
        if (ObjectUtil.isNull(pointValue)) {
            return;
        }

        final PointValue repositoryValue = pointValue.setCreateTime(new Date());
        repositoryHandleService.save(repositoryValue);
    }

    @Override
    public void savePointValues(List<PointValue> pointValues) {
        if (CollectionUtil.isEmpty(pointValues)) {
            return;
        }

        final List<PointValue> repositoryValues = pointValues.stream().map(pointValue -> pointValue.setCreateTime(new Date())).collect(Collectors.toList());
        repositoryHandleService.save(repositoryValues);
    }

    @Override
    public List<PointValue> realtime(String deviceId) {
        R<List<Point>> listR = pointClient.selectByDeviceId(deviceId);
        if (!listR.isOk()) {
            String prefix = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + deviceId + CommonConstant.Symbol.UNDERSCORE;
            List<String> keys = listR.getData().stream().map(point -> prefix + point.getId()).collect(Collectors.toList());
            if (keys.size() > 0) {
                List<PointValue> pointValues = redisUtil.getKey(keys, PointValue.class);
                pointValues = pointValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
                if (pointValues.size() > 0) {
                    return pointValues;
                }
            }
        }
        return null;
    }

    @Override
    public PointValue realtime(String deviceId, String pointId) {
        String key = CacheConstant.Prefix.REAL_TIME_VALUE_KEY_PREFIX + deviceId + CommonConstant.Symbol.UNDERSCORE + pointId;
        return redisUtil.getKey(key, PointValue.class);
    }

    @Override
    public List<PointValue> latest(String deviceId) {
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
                pointValues.add(pointValue);
            }
        });
        return pointValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public PointValue latest(String deviceId, String pointId) {
        R<Device> deviceR = deviceClient.selectById(deviceId);
        if (!deviceR.isOk()) {
            return null;
        }

        R<Point> pointR = pointClient.selectById(pointId);
        if (!pointR.isOk()) {
            return null;
        }

        Point point = pointR.getData();
        return latestPointValue(deviceId, point.getId());
    }

    @Override
    @SneakyThrows
    public Page<PointValue> list(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        pointValueDto = Optional.ofNullable(pointValueDto).orElse(new PointValueDto());

        if (StrUtil.isNotBlank(pointValueDto.getDeviceId())) {
            R<Device> deviceR = deviceClient.selectById(pointValueDto.getDeviceId());
            if (deviceR.isOk()) {
                Device device = deviceR.getData();
                criteria.and("deviceId").is(pointValueDto.getDeviceId());
                if (!device.getMulti()) {
                    if (StrUtil.isNotBlank(pointValueDto.getPointId())) {
                        criteria.and("pointId").is(pointValueDto.getPointId());
                    }
                } else if (StrUtil.isNotBlank(pointValueDto.getPointId())) {
                    criteria.and("multi").is(true);
                    if (StrUtil.isNotBlank(pointValueDto.getPointId())) {
                        criteria.and("children").elemMatch((new Criteria()).and("pointId").is(pointValueDto.getPointId()));
                    }
                }
            }
        } else if (StrUtil.isNotBlank(pointValueDto.getPointId())) {
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

        final String collection = null != pointValueDto.getDeviceId() ? CommonConstant.Storage.POINT_VALUE_PREFIX + pointValueDto.getDeviceId() : "point_value";
        Future<Long> count = threadPoolExecutor.submit(() -> {
            Query query = new Query(criteria);
            return mongoTemplate.count(query, PointValue.class, collection);
        });

        Future<List<PointValue>> pointValues = threadPoolExecutor.submit(() -> {
            Query query = new Query(criteria);
            query.limit((int) pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
            query.with(Sort.by(Sort.Direction.DESC, "originTime"));
            return mongoTemplate.find(query, PointValue.class, collection);
        });

        Page<PointValue> pointValuePage = new Page<>();
        List<PointValue> values = pointValues.get().stream().filter(Objects::nonNull).collect(Collectors.toList());
        pointValuePage.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count.get()).setRecords(values);

        return pointValuePage;
    }

    private PointValue latestPointValue(String deviceId, String pointId) {
        PointValue pointValue;

        Criteria criteria = new Criteria();
        criteria.and("pointId").is(pointId);

        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        pointValue = mongoTemplate.findOne(query, PointValue.class, CommonConstant.Storage.POINT_VALUE_PREFIX + deviceId);

        return pointValue;
    }

}

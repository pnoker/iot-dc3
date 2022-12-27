/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.data.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.dto.PointValueDto;
import io.github.pnoker.api.center.manager.dto.PointDto;
import io.github.pnoker.api.center.manager.feign.PointClient;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.center.data.service.RepositoryHandleService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.bean.entity.BaseModel;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.entity.Point;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {

    @Resource
    private PointClient pointClient;

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

        pointValue.setCreateTime(new Date());
        repositoryHandleService.save(pointValue);
    }

    @Override
    public void savePointValues(List<PointValue> pointValues) {
        if (CollUtil.isEmpty(pointValues)) {
            return;
        }

        final List<PointValue> repositoryValues = pointValues.stream().peek(pointValue -> pointValue.setCreateTime(new Date())).collect(Collectors.toList());
        repositoryHandleService.save(repositoryValues);
    }

    @Override
    public Page<PointValue> latest(PointValueDto pointValueDto, String tenantId) {
        Page<PointValue> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pointValueDto.getPage())) pointValueDto.setPage(new Pages());
        pointValuePage.setCurrent(pointValueDto.getPage().getCurrent()).setSize(pointValueDto.getPage().getSize());

        PointDto pointDto = new PointDto();
        pointDto.setDeviceId(pointValueDto.getDeviceId());
        pointDto.setPage(pointValueDto.getPage());
        pointDto.setPointName(pointValueDto.getPointName());
        pointDto.setEnableFlag(pointValueDto.getEnableFlag());
        R<Page<Point>> pageR = pointClient.list(pointDto, tenantId);
        if (!pageR.isOk()) return pointValuePage;

        List<Point> points = pageR.getData().getRecords();
        List<String> pointIds = points.stream().map(BaseModel::getId).collect(Collectors.toList());
        List<PointValue> pointValues = realtime(pointValueDto.getDeviceId(), pointIds);
        if (CollUtil.isEmpty(pointValues)) pointValues = latest(pointValueDto.getDeviceId(), pointIds);
        pointValuePage.setCurrent(pageR.getData().getCurrent()).setSize(pageR.getData().getSize()).setTotal(pageR.getData().getTotal()).setRecords(pointValues);

        // 返回最近100个非字符类型的历史值
        if (Boolean.TRUE.equals(pointValueDto.getHistory())) {
            pointValues.parallelStream().forEach(pointValue -> pointValue.setChildren(historyPointValue(pointValueDto.getDeviceId(), pointValue.getPointId(), 50)));
        }

        return pointValuePage;
    }

    @Override
    @SneakyThrows
    public Page<PointValue> list(PointValueDto pointValueDto, String tenantId) {
        long start = System.currentTimeMillis();
        Page<PointValue> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pointValueDto.getPage())) pointValueDto.setPage(new Pages());

        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        if (CharSequenceUtil.isNotEmpty(pointValueDto.getDeviceId()))
            criteria.and(FieldUtil.getField(PointValue::getDeviceId)).is(pointValueDto.getDeviceId());
        if (CharSequenceUtil.isNotEmpty(pointValueDto.getPointId()))
            criteria.and(FieldUtil.getField(PointValue::getPointId)).is(pointValueDto.getPointId());

        Pages pages = pointValueDto.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and(FieldUtil.getField(PointValue::getCreateTime)).gte(new Date(pages.getStartTime())).lte(new Date(pages.getEndTime()));
        }

        final String collection = CharSequenceUtil.isNotEmpty(pointValueDto.getDeviceId()) ? StorageConstant.POINT_VALUE_PREFIX + pointValueDto.getDeviceId() : PrefixConstant.POINT + SuffixConstant.VALUE;
        Future<Long> count = threadPoolExecutor.submit(() -> mongoTemplate.count(query, collection));

        Future<List<PointValue>> pointValues = threadPoolExecutor.submit(() -> {
            query.limit((int) pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
            query.with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValue::getCreateTime)));
            return mongoTemplate.find(query, PointValue.class, collection);
        });

        pointValuePage.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count.get()).setRecords(pointValues.get());
        long end = System.currentTimeMillis();
        log.info("end:{}", end - start);
        return pointValuePage;
    }

    public List<PointValue> realtime(String deviceId, List<String> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

        String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + deviceId + SymbolConstant.DOT;
        List<String> keys = pointIds.stream().map(pointId -> prefix + pointId).collect(Collectors.toList());
        List<PointValue> pointValues = redisUtil.getKey(keys);
        return pointValues.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<PointValue> latest(String deviceId, List<String> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

        return pointIds.stream().map(pointId -> latestPointValue(deviceId, pointId)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public PointValue realtime(String deviceId, String pointId) {
        String key = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + deviceId + SymbolConstant.UNDERSCORE + pointId;
        return redisUtil.getKey(key);
    }

    public PointValue latest(String deviceId, String pointId) {
        return latestPointValue(deviceId, pointId);
    }

    private PointValue latestPointValue(String deviceId, String pointId) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        criteria.and(FieldUtil.getField(PointValue::getPointId)).is(pointId);
        query.with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValue::getCreateTime)));

        return mongoTemplate.findOne(query, PointValue.class, StorageConstant.POINT_VALUE_PREFIX + deviceId);
    }

    private List<String> historyPointValue(String deviceId, String pointId, int count) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        criteria.and(FieldUtil.getField(PointValue::getDeviceId)).is(deviceId).and(FieldUtil.getField(PointValue::getPointId)).is(pointId);
        query.fields().include(FieldUtil.getField(PointValue::getValue)).exclude(FieldUtil.getField(PointValue::getId));
        query.limit(count).with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValue::getCreateTime)));

        List<PointValue> pointValues = mongoTemplate.find(query, PointValue.class, StorageConstant.POINT_VALUE_PREFIX + deviceId);
        return pointValues.stream().map(PointValue::getValue).collect(Collectors.toList());
    }
}

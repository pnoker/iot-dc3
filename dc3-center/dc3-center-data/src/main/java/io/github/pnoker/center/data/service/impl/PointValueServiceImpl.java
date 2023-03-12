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
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.PagePointQuery;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.center.manager.PointDTO;
import io.github.pnoker.api.center.manager.RPagePointDTO;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.PageDTO;
import io.github.pnoker.center.data.entity.query.PointValuePageQuery;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.center.data.service.RepositoryHandleService;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.point.PointValue;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
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

    @GrpcClient(ManagerServiceConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

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
    public Page<PointValue> latest(PointValuePageQuery pointValuePageQuery, String tenantId) {
        Page<PointValue> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pointValuePageQuery.getPage())) pointValuePageQuery.setPage(new Pages());
        pointValuePage.setCurrent(pointValuePageQuery.getPage().getCurrent()).setSize(pointValuePageQuery.getPage().getSize());

        PageDTO.Builder page = PageDTO.newBuilder()
                .setSize(pointValuePageQuery.getPage().getSize())
                .setCurrent(pointValuePageQuery.getPage().getCurrent());
        PointDTO.Builder point = PointDTO.newBuilder();
        if (ObjectUtil.isNotNull(pointValuePageQuery.getEnableFlag())) {
            point.setEnableFlag(EnableFlagDTOEnum.valueOf(pointValuePageQuery.getEnableFlag().name()));
        }
        if (StrUtil.isNotEmpty(pointValuePageQuery.getPointName())) {
            point.setPointName(pointValuePageQuery.getPointName());
        }
        PagePointQuery query = PagePointQuery.newBuilder()
                .setTenantId(tenantId)
                .setDeviceId(pointValuePageQuery.getDeviceId())
                .setPage(page)
                .setPoint(point)
                .build();
        RPagePointDTO rPagePointDTO = pointApiBlockingStub.list(query);

        if (!rPagePointDTO.getResult().getOk()) return pointValuePage;

        List<PointDTO> points = rPagePointDTO.getData().getDataList();
        List<String> pointIds = points.stream().map(p -> p.getBase().getId()).collect(Collectors.toList());
        List<PointValue> pointValues = realtime(pointValuePageQuery.getDeviceId(), pointIds);
        if (CollUtil.isEmpty(pointValues)) pointValues = latest(pointValuePageQuery.getDeviceId(), pointIds);
        pointValuePage.setCurrent(rPagePointDTO.getData().getPage().getCurrent()).setSize(rPagePointDTO.getData().getPage().getSize()).setTotal(rPagePointDTO.getData().getPage().getTotal()).setRecords(pointValues);

        // 返回最近100个非字符类型的历史值
        if (Boolean.TRUE.equals(pointValuePageQuery.getHistory())) {
            pointValues.parallelStream().forEach(pointValue -> pointValue.setChildren(historyPointValue(pointValuePageQuery.getDeviceId(), pointValue.getPointId(), 50)));
        }

        return pointValuePage;
    }

    @Override
    @SneakyThrows
    public Page<PointValue> list(PointValuePageQuery pointValuePageQuery, String tenantId) {
        long start = System.currentTimeMillis();
        Page<PointValue> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pointValuePageQuery.getPage())) pointValuePageQuery.setPage(new Pages());

        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        if (CharSequenceUtil.isNotEmpty(pointValuePageQuery.getDeviceId()))
            criteria.and(FieldUtil.getField(PointValue::getDeviceId)).is(pointValuePageQuery.getDeviceId());
        if (CharSequenceUtil.isNotEmpty(pointValuePageQuery.getPointId()))
            criteria.and(FieldUtil.getField(PointValue::getPointId)).is(pointValuePageQuery.getPointId());

        Pages pages = pointValuePageQuery.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and(FieldUtil.getField(PointValue::getCreateTime)).gte(new Date(pages.getStartTime())).lte(new Date(pages.getEndTime()));
        }

        final String collection = CharSequenceUtil.isNotEmpty(pointValuePageQuery.getDeviceId()) ? StorageConstant.POINT_VALUE_PREFIX + pointValuePageQuery.getDeviceId() : PrefixConstant.POINT + SuffixConstant.VALUE;
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

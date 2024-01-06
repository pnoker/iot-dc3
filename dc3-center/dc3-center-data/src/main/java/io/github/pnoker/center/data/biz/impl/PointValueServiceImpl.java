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

package io.github.pnoker.center.data.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.GrpcPagePointQueryDTO;
import io.github.pnoker.api.center.manager.GrpcPointDTO;
import io.github.pnoker.api.center.manager.GrpcRPagePointDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.common.GrpcPageDTO;
import io.github.pnoker.center.data.biz.PointValueRepositoryService;
import io.github.pnoker.center.data.biz.PointValueService;
import io.github.pnoker.center.data.entity.bo.PointValueBO;
import io.github.pnoker.center.data.entity.query.PointValueQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;

    @Resource
    private PointValueRepositoryService pointValueRepositoryService;

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public void savePointValue(PointValueBO pointValueBO) {
        if (ObjectUtil.isNull(pointValueBO)) {
            return;
        }

        pointValueBO.setCreateTime(LocalDateTime.now());
        pointValueRepositoryService.save(pointValueBO);
    }

    @Override
    public void savePointValues(List<PointValueBO> pointValueBOS) {
        if (CollUtil.isEmpty(pointValueBOS)) {
            return;
        }

        pointValueBOS.forEach(pointValue -> pointValue.setCreateTime(LocalDateTime.now()));
        pointValueRepositoryService.save(pointValueBOS);
    }

    @Override
    public Page<PointValueBO> latest(PointValueQuery pageQuery) {
        Page<PointValueBO> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pageQuery.getPage())) pageQuery.setPage(new Pages());
        pointValuePage.setCurrent(pageQuery.getPage().getCurrent()).setSize(pageQuery.getPage().getSize());

        GrpcPageDTO.Builder page = GrpcPageDTO.newBuilder()
                .setSize(pageQuery.getPage().getSize())
                .setCurrent(pageQuery.getPage().getCurrent());
        GrpcPointDTO.Builder builder = buildDTOByQuery(pageQuery);
        GrpcPagePointQueryDTO.Builder query = GrpcPagePointQueryDTO.newBuilder()
                .setPage(page)
                .setPoint(builder);
        if (ObjectUtil.isNotEmpty(pageQuery.getDeviceId())) {
            query.setDeviceId(pageQuery.getDeviceId());
        }
        GrpcRPagePointDTO rPagePointDTO = pointApiBlockingStub.list(query.build());

        if (!rPagePointDTO.getResult().getOk()) {
            return pointValuePage;
        }

        List<GrpcPointDTO> points = rPagePointDTO.getData().getDataList();
        List<Long> pointIds = points.stream().map(p -> p.getBase().getId()).collect(Collectors.toList());
        List<PointValueBO> pointValueBOS = realtime(pageQuery.getDeviceId(), pointIds);
        if (CollUtil.isEmpty(pointValueBOS)) {
            pointValueBOS = latest(pageQuery.getDeviceId(), pointIds);
        }
        pointValuePage.setCurrent(rPagePointDTO.getData().getPage().getCurrent()).setSize(rPagePointDTO.getData().getPage().getSize()).setTotal(rPagePointDTO.getData().getPage().getTotal()).setRecords(pointValueBOS);

        // 返回最近100个非字符类型的历史值
        if (Boolean.TRUE.equals(pageQuery.getHistory())) {
            pointValueBOS.parallelStream().forEach(pointValue -> pointValue.setChildren(historyPointValue(pageQuery.getDeviceId(), pointValue.getPointId(), 100)));
        }

        return pointValuePage;
    }

    @Override
    @SneakyThrows
    public Page<PointValueBO> list(PointValueQuery pageQuery) {
        Page<PointValueBO> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pageQuery.getPage())) pageQuery.setPage(new Pages());

        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        if (ObjectUtil.isNotEmpty(pageQuery.getDeviceId()))
            criteria.and(FieldUtil.getField(PointValueBO::getDeviceId)).is(pageQuery.getDeviceId());
        if (ObjectUtil.isNotEmpty(pageQuery.getPointId()))
            criteria.and(FieldUtil.getField(PointValueBO::getPointId)).is(pageQuery.getPointId());

        Pages pages = pageQuery.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and(FieldUtil.getField(PointValueBO::getCreateTime)).gte(new Date(pages.getStartTime())).lte(new Date(pages.getEndTime()));
        }

        final String collection = ObjectUtil.isNotEmpty(pageQuery.getDeviceId()) ? StorageConstant.POINT_VALUE_PREFIX + pageQuery.getDeviceId() : PrefixConstant.POINT + SuffixConstant.VALUE;
        long count = mongoTemplate.count(query, collection);
        query.limit((int) pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
        query.with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValueBO::getCreateTime)));
        List<PointValueBO> pointValueBOS = mongoTemplate.find(query, PointValueBO.class, collection);
        pointValuePage.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count).setRecords(pointValueBOS);
        return pointValuePage;
    }

    /**
     * Query to DTO
     *
     * @param pageQuery PointValuePageQuery
     * @return PointDTO Builder
     */
    private static GrpcPointDTO.Builder buildDTOByQuery(PointValueQuery pageQuery) {
        GrpcPointDTO.Builder builder = GrpcPointDTO.newBuilder();

        if (CharSequenceUtil.isNotEmpty(pageQuery.getPointName())) {
            builder.setPointName(pageQuery.getPointName());
        }
        builder.setPointTypeFlag(DefaultConstant.DEFAULT_INT);
        builder.setRwFlag(DefaultConstant.DEFAULT_INT);
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            builder.setEnableFlag(pageQuery.getEnableFlag().getIndex());
        } else {
            builder.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        return builder;
    }

    public List<PointValueBO> realtime(Long deviceId, List<Long> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

        String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + deviceId + SymbolConstant.DOT;
        List<String> keys = pointIds.stream().map(pointId -> prefix + pointId).collect(Collectors.toList());
        List<PointValueBO> pointValueBOS = redisUtil.getKey(keys);
        return pointValueBOS.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<PointValueBO> latest(Long deviceId, List<Long> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

        return pointIds.stream().map(pointId -> latestPointValue(deviceId, pointId)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private PointValueBO latestPointValue(Long deviceId, Long pointId) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        criteria.and(FieldUtil.getField(PointValueBO::getPointId)).is(pointId);
        query.with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValueBO::getCreateTime)));

        return mongoTemplate.findOne(query, PointValueBO.class, StorageConstant.POINT_VALUE_PREFIX + deviceId);
    }

    private List<String> historyPointValue(Long deviceId, Long pointId, int count) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        criteria.and(FieldUtil.getField(PointValueBO::getDeviceId)).is(deviceId).and(FieldUtil.getField(PointValueBO::getPointId)).is(pointId);
        query.fields().include(FieldUtil.getField(PointValueBO::getValue)).exclude(FieldUtil.getField(PointValueBO::getId));
        query.limit(count).with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValueBO::getCreateTime)));

        List<PointValueBO> pointValueBOS = mongoTemplate.find(query, PointValueBO.class, StorageConstant.POINT_VALUE_PREFIX + deviceId);
        return pointValueBOS.stream().map(PointValueBO::getValue).collect(Collectors.toList());
    }
}

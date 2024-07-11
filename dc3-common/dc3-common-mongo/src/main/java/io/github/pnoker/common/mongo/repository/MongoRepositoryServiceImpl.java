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

package io.github.pnoker.common.mongo.repository;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.mongo.entity.builder.MgPointValueBuilder;
import io.github.pnoker.common.mongo.entity.model.MgPointValueDO;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.utils.FieldUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service("mongoRepositoryService")
public class MongoRepositoryServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private MgPointValueBuilder mgPointValueBuilder;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.MONGO;
    }

    @Override
    public void savePointValue(PointValueBO entityBO) {
        if (Objects.isNull(entityBO.getDeviceId()) || Objects.isNull(entityBO.getPointId())) {
            return;
        }

        final String collection = StorageConstant.POINT_VALUE_PREFIX + entityBO.getDeviceId();
        ensurePointValueIndex(collection);
        MgPointValueDO entityDO = mgPointValueBuilder.buildMgDOByBO(entityBO);
        mongoTemplate.insert(entityDO, collection);
    }

    @Override
    public void savePointValue(Long deviceId, List<PointValueBO> entityBOList) {
        if (Objects.isNull(deviceId)) {
            return;
        }

        final String collection = StorageConstant.POINT_VALUE_PREFIX + deviceId;
        ensurePointValueIndex(collection);
        List<MgPointValueDO> entityDOList = mgPointValueBuilder.buildMgDOListByBOList(entityBOList);
        entityDOList = entityDOList.stream()
                .filter(entityBO -> Objects.nonNull(entityBO.getPointId()))
                .toList();
        mongoTemplate.insert(entityDOList, collection);
    }

    @Override
    public List<String> selectHistoryPointValue(Long deviceId, Long pointId, int count) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        criteria.and(FieldUtil.getField(MgPointValueDO::getDeviceId)).is(deviceId).and(FieldUtil.getField(MgPointValueDO::getPointId)).is(pointId);
        query.fields().include(FieldUtil.getField(MgPointValueDO::getValue)).exclude(FieldUtil.getField(MgPointValueDO::getId));
        query.limit(count).with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(MgPointValueDO::getCreateTime)));

        List<MgPointValueDO> entityDOList = mongoTemplate.find(query, MgPointValueDO.class, StorageConstant.POINT_VALUE_PREFIX + deviceId);
        return entityDOList.stream().map(MgPointValueDO::getValue).toList();
    }

    @Override
    public PointValueBO selectLatestPointValue(Long deviceId, Long pointId) {
        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        criteria.and(FieldUtil.getField(MgPointValueDO::getDeviceId)).is(deviceId).and(FieldUtil.getField(MgPointValueDO::getPointId)).is(pointId);
        query.fields().include(FieldUtil.getField(MgPointValueDO::getValue)).exclude(FieldUtil.getField(MgPointValueDO::getId));
        query.limit(1).with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(MgPointValueDO::getCreateTime)));
        MgPointValueDO entityDO = mongoTemplate.findOne(query, MgPointValueDO.class, StorageConstant.POINT_VALUE_PREFIX + deviceId);
        return mgPointValueBuilder.buildBOByMgDO(entityDO);
    }

    @Override
    public List<PointValueBO> selectLatestPointValue(Long deviceId, List<Long> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

        Criteria criteria = new Criteria();
        criteria.and(FieldUtil.getField(MgPointValueDO::getPointId)).in(pointIds);
        MatchOperation match = Aggregation.match(criteria);
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, FieldUtil.getField(MgPointValueDO::getCreateTime));
        GroupOperation group = Aggregation.group(FieldUtil.getField(MgPointValueDO::getPointId))
                .first(FieldUtil.getField(MgPointValueDO::getId)).as(FieldUtil.getField(MgPointValueDO::getId))
                .first(FieldUtil.getField(MgPointValueDO::getDeviceId)).as(FieldUtil.getField(MgPointValueDO::getDeviceId))
                .first(FieldUtil.getField(MgPointValueDO::getPointId)).as(FieldUtil.getField(MgPointValueDO::getPointId))
                .first(FieldUtil.getField(MgPointValueDO::getRawValue)).as(FieldUtil.getField(MgPointValueDO::getRawValue))
                .first(FieldUtil.getField(MgPointValueDO::getValue)).as(FieldUtil.getField(MgPointValueDO::getValue))
                .first(FieldUtil.getField(MgPointValueDO::getOriginTime)).as(FieldUtil.getField(MgPointValueDO::getOriginTime))
                .first(FieldUtil.getField(MgPointValueDO::getCreateTime)).as(FieldUtil.getField(MgPointValueDO::getCreateTime));
        Aggregation aggregation = Aggregation.newAggregation(match, sort, group);
        String collection = StorageConstant.POINT_VALUE_PREFIX + deviceId;
        AggregationResults<MgPointValueDO> aggregate = mongoTemplate.aggregate(aggregation, collection, MgPointValueDO.class);
        List<MgPointValueDO> entityDOList = aggregate.getMappedResults();
        return mgPointValueBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<PointValueBO> selectPagePointValue(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }

        Page<PointValueBO> entityPageBO = new Page<>();

        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        if (Objects.nonNull(entityQuery.getDeviceId()))
            criteria.and(FieldUtil.getField(MgPointValueDO::getDeviceId)).is(entityQuery.getDeviceId());
        if (Objects.nonNull(entityQuery.getPointId()))
            criteria.and(FieldUtil.getField(MgPointValueDO::getPointId)).is(entityQuery.getPointId());

        Pages pages = entityQuery.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and(FieldUtil.getField(MgPointValueDO::getCreateTime)).gte(new Date(pages.getStartTime())).lte(new Date(pages.getEndTime()));
        }

        final String collection = Objects.nonNull(entityQuery.getDeviceId()) ? StorageConstant.POINT_VALUE_PREFIX + entityQuery.getDeviceId() : PrefixConstant.POINT + SuffixConstant.VALUE;
        long count = mongoTemplate.count(query, collection);
        query.limit((int) pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
        query.with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(MgPointValueDO::getCreateTime)));
        List<MgPointValueDO> pointValueDOList = mongoTemplate.find(query, MgPointValueDO.class, collection);
        List<PointValueBO> pointValueBOList = mgPointValueBuilder.buildBOListByDOList(pointValueDOList);
        entityPageBO.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count).setRecords(pointValueBOList);
        return entityPageBO;
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.MONGO, this);
    }

    /**
     * Ensure device point and time index
     *
     * @param collection Collection Name
     */
    private void ensurePointValueIndex(String collection) {
        // ensure point index
        Index pointIndex = new Index();
        pointIndex.background()
                .on("pointId", Sort.Direction.DESC)
                .named("IX_point_id");
        mongoTemplate.indexOps(collection).ensureIndex(pointIndex);

        // ensure time index
        Index timeIndex = new Index();
        timeIndex.background()
                .on("createTime", Sort.Direction.DESC)
                .named("IX_create_time");
        mongoTemplate.indexOps(collection).ensureIndex(timeIndex);
    }

}
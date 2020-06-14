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
import com.dc3.center.data.service.PointValueService;
import com.dc3.common.bean.Pages;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void add(PointValue pointValue) {
        long createTime = System.currentTimeMillis();
        long interval = createTime - pointValue.getOriginTime();
        mongoTemplate.insert(pointValue.setCreateTime(createTime).setInterval(interval));
        log.debug("interval:{}", interval);
    }

    @Override
    public void batchAdd(List<PointValue> pointValues) {
        pointValues.stream().map(pointValue -> {
            long createTime = System.currentTimeMillis();
            long interval = createTime - pointValue.getOriginTime();
            return pointValue.setCreateTime(createTime).setInterval(interval);
        });
        mongoTemplate.insert(pointValues, PointValue.class);
    }

    @Override
    public Page<PointValue> list(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        Optional.ofNullable(pointValueDto).ifPresent(dto -> {
            Optional.ofNullable(dto.getDeviceId()).ifPresent(deviceId -> criteria.and("deviceId").is(deviceId));
            Optional.ofNullable(dto.getPointId()).ifPresent(pointId -> criteria.and("pointId").is(pointId));
            if (dto.getPage().getStartTime() > 0 && dto.getPage().getEndTime() > 0 && dto.getPage().getStartTime() <= dto.getPage().getEndTime()) {
                criteria.and("originTime").gte(dto.getPage().getStartTime()).lte(dto.getPage().getEndTime());
            }
        });
        return pageQuery(criteria, pointValueDto.getPage());
    }

    @Override
    public PointValue latest(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        Optional.ofNullable(pointValueDto).ifPresent(dto -> {
            Optional.ofNullable(dto.getDeviceId()).ifPresent(deviceId -> criteria.and("deviceId").is(deviceId));
            Optional.ofNullable(dto.getPointId()).ifPresent(pointId -> criteria.and("pointId").is(pointId));
        });
        return oneQuery(criteria);
    }

    /**
     * 查询 One
     *
     * @param criteriaDefinition CriteriaDefinition
     * @return
     */
    private PointValue oneQuery(CriteriaDefinition criteriaDefinition) {
        return mongoTemplate.findOne(desc(criteriaDefinition), PointValue.class);
    }

    /**
     * 分页&排序&查询
     *
     * @param criteriaDefinition CriteriaDefinition
     * @param pages
     * @return
     */
    private Page<PointValue> pageQuery(CriteriaDefinition criteriaDefinition, Pages pages) {
        Query query = desc(criteriaDefinition);
        long count = mongoTemplate.count(query, PointValue.class);
        List<PointValue> pointValues = mongoTemplate.find(page(query, pages), PointValue.class);
        Page<PointValue> page = (new Page<PointValue>()).setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count);
        page.setRecords(pointValues);
        return page;
    }

    /**
     * 排序
     *
     * @param criteriaDefinition CriteriaDefinition
     * @return
     */
    private Query desc(CriteriaDefinition criteriaDefinition) {
        Query query = new Query(criteriaDefinition);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        return query;
    }

    /**
     * 分页
     *
     * @param query Query
     * @param pages Pages
     * @return
     */
    private Query page(Query query, Pages pages) {
        int size = (int) pages.getSize();
        long page = pages.getCurrent();
        query.limit(size).skip(size * (page - 1));
        return query;
    }
}

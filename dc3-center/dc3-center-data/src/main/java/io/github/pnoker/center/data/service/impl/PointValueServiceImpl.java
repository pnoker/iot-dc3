/*
 * Copyright 2016-present the original author or authors.
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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.data.PointValueQuery;
import io.github.pnoker.api.center.manager.PagePointQueryDTO;
import io.github.pnoker.api.center.manager.PointApiGrpc;
import io.github.pnoker.api.center.manager.PointDTO;
import io.github.pnoker.api.center.manager.RPagePointDTO;
import io.github.pnoker.api.common.EnableFlagDTOEnum;
import io.github.pnoker.api.common.PageDTO;
import io.github.pnoker.center.data.entity.vo.query.PointValuePageQuery;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.center.data.service.RepositoryHandleService;
import io.github.pnoker.common.constant.common.DefaultConstant;
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

        pointValues.forEach(pointValue -> pointValue.setCreateTime(new Date()));
        repositoryHandleService.save(pointValues);
    }

    @Override
    public Page<PointValue> latest(PointValuePageQuery pageQuery) {
        Page<PointValue> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pageQuery.getPage())) pageQuery.setPage(new Pages());
        pointValuePage.setCurrent(pageQuery.getPage().getCurrent()).setSize(pageQuery.getPage().getSize());

        PageDTO.Builder page = PageDTO.newBuilder()
                .setSize(pageQuery.getPage().getSize())
                .setCurrent(pageQuery.getPage().getCurrent());
        PointDTO.Builder builder = buildDTOByQuery(pageQuery);
        PagePointQueryDTO.Builder query = PagePointQueryDTO.newBuilder()
                .setPage(page)
                .setPoint(builder);
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDeviceId())) {
            query.setDeviceId(pageQuery.getDeviceId());
        }
        RPagePointDTO rPagePointDTO = pointApiBlockingStub.list(query.build());

        if (!rPagePointDTO.getResult().getOk()) {
            return pointValuePage;
        }

        List<PointDTO> points = rPagePointDTO.getData().getDataList();
        List<String> pointIds = points.stream().map(p -> p.getBase().getId()).collect(Collectors.toList());
        List<PointValue> pointValues = realtime(pageQuery.getDeviceId(), pointIds);
        if (CollUtil.isEmpty(pointValues)) {
            pointValues = latest(pageQuery.getDeviceId(), pointIds);
        }
        pointValuePage.setCurrent(rPagePointDTO.getData().getPage().getCurrent()).setSize(rPagePointDTO.getData().getPage().getSize()).setTotal(rPagePointDTO.getData().getPage().getTotal()).setRecords(pointValues);

        // 返回最近100个非字符类型的历史值
        if (Boolean.TRUE.equals(pageQuery.getHistory())) {
            pointValues.parallelStream().forEach(pointValue -> pointValue.setChildren(historyPointValue(pageQuery.getDeviceId(), pointValue.getPointId(), 100)));
        }

        return pointValuePage;
    }

    @Override
    @SneakyThrows
    public Page<PointValue> list(PointValuePageQuery pageQuery) {
        Page<PointValue> pointValuePage = new Page<>();
        if (ObjectUtil.isEmpty(pageQuery.getPage())) pageQuery.setPage(new Pages());

        Criteria criteria = new Criteria();
        Query query = new Query(criteria);
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDeviceId()))
            criteria.and(FieldUtil.getField(PointValue::getDeviceId)).is(pageQuery.getDeviceId());
        if (CharSequenceUtil.isNotEmpty(pageQuery.getPointId()))
            criteria.and(FieldUtil.getField(PointValue::getPointId)).is(pageQuery.getPointId());

        Pages pages = pageQuery.getPage();
        if (pages.getStartTime() > 0 && pages.getEndTime() > 0 && pages.getStartTime() <= pages.getEndTime()) {
            criteria.and(FieldUtil.getField(PointValue::getCreateTime)).gte(new Date(pages.getStartTime())).lte(new Date(pages.getEndTime()));
        }

        final String collection = CharSequenceUtil.isNotEmpty(pageQuery.getDeviceId()) ? StorageConstant.POINT_VALUE_PREFIX + pageQuery.getDeviceId() : PrefixConstant.POINT + SuffixConstant.VALUE;
        long count = mongoTemplate.count(query, collection);
        query.limit((int) pages.getSize()).skip(pages.getSize() * (pages.getCurrent() - 1));
        query.with(Sort.by(Sort.Direction.DESC, FieldUtil.getField(PointValue::getCreateTime)));
        List<PointValue> pointValues = mongoTemplate.find(query, PointValue.class, collection);
        pointValuePage.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count).setRecords(pointValues);
        return pointValuePage;
    }

    @Override
    public PointValue latest(PointValueQuery request) {
        final String prefix = PrefixConstant.REAL_TIME_VALUE_KEY_PREFIX + request.getDeviceId() + SymbolConstant.DOT;
        return redisUtil.getKey(prefix + request.getPointId());
    }

    /**
     * Query to DTO
     *
     * @param pageQuery PointValuePageQuery
     * @return PointDTO Builder
     */
    private static PointDTO.Builder buildDTOByQuery(PointValuePageQuery pageQuery) {
        PointDTO.Builder builder = PointDTO.newBuilder();

        if (CharSequenceUtil.isNotEmpty(pageQuery.getPointName())) {
            builder.setPointName(pageQuery.getPointName());
        }
        builder.setPointTypeFlagValue(DefaultConstant.DEFAULT_INT);
        builder.setRwFlagValue(DefaultConstant.DEFAULT_INT);
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            builder.setEnableFlag(EnableFlagDTOEnum.valueOf(pageQuery.getEnableFlag().name()));
        } else {
            builder.setEnableFlagValue(DefaultConstant.DEFAULT_INT);
        }
        return builder;
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

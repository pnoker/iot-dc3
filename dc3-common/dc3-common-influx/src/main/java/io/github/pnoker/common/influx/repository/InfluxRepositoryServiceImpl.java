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

package io.github.pnoker.common.influx.repository;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.influx.entity.builder.InfluxPointValueBuilder;
import io.github.pnoker.common.influx.entity.model.InfluxMapperBO;
import io.github.pnoker.common.influx.entity.model.InfluxMapperDO;
import io.github.pnoker.common.influx.entity.model.InfluxPointValueDO;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service("influxRepositoryService")
public class InfluxRepositoryServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private InfluxPointValueBuilder influxPointValueBuilder;
    @Resource
    private InfluxDBClient influxDBClient;

    /**
     * mapper转换DO
     *
     * @param listBo
     * @return
     */
    public static List<InfluxPointValueDO> convertMapperBoToValueDo(List<InfluxMapperBO> listBo) {
        List<InfluxPointValueDO> listDo = new ArrayList<>();

        for (InfluxMapperBO bo : listBo) {
            InfluxPointValueDO entity = new InfluxPointValueDO();
            entity.setDeviceId(Long.parseLong(bo.getDeviceId()));
            entity.setPointId(Long.parseLong(bo.getPointId()));
            entity.setRawValue(bo.getRawValue().toString());
            entity.setValue(bo.getValue().toString());
            entity.setOriginTime(LocalDateTime.parse(bo.getOriginTime())); // Assuming originTime is in ISO-8601 format

            // Convert Instant to LocalDateTime
            Instant instant = bo.getTime();
            LocalDateTime time = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            entity.setCreateTime(time);

            listDo.add(entity);
        }

        return listDo;
    }

    /**
     * 分页
     *
     * @param data
     * @param pageSize
     * @param pageNumber
     * @return
     */
    public static List<InfluxPointValueDO> getPage(List<InfluxPointValueDO> data, int pageSize, int pageNumber) {
        int startIndex = (pageNumber - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, data.size());
        if (startIndex >= endIndex) {
            return new ArrayList<>();
        }
        return data.subList(startIndex, endIndex);
    }

    @Override
    public PointValueBO selectLatestPointValue(Long deviceId, Long pointId) {
        return null;
    }

    /**
     * influx库转为BO类
     *
     * @param dos
     * @return
     */
    public static List<InfluxMapperBO> convertToBO(List<InfluxMapperDO> dos) {
        Map<String, InfluxMapperBO> boMap = new HashMap<>();

        for (InfluxMapperDO dobj : dos) {
            String key = dobj.getTime() + "-" + dobj.getDeviceId() + "-" + dobj.getPointId();

            InfluxMapperBO bo = boMap.get(key);
            if (bo == null) {
                bo = new InfluxMapperBO(dobj.getTime(), dobj.getDeviceId(), dobj.getPointId(), dobj.getOriginTime(), null, null);
                boMap.put(key, bo);
            }

            if ("value".equals(dobj.getField())) {
                bo.setValue(dobj.getValue());
            } else if ("rawValue".equals(dobj.getField())) {
                bo.setRawValue(dobj.getValue());
            }
        }

        return new ArrayList<>(boMap.values());
    }

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.INFLUXDB;
    }

    @Override
    public void savePointValue(PointValueBO entityBO) {
        if (Objects.isNull(entityBO.getDeviceId()) || Objects.isNull(entityBO.getPointId())) {
            return;
        }
        WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();
        InfluxPointValueDO influxPointValueDO = influxPointValueBuilder.buildMgDOByBO(entityBO);
        Point point = Point.measurement("dc3")
                .addTag("deviceId", influxPointValueDO.getDeviceId().toString())
                .addTag("pointId", influxPointValueDO.getPointId().toString())
                .addTag("originTime", influxPointValueDO.getOriginTime().toString())
                .addField("value", Long.valueOf(influxPointValueDO.getValue()))
                .addField("rawValue", Long.valueOf(influxPointValueDO.getRawValue()))
                .time(Instant.now(), WritePrecision.MS);
        writeApiBlocking.writePoint(point);

    }

    @Override
    public void savePointValue(Long deviceId, List<PointValueBO> entityBOList) {
        if (Objects.isNull(deviceId)) {
            return;
        }
        WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();
        List<InfluxPointValueDO> influxPointValueDOList = influxPointValueBuilder.buildMgDOListByBOList(entityBOList);
        for (InfluxPointValueDO influxPointValueDO : influxPointValueDOList) {
            Point point = Point.measurement("dc3")
                    .addTag("deviceId", influxPointValueDO.getDeviceId().toString())
                    .addTag("pointId", influxPointValueDO.getPointId().toString())
                    .addTag("originTime", influxPointValueDO.getOriginTime().toString())
                    .addField("value", Long.valueOf(influxPointValueDO.getValue()))
                    .addField("rawValue", Long.valueOf(influxPointValueDO.getRawValue()))
                    .time(Instant.now(), WritePrecision.MS);
            writeApiBlocking.writePoint(point);
        }
    }

    @Override
    public List<String> selectHistoryPointValue(Long deviceId, Long pointId, int count) {
        QueryApi queryApi = influxDBClient.getQueryApi();
        String flux = "from(bucket:\"dc3\")" +
                "|> range(start: -30d)" + // Filter by last 30 day
                "|> filter(fn: (r) => r._measurement == \"dc3\" and r.deviceId == \"" + deviceId + "\" and r.pointId == \"" + pointId + "\")" +
                "|> sort(columns: [\"_time\"], desc: true)";
        List<InfluxMapperDO> query = queryApi.query(flux, InfluxMapperDO.class);
        List<InfluxMapperBO> influxMapperBOList = convertToBO(query);
        List<InfluxMapperBO> sortedList = influxMapperBOList.stream()
                .sorted((a, b) -> b.getTime().compareTo(a.getTime())) // 倒序排序
                .toList();
        List<InfluxMapperBO> takenList = sortedList.subList(0, count);
        List<String> valueList = takenList.stream()
                .map(InfluxMapperBO::getValue) // 提取 value 属性
                .map(Object::toString) // 转换为字符串
                .toList();
        return valueList;
    }

    @Override
    public List<PointValueBO> selectLatestPointValue(Long deviceId, List<Long> pointIds) {
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }
        QueryApi queryApi = influxDBClient.getQueryApi();
        String pointIdsStr = pointIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining("|"));
        String flux = "from(bucket:\"dc3\")" +
                "|> range(start: -30d)" + // Filter by last 1 day
                "|> filter(fn: (r) => r._measurement == \"dc3\" and r.deviceId == \"" + deviceId + "\" and r.pointId =~ /" + pointIdsStr + "/)" +
                "|> group(columns: [\"pointId\"])";

        //   System.out.println(flux);
        // 执行查询
        List<InfluxMapperDO> query = queryApi.query(flux, InfluxMapperDO.class);
        List<InfluxMapperBO> influxMapperBOList = convertToBO(query);
        Map<String, InfluxMapperBO> latestDataMap = new HashMap<>();
        //  System.out.println("-------------------------------------------");
        for (InfluxMapperBO bo : influxMapperBOList) {
            String key = bo.getDeviceId() + "-" + bo.getPointId();
            if (!latestDataMap.containsKey(key) || bo.getTime().compareTo(latestDataMap.get(key).getTime()) > 0) {
                latestDataMap.put(key, bo);
            }
        }

        List<InfluxMapperBO> latestDataList = new ArrayList<>(latestDataMap.values());
        List<InfluxPointValueDO> influxPointValueDOList = convertMapperBoToValueDo(latestDataList);
        List<PointValueBO> pointValueBOList = influxPointValueBuilder.buildBOListByDOList(influxPointValueDOList);
        return pointValueBOList;
    }

    @Override
    public Page<PointValueBO> selectPagePointValue(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        QueryApi queryApi = influxDBClient.getQueryApi();
        StringBuilder flux = new StringBuilder();
        flux.append("from(bucket: \"dc3\")"
                + " |> range(start: -100h)" // 查询过去1小时的数据
                + " |> filter(fn: (r) => r._measurement == \"dc3\"");
        Page<PointValueBO> entityPageBO = new Page<>();
        if (Objects.nonNull(entityQuery.getDeviceId())) {
            flux.append("and r.deviceId ==\"" + entityQuery.getDeviceId() + "\"");
        }
        if (Objects.nonNull(entityQuery.getPointId())) {
            flux.append("and r.pointId ==\"" + entityQuery.getPointId() + "\"");
        }
        flux.append(")");
        //System.out.println(flux.toString());
        List<InfluxMapperDO> query = queryApi.query(flux.toString(), InfluxMapperDO.class);
        List<InfluxMapperBO> influxMapperBOList = convertToBO(query);
        List<InfluxPointValueDO> influxPointValueDOList = convertMapperBoToValueDo(influxMapperBOList);
        Pages pages = entityQuery.getPage();
        List<InfluxPointValueDO> page = getPage(influxPointValueDOList, (int) pages.getSize(), (int) pages.getCurrent());
        List<PointValueBO> pointValueBOList = influxPointValueBuilder.buildBOListByDOList(page);
        Page<PointValueBO> pointValueBOPage = entityPageBO.setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(influxPointValueDOList.size()).setRecords(pointValueBOList);
        return pointValueBOPage;
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.INFLUXDB, this);
    }
}
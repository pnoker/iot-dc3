package io.github.pnoker.center.data.service.impl.repository;

import cn.hutool.core.text.CharSequenceUtil;
import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.Bucket;
import com.influxdb.client.domain.WritePrecision;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.point.InfluxPoint;
import io.github.pnoker.common.entity.point.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.influxdb.enable", havingValue = "true")
public class InfluxServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String bucketName;

    @Value("${influx.org}")
    private String organization;

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.INFLUXDB;
    }

    @Override
    public void savePointValue(PointValue pointValue) throws IOException {
        if (!CharSequenceUtil.isAllNotEmpty(pointValue.getDeviceId(), pointValue.getPointId())) {
            return;
        }
        ensurePointValueBucket();
        WriteApiBlocking writeApiBlocking = influxDBClient.getWriteApiBlocking();
        writeApiBlocking.writeMeasurement(WritePrecision.MS, new InfluxPoint(pointValue));
    }

    private void ensurePointValueBucket() {
        BucketsApi bucketsApi = influxDBClient.getBucketsApi();
        Bucket bucket = bucketsApi.findBucketByName(bucketName);
        if (null == bucket) {
            bucketsApi.createBucket(bucketName, organization);
        }
    }

    @Override
    public void savePointValues(String deviceId, List<PointValue> pointValues) throws IOException {
        if (CharSequenceUtil.isEmpty(deviceId)) {
            return;
        }
        ensurePointValueBucket();
        WriteApi writeApi = influxDBClient.makeWriteApi();
        writeApi.writeMeasurements(WritePrecision.MS, pointValues.stream().map(InfluxPoint::new).collect(Collectors.toList()));
        writeApi.flush();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.INFLUXDB, this);
    }
}

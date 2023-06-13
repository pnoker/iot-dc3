package io.github.pnoker.center.data.service.impl.repository;

import io.github.pnoker.center.data.mapper.TaosPointValueMapper;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.point.PointValue;
import io.github.pnoker.common.entity.point.TaosPointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.tdengine.enable", havingValue = "true")
public class TDengineServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    TaosPointValueMapper taosPointValueMapper;

    @EventListener
    public void initDatabase(ContextRefreshedEvent event) {
        taosPointValueMapper.createSuperTable();
    }

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.TDENGINE;
    }

    @Override
    public void savePointValue(PointValue pointValue) throws IOException {
        taosPointValueMapper.createDeviceTable(pointValue.getDeviceId(), pointValue.getPointId());
        taosPointValueMapper.insertOne(new TaosPointValue(pointValue));
    }

    @Override
    public void savePointValues(String deviceId, List<PointValue> pointValues) throws IOException {
        taosPointValueMapper.createDeviceTable(deviceId, pointValues.get(0).getPointId());
        taosPointValueMapper.batchInsert(pointValues.stream().map(TaosPointValue::new).collect(Collectors.toList()));

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.TDENGINE, this);
    }
}

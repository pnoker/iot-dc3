package com.pnoker.center.data.service.impl;

import com.pnoker.center.data.service.PointValueService;
import com.pnoker.common.bean.driver.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class PointValueServiceImpl implements PointValueService {
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void addPointValue(PointValue pointValue) {
        long createTime = System.currentTimeMillis();
        long interval = createTime - pointValue.getOriginTime();
        mongoTemplate.save(pointValue.setCreateTime(createTime).setInterval(interval));
    }
}

package com.pnoker.center.data.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.center.data.service.PointValueService;
import com.pnoker.center.data.service.pool.ThreadPool;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.bean.driver.PointValueDto;
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
    private ThreadPool threadPool;
    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void add(PointValue pointValue) {
        threadPool.executor.execute(() -> {
            long createTime = System.currentTimeMillis();
            long interval = createTime - pointValue.getOriginTime();
            mongoTemplate.insert(pointValue.setCreateTime(createTime).setInterval(interval));
        });
    }

    @Override
    public Page<PointValue> list(PointValueDto pointValueDto) {
        Criteria criteria = new Criteria();
        Optional.ofNullable(pointValueDto).ifPresent(dto -> {
            if (null != dto.getDeviceId()) {
                criteria.and("deviceId").is(dto.getDeviceId());
            }
            if (null != dto.getPointId()) {
                criteria.and("pointId").is(dto.getPointId());
            }
            if (dto.getPage().getStartTime() > 0 && dto.getPage().getEndTime() > 0 && dto.getPage().getStartTime() <= dto.getPage().getEndTime()) {
                criteria.and("originTime").gte(dto.getPage().getStartTime()).lte(dto.getPage().getEndTime());
            }
        });
        Page<PointValue> page = query(criteria, pointValueDto.getPage());
        return page;
    }

    /**
     * 分页&排序&查询
     *
     * @param criteriaDefinition
     * @param pages
     * @return
     */
    private Page<PointValue> query(CriteriaDefinition criteriaDefinition, Pages pages) {
        Query query = queryBySort(criteriaDefinition);
        long count = mongoTemplate.count(query, PointValue.class);
        List<PointValue> pointValues = mongoTemplate.find(queryByPage(query, pages), PointValue.class);
        Page<PointValue> page = (new Page<PointValue>()).setCurrent(pages.getCurrent()).setSize(pages.getSize()).setTotal(count);
        page.setRecords(pointValues);
        return page;
    }

    /**
     * 排序
     *
     * @param criteriaDefinition
     * @return
     */
    private Query queryBySort(CriteriaDefinition criteriaDefinition) {
        Query query = new Query(criteriaDefinition);
        query.with(Sort.by(Sort.Direction.DESC, "originTime"));
        return query;
    }

    /**
     * 分页
     *
     * @param query
     * @param pages
     * @return
     */
    private Query queryByPage(Query query, Pages pages) {
        int size = (int) pages.getSize();
        long page = pages.getCurrent();
        query.limit(size).skip(size * (page - 1));
        return query;
    }
}

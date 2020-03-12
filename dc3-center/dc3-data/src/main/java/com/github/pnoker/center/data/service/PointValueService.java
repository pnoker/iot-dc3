package com.github.pnoker.center.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pnoker.common.bean.driver.PointValue;
import com.github.pnoker.common.bean.driver.PointValueDto;

/**
 * @author pnoker
 */
public interface PointValueService {
    /**
     * 新增 PointValue
     *
     * @param pointValue
     */
    void add(PointValue pointValue);

    /**
     * 获取带分页、排序
     *
     * @param pointValueDto
     * @return
     */
    Page<PointValue> list(PointValueDto pointValueDto);

    /**
     * 获取最新的一个位号数据
     *
     * @param pointValueDto
     * @return
     */
    PointValue latest(PointValueDto pointValueDto);
}

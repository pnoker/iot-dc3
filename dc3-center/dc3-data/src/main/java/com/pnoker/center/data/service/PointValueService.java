package com.pnoker.center.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.driver.PointValue;
import com.pnoker.common.bean.driver.PointValueDto;

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
}

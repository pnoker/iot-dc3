package com.dc3.center.data.service;

import com.dc3.common.model.PointValue;

import java.util.List;

/**
 * 用户自定义数据处理服务接口
 *
 * @author pnoker
 */
public interface DataCustomService {

    void preHandle(PointValue pointValue);

    void postHandle(PointValue pointValue);

    void postHandle(List<PointValue> pointValues);

    void afterHandle(PointValue pointValue);
}

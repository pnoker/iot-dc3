package com.dc3.center.data.service.impl;

import com.dc3.center.data.service.DataCustomService;
import com.dc3.common.model.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DataCustomServiceImpl implements DataCustomService {

    @Override
    public void preHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据之前的操作
    }

    @Override
    public void postHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据的时候的操作，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
    }

    @Override
    public void postHandle(List<PointValue> pointValues) {
        // TODO 接收数据之后，存储数据的时候的操作，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
    }

    @Override
    public void afterHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据之后的操作
    }
}

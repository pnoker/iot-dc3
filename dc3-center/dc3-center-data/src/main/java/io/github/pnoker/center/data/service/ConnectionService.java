package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;

public interface ConnectionService {
    RabbitMQDataVo queryConn(String cluster);
    RabbitMQDataVo queryToConn(String cluster);
    RabbitMQDataVo queryConnOpen(String cluster);
    RabbitMQDataVo queryConnClose(String cluster);
}

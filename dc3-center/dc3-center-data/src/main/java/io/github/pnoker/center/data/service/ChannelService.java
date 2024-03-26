package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;

public interface ChannelService {
    RabbitMQDataVo queryChan(String cluster);
    RabbitMQDataVo queryToChan(String cluster);
    RabbitMQDataVo queryChanOpen(String cluster);
    RabbitMQDataVo queryChanClose(String cluster);
}

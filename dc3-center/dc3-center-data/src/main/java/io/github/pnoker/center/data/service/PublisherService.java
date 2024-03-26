package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;

public interface PublisherService {
    RabbitMQDataVo queryPub(String cluster);
}

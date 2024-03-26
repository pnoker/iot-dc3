package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;

public interface QueueService {
    RabbitMQDataVo queryQue(String cluster);
    RabbitMQDataVo readyToCons(String cluster);
    RabbitMQDataVo pendToCons(String cluster);
    RabbitMQDataVo queryToQue(String cluster);
    RabbitMQDataVo queryQueDec(String cluster);
    RabbitMQDataVo queryQueCre(String cluster);
    RabbitMQDataVo queryQueDel(String cluster);
 }

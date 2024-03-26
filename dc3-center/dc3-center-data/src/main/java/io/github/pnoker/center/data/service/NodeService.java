package io.github.pnoker.center.data.service;

import io.github.pnoker.center.data.entity.vo.RabbitMQDataVo;
import io.github.pnoker.center.data.entity.vo.RabbitMQNodeVo;

import java.util.List;

public interface NodeService {
    RabbitMQDataVo queryNode(String cluster);
    List<RabbitMQNodeVo> queryNodeTable(String cluster);
}

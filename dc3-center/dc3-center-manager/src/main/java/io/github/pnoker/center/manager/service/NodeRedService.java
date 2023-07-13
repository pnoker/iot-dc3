package io.github.pnoker.center.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.NodeRedFlowsPageQuery;
import io.github.pnoker.common.model.NodeRedFlows;

public interface NodeRedService {
    Page<NodeRedFlows> flowsList(NodeRedFlowsPageQuery nodeRedFlowsPageQuery);
}
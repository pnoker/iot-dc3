package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.NodeRedFlowsPageQuery;
import io.github.pnoker.center.manager.mapper.NodeRedFlowsMapper;
import io.github.pnoker.center.manager.service.NodeRedService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.model.NodeRedFlows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class NodeRedServiceImpl implements NodeRedService {
    private static final Logger log = LoggerFactory.getLogger(NodeRedServiceImpl.class);
    @Resource
    NodeRedFlowsMapper nodeRedFlowsMapper;

    public NodeRedServiceImpl() {
    }

    public Page<NodeRedFlows> flowsList(NodeRedFlowsPageQuery nodeRedFlowsPageQuery) {
        if (ObjectUtil.isNotNull(nodeRedFlowsPageQuery.getPage())) {
            nodeRedFlowsPageQuery.setPage(new Pages());
        }

        return nodeRedFlowsMapper.selectPage(nodeRedFlowsPageQuery.getPage().convert(), fuzzyFlowsQuery(nodeRedFlowsPageQuery));
    }

    private LambdaQueryWrapper<NodeRedFlows> fuzzyFlowsQuery(NodeRedFlowsPageQuery query) {
        LambdaQueryWrapper<NodeRedFlows> queryWrapper = Wrappers.<NodeRedFlows>query().lambda();
        queryWrapper.eq(NodeRedFlows::getFlowType, "tab");
        return queryWrapper;
    }
}
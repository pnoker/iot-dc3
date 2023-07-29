/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
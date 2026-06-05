/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.entity.query.TopicQuery;
import io.github.pnoker.common.manager.entity.vo.TopicVO;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.TopicService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Business service implementation for topic operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */

@Service
public class TopicServiceImpl extends ServiceImpl<DeviceMapper, DeviceDO> implements TopicService {

    @Override
    public Page<TopicVO> query(TopicQuery topicQuery) {
        if (Objects.isNull(topicQuery.getPage())) {
            topicQuery.setPage(new Pages());
        }
        int page = (int) topicQuery.getPage().getCurrent();
        int size = (int) topicQuery.getPage().getSize();
        Page<TopicVO> resultPage = new Page<>(page, size);
        List<TopicVO> topicVOList = new ArrayList<>();
        String topic = topicQuery.getTopic();
        Long deviceIdL = null;
        if (Objects.nonNull(topic) && topic.length() > 0) {
            String[] parts = topic.split(SymbolConstant.SLASH);
            deviceIdL = Long.parseLong(parts[parts.length - 1]);
        }
        String dName = topicQuery.getDeviceName();
        List<DeviceDO> deviceList = lambdaQuery().eq(Objects.nonNull(deviceIdL), DeviceDO::getId, deviceIdL)
                .eq(Objects.nonNull(topicQuery.getTenantId()), DeviceDO::getTenantId, topicQuery.getTenantId())
                .eq(Objects.nonNull(dName) && !dName.isEmpty(), DeviceDO::getDeviceName, dName)
                // .eq(DeviceDO::getEnableFlag, 1)
                .list();

        for (DeviceDO device : deviceList) {
            String deviceName = device.getDeviceName();
            Long deviceId = device.getId();
            Long profileId = device.getProfileId();
            if (Objects.isNull(profileId)) {
                continue;
            }
            List<PointDO> points = Db.lambdaQuery(PointDO.class)
                    .eq(PointDO::getProfileId, profileId)
                    .eq(Objects.nonNull(topicQuery.getTenantId()), PointDO::getTenantId, topicQuery.getTenantId())
                    // .eq(PointDO::getEnableFlag, 1)
                    .eq(PointDO::getDeleted, 0)
                    .list();
            for (PointDO point : points) {
                TopicVO topicVO = new TopicVO();
                topicVO.setTopic(String.join(SymbolConstant.SLASH, "dc3", DataConstant.SERVICE_NAME, "device",
                        String.valueOf(deviceId)));
                topicVO.setDeviceName(deviceName);
                topicVO.setPointName(point.getPointName());
                topicVOList.add(topicVO);
            }
        }
        int totalItems = topicVOList.size();
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<TopicVO> paginatedData;
        if (fromIndex < toIndex) {
            paginatedData = topicVOList.subList(fromIndex, toIndex);
        } else {
            paginatedData = new ArrayList<>();
        }
        resultPage.setRecords(paginatedData);
        resultPage.setTotal(totalItems);
        return resultPage;
    }

}

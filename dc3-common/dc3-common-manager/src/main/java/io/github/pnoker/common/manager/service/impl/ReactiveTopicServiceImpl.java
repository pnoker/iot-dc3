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

import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.TopicOffsetQuery;
import io.github.pnoker.common.manager.entity.vo.TopicVO;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.manager.service.ReactivePointService;
import io.github.pnoker.common.manager.service.ReactiveTopicService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Default topic service implementation. */
@Service
@RequiredArgsConstructor
public class ReactiveTopicServiceImpl implements ReactiveTopicService {
    private static final int SOURCE_PAGE_SIZE = 200;
    private final ReactiveDeviceService deviceService;
    private final ReactivePointService pointService;

    @Override
    public Mono<OffsetPage<TopicVO>> list(TopicOffsetQuery query) {
        return devicePage(query, 0)
                .expand(page -> page.hasNext() ? devicePage(query, page.offset() + page.limit()) : Mono.empty())
                .flatMapIterable(page -> page.items())
                .flatMap(
                        device -> pointService
                                .listByDeviceId(query.tenantId(), device.getId())
                                .map(point -> toTopic(device.getId(), device.getDeviceName(), point.getPointName())),
                        8)
                .filter(topic -> query.topic() == null
                        || query.topic().isBlank()
                        || topic.getTopic().contains(query.topic()))
                .collectList()
                .map(all -> {
                    long from = Math.min(query.offset(), all.size());
                    long to = Math.min(from + query.limit(), all.size());
                    List<TopicVO> items = all.subList((int) from, (int) to);
                    return OffsetPage.of(items, query.offset(), query.limit(), all.size());
                });
    }

    private Mono<OffsetPage<DeviceBO>> devicePage(TopicOffsetQuery query, long offset) {
        DeviceFilter filter = new DeviceFilter(
                query.tenantId(), query.deviceName(), null, null, null, null, offset, SOURCE_PAGE_SIZE, List.of());
        return deviceService.list(filter);
    }

    private TopicVO toTopic(Long deviceId, String deviceName, String pointName) {
        TopicVO value = new TopicVO();
        value.setTopic(String.join(
                SymbolConstant.SLASH, "dc3", DataConstant.SERVICE_NAME, "device", String.valueOf(deviceId)));
        value.setDeviceName(deviceName);
        value.setPointName(pointName);
        return value;
    }
}

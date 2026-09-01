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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
                .flatMap(device -> pointService.listByDeviceId(query.tenantId(), device.getId())
                        .map(point -> toTopic(device.getId(), device.getDeviceName(), point.getPointName())), 8)
                .filter(topic -> query.topic() == null || query.topic().isBlank() || topic.getTopic().contains(query.topic()))
                .collectList()
                .map(all -> {
                    long from = Math.min(query.offset(), all.size());
                    long to = Math.min(from + query.limit(), all.size());
                    List<TopicVO> items = all.subList((int) from, (int) to);
                    return OffsetPage.of(items, query.offset(), query.limit(), all.size());
                });
    }

    private Mono<OffsetPage<DeviceBO>> devicePage(TopicOffsetQuery query, long offset) {
        DeviceFilter filter = new DeviceFilter(query.tenantId(), query.deviceName(), null, null, null, null,
                offset, SOURCE_PAGE_SIZE, List.of());
        return deviceService.list(filter);
    }

    private TopicVO toTopic(Long deviceId, String deviceName, String pointName) {
        TopicVO value = new TopicVO();
        value.setTopic(String.join(SymbolConstant.SLASH, "dc3", DataConstant.SERVICE_NAME, "device", String.valueOf(deviceId)));
        value.setDeviceName(deviceName);
        value.setPointName(pointName);
        return value;
    }
}

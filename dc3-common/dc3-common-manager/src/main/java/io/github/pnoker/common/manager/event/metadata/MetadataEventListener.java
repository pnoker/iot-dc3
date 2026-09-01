/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.manager.event.metadata;

import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.mq.message.MqMessage;
import io.github.pnoker.common.mq.sender.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Dispatches tenant-scoped metadata changes to affected driver queues. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataEventListener {

    private final ReactiveDriverService driverService;
    private final MessageSender messageSender;

    @EventListener
    public void onApplicationEvent(MetadataEvent metadataEvent) {
        if (metadataEvent == null || metadataEvent.getTenantId() == null) {
            log.warn("Dropping metadata event without tenant scope, id={}, type={}",
                    metadataEvent == null ? null : metadataEvent.getId(),
                    metadataEvent == null ? null : metadataEvent.getMetadataType());
            return;
        }
        dispatch(metadataEvent).subscribe(null,
                error -> log.error("Metadata event handling failed, id={}, type={}, operation={}",
                        metadataEvent.getId(), metadataEvent.getMetadataType(), metadataEvent.getOperateType(), error));
    }

    private Mono<Void> dispatch(MetadataEvent metadataEvent) {
        MetadataEventDTO payload = new MetadataEventDTO(metadataEvent.getTenantId(), metadataEvent.getId(),
                metadataEvent.getMetadataType(), metadataEvent.getOperateType());
        Flux<String> services;
        if (!metadataEvent.getTargetServices().isEmpty()) {
            services = Flux.fromIterable(metadataEvent.getTargetServices());
        } else {
            services = switch (metadataEvent.getMetadataType()) {
                case DEVICE -> driverService.getByDeviceId(metadataEvent.getTenantId(), metadataEvent.getId())
                        .flux().map(DriverBO::getServiceName);
                case POINT -> driverService.listByPointId(metadataEvent.getTenantId(), metadataEvent.getId())
                        .map(DriverBO::getServiceName);
                case DRIVER -> driverService.getById(metadataEvent.getTenantId(), metadataEvent.getId())
                        .flux().map(DriverBO::getServiceName);
                default -> Flux.empty();
            };
        }
        return services.filter(service -> service != null && !service.isBlank())
                .distinct()
                .flatMap(service -> Mono.fromRunnable(() -> notifyDriver(service, payload)), 16)
                .then();
    }

    private void notifyDriver(String service, MetadataEventDTO payload) {
        log.debug("Driver metadata notification published, tenantId={}, serviceName={}, id={}, type={}",
                payload.getTenantId(), service, payload.getId(), payload.getMetadataType());
        messageSender.send(MqMessage.of(MqTopic.METADATA, service, payload));
    }
}

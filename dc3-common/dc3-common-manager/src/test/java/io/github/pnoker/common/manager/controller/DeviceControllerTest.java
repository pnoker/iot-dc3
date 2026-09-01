/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.query.DeviceImportRequest;
import io.github.pnoker.common.manager.entity.query.DeviceImportTemplateRequest;
import io.github.pnoker.common.manager.entity.query.DeviceListRequest;
import io.github.pnoker.common.manager.entity.vo.DeviceVO;
import io.github.pnoker.common.manager.repository.DeviceFilter;
import io.github.pnoker.common.manager.service.ReactiveDeviceImportService;
import io.github.pnoker.common.manager.service.ReactiveDeviceService;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.operation.OperationAccepted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    private static final Long TENANT_ID = 100L;

    @Mock
    private DeviceBuilder deviceBuilder;

    @Mock
    private ReactiveDeviceImportService deviceImportService;

    @Mock
    private ReactiveDeviceService reactiveDeviceService;

    private DeviceController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceController(deviceBuilder, reactiveDeviceService, deviceImportService);
    }

    @Test
    void addReturnsCreatedResourceAndProjectsPrincipal() {
        DeviceVO request = new DeviceVO();
        DeviceBO input = new DeviceBO();
        DeviceBO saved = new DeviceBO();
        DeviceVO response = new DeviceVO();
        when(deviceBuilder.buildBOByVO(request)).thenReturn(input);
        when(reactiveDeviceService.add(input)).thenReturn(Mono.just(saved));
        when(deviceBuilder.buildVOByBO(saved)).thenReturn(response);

        StepVerifier.create(withPrincipal(controller.add(request)))
                .assertNext(entity -> {
                    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(entity.getBody()).isSameAs(response);
                })
                .verifyComplete();

        assertThat(input.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(input.getOperatorId()).isEqualTo(7L);
        assertThat(input.getOperatorName()).isEqualTo("alice");
    }

    @Test
    void listUsesCanonicalOffsetRequestAndReturnsCanonicalPage() {
        DeviceListRequest request = new DeviceListRequest(20, 10, List.of(), "sensor", null,
                2L, 3L, null, null, null, null);
        DeviceBO device = new DeviceBO();
        DeviceVO deviceVO = new DeviceVO();
        when(reactiveDeviceService.list(any(DeviceFilter.class)))
                .thenReturn(Mono.just(OffsetPage.of(List.of(device), 20, 10, 31)));
        when(deviceBuilder.buildVOByBO(device)).thenReturn(deviceVO);

        StepVerifier.create(withPrincipal(controller.list(request)))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(deviceVO);
                    assertThat(page.offset()).isEqualTo(20);
                    assertThat(page.limit()).isEqualTo(10);
                    assertThat(page.total()).isEqualTo(31);
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();

        ArgumentCaptor<DeviceFilter> captor = ArgumentCaptor.forClass(DeviceFilter.class);
        verify(reactiveDeviceService).list(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().deviceName()).isEqualTo("sensor");
    }

    @Test
    void importUsesMinimalContextAndProjectsPrincipal() {
        FilePart filePart = org.mockito.Mockito.mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(4);
        when(filePart.filename()).thenReturn("devices.xlsx");
        when(filePart.headers()).thenReturn(headers);
        when(filePart.content()).thenReturn(Flux.just(new DefaultDataBufferFactory().wrap(new byte[]{1, 2, 3, 4})));
        OperationAccepted accepted = new OperationAccepted(UUID.randomUUID(), "/operations/get_by_id?id=1");
        when(deviceImportService.submit(any(DeviceBO.class), org.mockito.ArgumentMatchers.eq("devices.xlsx"),
                org.mockito.ArgumentMatchers.any(byte[].class), org.mockito.ArgumentMatchers.eq("import-1")))
                .thenReturn(Mono.just(accepted));

        StepVerifier.create(withPrincipal(controller.importDevice(new DeviceImportRequest(11L, 12L),
                        Mono.just(filePart), "import-1")))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
                    assertThat(response.getHeaders().getLocation())
                            .hasToString("/operations/get_by_id?id=1");
                    assertThat(response.getBody()).isSameAs(accepted);
                })
                .verifyComplete();

        ArgumentCaptor<DeviceBO> context = ArgumentCaptor.forClass(DeviceBO.class);
        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(deviceImportService).submit(context.capture(), org.mockito.ArgumentMatchers.eq("devices.xlsx"),
                content.capture(), org.mockito.ArgumentMatchers.eq("import-1"));
        assertThat(context.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(context.getValue().getDriverId()).isEqualTo(11L);
        assertThat(context.getValue().getProfileId()).isEqualTo(12L);
        assertThat(context.getValue().getOperatorId()).isEqualTo(7L);
        assertThat(context.getValue().getOperatorName()).isEqualTo("alice");
        assertThat(content.getValue()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void templateUsesMinimalContext() {
        when(deviceImportService.generateTemplate(TENANT_ID, 21L, 22L)).thenReturn(Mono.just(new byte[]{7, 8}));

        StepVerifier.create(withPrincipal(controller.importTemplate(new DeviceImportTemplateRequest(21L, 22L))))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().getByteArray()).containsExactly(7, 8);
                })
                .verifyComplete();
    }

    private static <T> Mono<T> withPrincipal(Mono<T> publisher) {
        RequestHeader.PrincipalHeader principal = new RequestHeader.PrincipalHeader(7L, "USER", "Alice", "alice",
                TENANT_ID, null, null);
        return publisher.contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                new GatewayAuthenticationToken(principal, Set.of())));
    }
}

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

package io.github.pnoker.common.manager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.entity.vo.DriverVO;
import io.github.pnoker.common.manager.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reactive controller test that exercises DriverController through StepVerifier
 * with the user-header context wired manually. This is intentionally lighter than a
 * full {@code @WebFluxTest} — controller-only routing, request validation and
 * filter chain end up covered by the gateway slice and Spring slice tests in
 * later stages.
 */
@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    private static final long TENANT_ID = 100L;

    @Mock
    private DriverBuilder driverBuilder;

    @Mock
    private DriverService driverService;

    private DriverController controller;

    private static <T> Mono<T> withTenantContext(Mono<T> mono) {
        RequestHeader.UserHeader user = new RequestHeader.UserHeader(7L, "Alice", "alice", TENANT_ID);
        return mono.contextWrite(Context.of(RequestConstant.Key.USER_HEADER, user));
    }

    private static <T> Mono<T> withMissingHeader(Mono<T> mono) {
        return mono.contextWrite(Context.empty());
    }

    @BeforeEach
    void setUp() {
        controller = new DriverController(driverBuilder, driverService);
    }

    @Test
    void addProjectsTenantOntoBoBeforeDelegating() {
        DriverVO vo = new DriverVO();
        DriverBO bo = new DriverBO();
        when(driverBuilder.buildBOByVO(vo)).thenReturn(bo);

        StepVerifier.create(withTenantContext(controller.add(vo)))
                .assertNext(envelope -> assertThat(envelope.isOk()).isTrue())
                .verifyComplete();

        ArgumentCaptor<DriverBO> captor = ArgumentCaptor.forClass(DriverBO.class);
        verify(driverService).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void addReturnsErrorWhenTenantHeaderMissing() {
        StepVerifier.create(withMissingHeader(controller.add(new DriverVO())))
                .expectError()
                .verify();
        verify(driverService, never()).save(any(DriverBO.class));
    }

    @Test
    void deleteRequiresTenantOwnership() {
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        bo.setTenantId(TENANT_ID);
        when(driverService.selectById(1L)).thenReturn(bo);

        StepVerifier.create(withTenantContext(controller.delete(1L)))
                .assertNext(envelope -> assertThat(envelope.isOk()).isTrue())
                .verifyComplete();
        verify(driverService).remove(1L);
    }

    @Test
    void deleteRejectsCrossTenantResource() {
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        bo.setTenantId(999L);
        when(driverService.selectById(1L)).thenReturn(bo);

        StepVerifier.create(withTenantContext(controller.delete(1L)))
                .expectErrorMatches(throwable -> throwable instanceof NotFoundException)
                .verify();
        verify(driverService, never()).remove(any(Long.class));
    }

    @Test
    void updateProjectsTenantAndChecksOwnership() {
        DriverVO vo = new DriverVO();
        vo.setId(1L);
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        bo.setTenantId(TENANT_ID);
        when(driverBuilder.buildBOByVO(vo)).thenReturn(bo);
        when(driverService.selectById(1L)).thenReturn(bo);

        StepVerifier.create(withTenantContext(controller.update(vo)))
                .assertNext(envelope -> assertThat(envelope.isOk()).isTrue())
                .verifyComplete();
        verify(driverService).update(bo);
    }

    @Test
    void updateRejectsCrossTenantUpdate() {
        DriverVO vo = new DriverVO();
        vo.setId(1L);
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        DriverBO existing = new DriverBO();
        existing.setId(1L);
        existing.setTenantId(999L);
        when(driverBuilder.buildBOByVO(vo)).thenReturn(bo);
        when(driverService.selectById(1L)).thenReturn(existing);

        StepVerifier.create(withTenantContext(controller.update(vo)))
                .expectError(NotFoundException.class)
                .verify();
        verify(driverService, never()).update(any(DriverBO.class));
    }

    @Test
    void selectByIdReturnsBoForOwnedRow() {
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        bo.setTenantId(TENANT_ID);
        DriverVO vo = new DriverVO();
        when(driverService.selectById(1L)).thenReturn(bo);
        when(driverBuilder.buildVOByBO(bo)).thenReturn(vo);

        StepVerifier.create(withTenantContext(controller.selectById(1L)))
                .assertNext(envelope -> assertThat(envelope.getData()).isSameAs(vo))
                .verifyComplete();
    }

    @Test
    void selectByIdRejectsCrossTenantRow() {
        DriverBO bo = new DriverBO();
        bo.setId(1L);
        bo.setTenantId(999L);
        when(driverService.selectById(1L)).thenReturn(bo);

        StepVerifier.create(withTenantContext(controller.selectById(1L)))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void selectByIdsFiltersForOwnedDrivers() {
        DriverBO own = new DriverBO();
        own.setId(1L);
        own.setTenantId(TENANT_ID);
        DriverBO foreign = new DriverBO();
        foreign.setId(2L);
        foreign.setTenantId(999L);
        DriverVO vo = new DriverVO();
        vo.setId(1L);
        when(driverService.selectByIds(Set.of(1L, 2L))).thenReturn(List.of(own, foreign));
        when(driverBuilder.buildVOByBO(own)).thenReturn(vo);

        StepVerifier.create(withTenantContext(controller.selectByIds(Set.of(1L, 2L))))
                .assertNext(envelope -> {
                    assertThat(envelope.getData()).hasSize(1).containsKey(1L);
                    assertThat(envelope.getData().get(1L)).isSameAs(vo);
                })
                .verifyComplete();
    }

    @Test
    void listForcesTenantOnSearchQuery() {
        DriverQuery incoming = new DriverQuery();
        incoming.setTenantId(999L); // attempt to read other tenant; controller must override.
        Page<DriverBO> bos = new Page<>();
        Page<DriverVO> vos = new Page<>();
        when(driverService.selectByPage(any(DriverQuery.class))).thenReturn(bos);
        when(driverBuilder.buildVOPageByBOPage(bos)).thenReturn(vos);

        StepVerifier.create(withTenantContext(controller.list(incoming)))
                .assertNext(envelope -> assertThat(envelope.getData()).isSameAs(vos))
                .verifyComplete();

        ArgumentCaptor<DriverQuery> captor = ArgumentCaptor.forClass(DriverQuery.class);
        verify(driverService).selectByPage(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void listAcceptsNullEntityQuery() {
        Page<DriverBO> bos = new Page<>();
        Page<DriverVO> vos = new Page<>();
        when(driverService.selectByPage(any(DriverQuery.class))).thenReturn(bos);
        when(driverBuilder.buildVOPageByBOPage(bos)).thenReturn(vos);

        StepVerifier.<R<Page<DriverVO>>>create(withTenantContext(controller.list(null)))
                .assertNext(envelope -> assertThat(envelope.getData()).isSameAs(vos))
                .verifyComplete();
    }

    @Test
    void selectByServiceNameDelegatesWithTenantScope() {
        DriverBO bo = new DriverBO();
        DriverVO vo = new DriverVO();
        when(driverService.selectByServiceName("dc3-driver-modbus-tcp", TENANT_ID)).thenReturn(bo);
        when(driverBuilder.buildVOByBO(bo)).thenReturn(vo);

        StepVerifier.create(withTenantContext(controller.selectByServiceName("dc3-driver-modbus-tcp")))
                .assertNext(envelope -> assertThat(envelope.getData()).isSameAs(vo))
                .verifyComplete();
        verify(driverService).selectByServiceName(eq("dc3-driver-modbus-tcp"), eq(TENANT_ID));
    }
}

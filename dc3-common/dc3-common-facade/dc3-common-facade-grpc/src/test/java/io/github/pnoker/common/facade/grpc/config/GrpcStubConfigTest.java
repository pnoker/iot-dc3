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
package io.github.pnoker.common.facade.grpc.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.api.center.manager.DeviceApiGrpc;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.client.GrpcChannelFactory;

class GrpcStubConfigTest {

    @Test
    void createsAllDomainStubsByDefault() {
        contextRunner().run(context -> {
            assertThat(context).hasSingleBean(TenantApiGrpc.TenantApiStub.class);
            assertThat(context).hasSingleBean(DeviceApiGrpc.DeviceApiStub.class);
            assertThat(context).hasSingleBean(PointValueApiGrpc.PointValueApiStub.class);
        });
    }

    @Test
    void omitsManagerStubsWhenManagerDomainIsDisabled() {
        contextRunner().withPropertyValues("dc3.facade.manager.mode=disabled").run(context -> {
            assertThat(context).hasSingleBean(TenantApiGrpc.TenantApiStub.class);
            assertThat(context).doesNotHaveBean(DeviceApiGrpc.DeviceApiStub.class);
            assertThat(context).doesNotHaveBean(DeviceApiGrpc.DeviceApiBlockingStub.class);
            assertThat(context).hasSingleBean(PointValueApiGrpc.PointValueApiStub.class);
        });
    }

    private ApplicationContextRunner contextRunner() {
        GrpcChannelFactory channels = mock(GrpcChannelFactory.class);
        when(channels.createChannel(anyString())).thenReturn(mock(ManagedChannel.class));
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GrpcStubConfig.class))
                .withBean(GrpcChannelFactory.class, () -> channels);
    }
}

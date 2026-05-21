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

package io.github.pnoker.test.support;

import lombok.RequiredArgsConstructor;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.UUID;

/**
 * JUnit 5 extension that boots an in-process gRPC server bound to the supplied
 * {@link BindableService} implementations and exposes a managed channel to the
 * test, then shuts both down after each test.
 */
@RequiredArgsConstructor
public class GrpcInProcessExtension implements BeforeEachCallback, AfterEachCallback {

    private final BindableService[] services;
    private final String name = "dc3-grpc-" + UUID.randomUUID();

    private Server server;
    private ManagedChannel channel;

    public ManagedChannel channel() {
        return channel;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder.forName(name).directExecutor();
        for (BindableService service : services) {
            builder.addService(service);
        }
        server = builder.build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }
}

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
package io.github.pnoker.common.config;

import ch.qos.logback.classic.spi.LogbackServiceProvider;
import io.github.pnoker.common.security.PermissionMethods;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * GraalVM native-image hints shared by every DC3 service. Bean wiring is
 * covered by Spring Boot AOT processing and third-party runtime reflection by
 * the GraalVM reachability metadata repository; what neither covers are the
 * classpath resources that live inside DC3's own library jars and the
 * ServiceLoader descriptors third-party stacks discover at runtime — native
 * images only embed resources matched by an explicit pattern.
 *
 * @author pnoker
 */
public class Dc3NativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // Shared configuration shipped in dc3-common-web: web profile defaults
        // (application-web.yml) and the favicon served with every service.
        hints.resources().registerPattern("application-web.yml");
        hints.resources().registerPattern("static/favicon.ico");

        // Logging configuration shipped in dc3-common-log; without this pattern
        // native services silently fall back to the logback default config.
        hints.resources().registerPattern("logback.xml");

        // SLF4J -> logback binding: logback-classic registers its provider via
        // a META-INF/services descriptor and ships no native-image config of
        // its own. Dropped from the image, the binding degrades to NOP and
        // org.slf4j.MarkerFactory's static initializer kills the whole
        // commons-logging -> log4j-api -> log4j-to-slf4j bridge at startup.
        hints.resources().registerPattern("META-INF/services/org.slf4j.spi.SLF4JServiceProvider");
        hints.reflection().registerType(LogbackServiceProvider.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

        // R2DBC connection factories resolve their driver through the SPI's
        // ServiceLoader descriptor — same gap as SLF4J above. The PostgreSQL
        // provider lives in dc3-db modules, outside this module's classpath,
        // hence the TypeReference instead of a class literal.
        hints.resources().registerPattern("META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider");
        hints.reflection().registerType(TypeReference.of("io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider"),
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

        // gRPC resolves name resolvers (dns:// etc.) through ServiceLoader.
        hints.resources().registerPattern("META-INF/services/io.grpc.NameResolverProvider");

        // grpc-java's ReflectionLongAdderCounter instantiates LongAdder
        // reflectively when a server/channel tracer boots; without the
        // constructor registration gRPC server startup dies with
        // MissingReflectionRegistrationError in a native image.
        hints.reflection().registerType(java.util.concurrent.atomic.LongAdder.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

        // springdoc / swagger-ui assets served from inside the starter jars.
        hints.resources().registerPattern("META-INF/resources/webjars/swagger-ui/**");

        // gRPC-netty's Utils.getEpollChannelOption reflectively reads static
        // final ChannelOption fields (e.g. TCP_USER_TIMEOUT) from the epoll
        // option class via Class.forName + getField. Every center and the
        // gateway runs a gRPC channel, so this hint is shared.
        hints.reflection().registerType(
                TypeReference.of("io.netty.channel.epoll.EpollChannelOption"),
                MemberCategory.PUBLIC_FIELDS,
                MemberCategory.DECLARED_FIELDS);
        hints.reflection().registerType(
                TypeReference.of("jdk.net.ExtendedSocketOptions"),
                MemberCategory.PUBLIC_FIELDS,
                MemberCategory.DECLARED_FIELDS);

        // GatewayJwtConverter deserializes the gateway-injected principal
        // header with Jackson; without constructor + field access every
        // authenticated request dies with malformedPrincipalHeader.
        hints.reflection().registerType(
                TypeReference.of("io.github.pnoker.common.entity.common.RequestHeader$PrincipalHeader"),
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.PUBLIC_FIELDS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS);

        // Every @PreAuthorize("@perm.can(...)") guard resolves through
        // Spring Security's SpEL engine, which invokes the bean methods
        // reflectively. The expression's bean reference is not covered by
        // Spring AOT security processing, so register the whole bean.
        hints.reflection().registerType(
                PermissionMethods.class,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS);
    }
}

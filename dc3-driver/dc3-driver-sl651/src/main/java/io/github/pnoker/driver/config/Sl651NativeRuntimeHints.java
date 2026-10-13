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
package io.github.pnoker.driver.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * GraalVM native-image hints for the SL651 driver's optional-dependency
 * bridge. Sl651DriverCustomServiceImpl reaches the SL651 stack through
 * Class.forName + a JDK proxy (the dependency is optional at runtime), and
 * without these registrations a native image silently takes the
 * sl651ApiMissing branch - the driver boots, reports health, and never
 * receives a single message. Both classes are string-based references
 * because the sl651 stack only exists on this module's classpath.
 *
 * <p>The shared SDK surface (logback, gRPC, Quartz, Caffeine, receivers)
 * is registered by DriverNativeRuntimeHints in dc3-common-driver.
 *
 * @author pnoker
 */
public class Sl651NativeRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // SL651Server: constructed reflectively with the server password,
        // then driven through setMessageListener(ISl651MessageListener) and
        // start(int) - getMethod() only resolves public members.
        hints.reflection()
                .registerType(
                        TypeReference.of("com.github.xingshuangs.iot.protocol.sl651.service.SL651Server"),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);

        // ISl651MessageListener: the JDK proxy implements this interface, so
        // its methods must be reflectively accessible for proxy dispatch.
        hints.reflection()
                .registerType(
                        TypeReference.of("com.github.xingshuangs.iot.protocol.sl651.event.ISl651MessageListener"),
                        MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}

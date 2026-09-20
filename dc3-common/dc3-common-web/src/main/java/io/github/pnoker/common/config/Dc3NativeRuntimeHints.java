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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native-image hints shared by every DC3 service. Bean wiring is
 * covered by Spring Boot AOT processing and third-party runtime reflection by
 * the GraalVM reachability metadata repository; what neither covers are the
 * classpath resources that live inside DC3's own library jars — native images
 * only embed resources matched by an explicit pattern.
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

        // springdoc / swagger-ui assets served from inside the starter jars.
        hints.resources().registerPattern("META-INF/resources/webjars/swagger-ui/**");
    }
}

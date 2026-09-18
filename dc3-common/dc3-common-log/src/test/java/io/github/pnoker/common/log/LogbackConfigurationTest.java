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
package io.github.pnoker.common.log;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.Status;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogbackConfigurationTest {

    @Test
    void sharedConfigurationCarriesDistributedContextAndUsesLosslessAsyncIo() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream("logback.xml")) {
            assertThat(stream).isNotNull();
            String configuration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(configuration)
                    .contains("${APPLICATION_NAME:-unknown}")
                    .contains("<mdc/>")
                    .contains("<keyValuePairs/>")
                    .contains("<stackTrace/>")
                    .contains("name=\"ASYNC_CONSOLE\"")
                    .contains("name=\"ASYNC_FILE\"")
                    .contains("<discardingThreshold>0</discardingThreshold>")
                    .contains("<neverBlock>false</neverBlock>")
                    .contains("<includeCallerData>true</includeCallerData>")
                    .contains("<appender-ref ref=\"ASYNC_CONSOLE\"/>")
                    .contains("<appender-ref ref=\"ASYNC_FILE\"/>");
        }

        URL configurationUrl = classLoader.getResource("logback.xml");
        assertThat(configurationUrl).isNotNull();
        LoggerContext context = new LoggerContext();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(configurationUrl);
            List<String> errors = context.getStatusManager().getCopyOfStatusList().stream()
                    .filter(status -> status.getLevel() == Status.ERROR)
                    .map(Status::getMessage)
                    .toList();
            assertThat(errors).as("Logback configuration errors").isEmpty();
        } finally {
            context.stop();
        }
    }
}

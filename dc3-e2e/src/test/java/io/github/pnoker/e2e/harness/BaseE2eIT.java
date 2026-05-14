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

package io.github.pnoker.e2e.harness;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

/**
 * Base class for end-to-end integration tests. Subclasses inherit the lifecycle that
 * boots the {@link E2eStack} once per JVM and inherits the {@code @Tag("e2e")} marker
 * so non-E2E builds (which do not carry Docker) can exclude these tests via
 * {@code -Dgroups='!e2e'} or surefire excludedGroups configuration.
 *
 * <p>Each concrete subclass MUST add
 * {@code @EnabledIfEnvironmentVariable(named = "DC3_E2E", matches = "(?i)true|1|yes|on")}
 * directly on the class. JUnit 5 evaluates the condition on the declared class, so
 * keeping it on the abstract base would silently leave subclasses enabled when
 * {@code DC3_E2E} is unset. The {@code e2e.yml} workflow exports the env var before
 * running this suite.
 */
@Tag("e2e")
public abstract class BaseE2eIT {

    @BeforeAll
    static void bootStack() {
        E2eStack.start();
    }
}

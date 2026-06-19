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

package io.github.pnoker.common.manager.scan;

import io.github.pnoker.common.resource.registrar.scan.ControllerAnnotationGate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SP2 ratchet gate for the manager service: every controller endpoint must carry a complete,
 * legal {@code x-dc3-ai} annotation and described request parameters. Once this test is green it
 * stays green — a newly added or edited manager endpoint that drops the annotation fails the build.
 */
class ManagerAnnotationGateTest {

    @Test
    void allManagerControllersAreFullyAnnotated() {
        List<String> defects = new ControllerAnnotationGate()
                .validatePackage("io.github.pnoker.common.manager.controller");
        assertThat(defects)
                .as("x-dc3-ai annotation defects in manager controllers: %s", defects)
                .isEmpty();
    }
}

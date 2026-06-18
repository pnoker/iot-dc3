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

package io.github.pnoker.common.resource.registrar.scan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerAnnotationGateTest {

    private final ControllerAnnotationGate gate = new ControllerAnnotationGate();

    @Test
    void reportsDefectsForDefectiveControllerOnly() {
        List<String> defects = gate.validatePackage(
                "io.github.pnoker.common.resource.registrar.scan.gatefixtures");
        // The well-formed controller contributes nothing; the defective one contributes ≥1.
        assertThat(defects).isNotEmpty();
        assertThat(defects).anyMatch(d -> d.startsWith("DefectiveController#get"));
        assertThat(defects).noneMatch(d -> d.startsWith("WellFormedController#get"));
    }
}

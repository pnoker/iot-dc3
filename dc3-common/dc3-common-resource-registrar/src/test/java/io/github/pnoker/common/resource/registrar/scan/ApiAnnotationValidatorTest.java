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

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAnnotationValidatorTest {

    private final ApiAnnotationValidator validator = new ApiAnnotationValidator();

    @Test
    void flagsMissingExtensionAndShortDescription() {
        List<String> defects = validator.validate("dc3-center-manager:POST:/x",
                ValidatorFixtures.opNoExtensionShortDescription());
        assertThat(defects).anyMatch(d -> d.contains("x-dc3-ai"));
        assertThat(defects).anyMatch(d -> d.contains("description"));
    }

    @Test
    void flagsIllegalRiskAndNonBooleanFlag() {
        List<String> defects = validator.validate("dc3-center-manager:POST:/y",
                ValidatorFixtures.opIllegalRiskAndFlag());
        assertThat(defects).anyMatch(d -> d.contains("riskLevel"));
        assertThat(defects).anyMatch(d -> d.contains("destructive"));
    }

    @Test
    void passesAWellFormedOperation() {
        assertThat(validator.validate("dc3-center-manager:POST:/z", ValidatorFixtures.opValid())).isEmpty();
    }

    @Test
    void flagsUndescribedBodyFieldAndRequestParam() {
        Method method = ValidatorFixtures.methodWithUndescribedParams();
        List<String> defects = validator.validate("dc3-center-manager:POST:/x", method);
        assertThat(defects).anyMatch(d -> d.contains("body field") && d.contains("deviceName"));
        assertThat(defects).anyMatch(d -> d.contains("param") && d.contains("id"));
    }

    @Test
    void passesWhenBodyFieldsAndParamsAreDescribed() {
        Method method = ValidatorFixtures.methodWithFullyDescribedParams();
        assertThat(validator.validate("dc3-center-manager:POST:/y", method)).isEmpty();
    }
}

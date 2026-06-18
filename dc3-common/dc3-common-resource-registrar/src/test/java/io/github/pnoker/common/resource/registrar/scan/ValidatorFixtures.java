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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

/**
 * Test helper that returns {@link Operation} instances via reflection over annotated dummy methods,
 * mirroring the pattern used in {@link ApiEndpointScannerTest}.
 */
class ValidatorFixtures {

    /**
     * Returns an @Operation with NO x-dc3-ai extension and a description shorter than 20 chars.
     * The validator should report BOTH a description defect AND a missing x-dc3-ai defect.
     */
    static Operation opNoExtensionShortDescription() {
        return getOperation("noExtensionShortDescription");
    }

    /**
     * Returns an @Operation WITH x-dc3-ai extension where riskLevel is an illegal value ("foo")
     * and the destructive flag is a non-boolean value ("maybe").
     * The description is valid (>=20 chars) so only riskLevel + destructive defects are expected.
     */
    static Operation opIllegalRiskAndFlag() {
        return getOperation("illegalRiskAndFlag");
    }

    /**
     * Returns a well-formed @Operation with a description >=20 chars and a valid x-dc3-ai extension.
     * The validator should return an empty defects list.
     */
    static Operation opValid() {
        return getOperation("valid");
    }

    // --- annotated dummy methods ---

    @Operation(
            description = "short"
            // no extensions → missing x-dc3-ai
    )
    private void noExtensionShortDescription() {
    }

    @Operation(
            description = "This is a valid description that exceeds twenty characters",
            extensions = @Extension(
                    name = "x-dc3-ai",
                    properties = {
                            @ExtensionProperty(name = "riskLevel", value = "foo"),
                            @ExtensionProperty(name = "destructive", value = "maybe"),
                            @ExtensionProperty(name = "idempotent", value = "true")
                    }
            )
    )
    private void illegalRiskAndFlag() {
    }

    @Operation(
            description = "This is a valid description that exceeds twenty characters",
            extensions = @Extension(
                    name = "x-dc3-ai",
                    properties = {
                            @ExtensionProperty(name = "riskLevel", value = "LOW"),
                            @ExtensionProperty(name = "destructive", value = "false"),
                            @ExtensionProperty(name = "idempotent", value = "true"),
                            @ExtensionProperty(name = "openWorld", value = "false"),
                            @ExtensionProperty(name = "hidden", value = "false")
                    }
            )
    )
    private void valid() {
    }

    // --- reflection helper ---

    private static Operation getOperation(String methodName) {
        try {
            return ValidatorFixtures.class
                    .getDeclaredMethod(methodName)
                    .getAnnotation(Operation.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Fixture method not found: " + methodName, e);
        }
    }
}

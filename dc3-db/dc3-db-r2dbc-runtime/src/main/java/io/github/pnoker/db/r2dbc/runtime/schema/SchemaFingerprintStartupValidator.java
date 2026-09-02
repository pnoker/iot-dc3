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
package io.github.pnoker.db.r2dbc.runtime.schema;

import io.github.pnoker.db.r2dbc.runtime.config.R2dbcRuntimeProperties;
import java.time.Duration;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Blocks application readiness until the clean-schema fingerprint is verified.
 * This runs once on the startup thread, never on a request/event-loop thread.
 */
public final class SchemaFingerprintStartupValidator implements SmartInitializingSingleton {

    private final SchemaFingerprintVerifier verifier;
    private final Duration timeout;

    public SchemaFingerprintStartupValidator(SchemaFingerprintVerifier verifier, R2dbcRuntimeProperties properties) {
        this.verifier = verifier;
        Duration configured = properties.getStartupTimeout();
        if (configured == null || configured.isZero() || configured.isNegative()) {
            throw new IllegalArgumentException("dc3.r2dbc.startup-timeout must be positive");
        }
        this.timeout = configured;
    }

    @Override
    public void afterSingletonsInstantiated() {
        verifier.verify().block(timeout);
    }
}

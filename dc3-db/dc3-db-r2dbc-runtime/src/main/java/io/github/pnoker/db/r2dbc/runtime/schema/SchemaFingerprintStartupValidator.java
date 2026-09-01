package io.github.pnoker.db.r2dbc.runtime.schema;

import io.github.pnoker.db.r2dbc.runtime.config.R2dbcRuntimeProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.time.Duration;

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

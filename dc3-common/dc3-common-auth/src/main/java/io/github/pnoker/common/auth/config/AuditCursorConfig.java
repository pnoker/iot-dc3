package io.github.pnoker.common.auth.config;

import io.github.pnoker.common.config.HmacAuthProperties;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import io.github.pnoker.common.auth.support.IdentityAuditCursorCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Clock;

/** Configures the signed identity-audit cursor codec from the shared HMAC secret. */
@Configuration
public class AuditCursorConfig {

    @Bean
    IdentityAuditCursorCodec identityAuditCursorCodec(HmacAuthProperties properties, Environment environment) {
        String secret = StringUtils.defaultIfBlank(properties.getSecret(),
                environment.getProperty(EnvironmentConstant.AUTH_HMAC_SECRET_ENV, ""));
        return new IdentityAuditCursorCodec(secret, Clock.systemUTC());
    }
}

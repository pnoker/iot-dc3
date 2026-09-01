package io.github.pnoker.common.data.support;

import io.github.pnoker.common.config.HmacAuthProperties;
import io.github.pnoker.common.constant.common.EnvironmentConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.time.Clock;

/** Configures the signing key used by point-value cursors. */
@AutoConfiguration
@EnableConfigurationProperties(HmacAuthProperties.class)
public class PointValueCursorConfig {

    @Bean
    @ConditionalOnMissingBean
    public PointValueCursorCodec pointValueCursorCodec(HmacAuthProperties properties, Environment environment) {
        String secret = StringUtils.defaultIfBlank(properties.getSecret(),
                environment.getProperty(EnvironmentConstant.AUTH_HMAC_SECRET_ENV, ""));
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException("Point-value cursor signing secret must be configured");
        }
        return new PointValueCursorCodec(secret, Clock.systemUTC());
    }
}

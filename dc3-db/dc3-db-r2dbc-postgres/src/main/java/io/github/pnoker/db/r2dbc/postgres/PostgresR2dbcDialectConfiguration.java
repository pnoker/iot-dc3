package io.github.pnoker.db.r2dbc.postgres;

import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.db", name = "type", havingValue = "postgres")
public class PostgresR2dbcDialectConfiguration {

    @Bean
    R2dbcDialect r2dbcDialect() {
        return new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint", '"', true);
    }
}

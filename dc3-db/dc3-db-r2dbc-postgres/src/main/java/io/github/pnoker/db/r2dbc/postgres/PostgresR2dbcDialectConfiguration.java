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
package io.github.pnoker.db.r2dbc.postgres;

import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Spring configuration registering postgres r2dbc dialect beans. */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.db", name = "type", havingValue = "postgres")
public class PostgresR2dbcDialectConfiguration {

    @Bean
    R2dbcDialect r2dbcDialect() {
        return new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint");
    }
}

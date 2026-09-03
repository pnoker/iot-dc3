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
package io.github.pnoker.db.postgres.agentic;

import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.r2dbc.core.DatabaseClient;

/**
 * Registers the PostgreSQL {@literal R2DBC} store adapters for the Agentic domain.
 * The stores implement the Agentic {@code Reactive*Store} ports declared by
 * {@code dc3-common-agentic}; exactly one engine adapter must be on the
 * classpath, enforced by the runtime dialect cardinality guard.
 */
@AutoConfiguration
@ConditionalOnClass({DatabaseClient.class, R2dbcDialect.class})
@ComponentScan(basePackages = "io.github.pnoker.db.postgres.agentic")
public class AgenticPostgresStoresConfiguration {
}

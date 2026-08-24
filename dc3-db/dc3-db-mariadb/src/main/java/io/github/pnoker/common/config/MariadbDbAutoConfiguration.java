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

package io.github.pnoker.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * MariaDB dialect contribution: the pagination {@link DbType} consumed by the
 * neutral MybatisPlusConfig, active when {@code dc3.db.type=mariadb}. MariaDB
 * 10.6+ is the floor (SKIP LOCKED, expression defaults); the mapper forks are
 * MySQL-shaped except ODKU row aliases — MariaDB never adopted {@code AS new},
 * its twins reference {@code VALUES(col)} instead (docs/db-dialects.md).
 *
 * @author pnoker
 * @since 2026.8.24
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "dc3.db", name = "type", havingValue = "mariadb")
public class MariadbDbAutoConfiguration {

    @Bean
    public DbType paginationDbType() {
        return DbType.MARIADB;
    }
}

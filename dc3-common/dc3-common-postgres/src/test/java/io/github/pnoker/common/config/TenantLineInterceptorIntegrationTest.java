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

import io.github.pnoker.common.exception.TenantNotScopedException;
import io.github.pnoker.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test exercising the tenant-line interceptor across the full MyBatis-Plus SQL
 * pipeline (not just the handler in isolation): the fail-closed path and the scoped-query
 * path that the Mockito-only unit tests of the gRPC layer cannot reach.
 */
@SpringBootTest
@SpringBootConfiguration
@EnableAutoConfiguration(excludeName = "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration")
@Import({MybatisPlusConfig.class, TenantLineHandlerImpl.class})
@MapperScan(basePackageClasses = TestMapper.class)
class TenantLineInterceptorIntegrationTest {

    @Autowired
    private TestMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.execute("CREATE TABLE dc3_test_entity (id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(32))");
        // Insert via JdbcTemplate so the rows bypass the tenant-line interceptor and we can
        // assert the interceptor filters them on read.
        jdbcTemplate.update("INSERT INTO dc3_test_entity (id, tenant_id, name) VALUES (1, 7, 'tenant-seven')");
        jdbcTemplate.update("INSERT INTO dc3_test_entity (id, tenant_id, name) VALUES (2, 8, 'tenant-eight')");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        jdbcTemplate.execute("DROP TABLE dc3_test_entity");
    }

    @Test
    void selectListFiltersRowsToBoundTenant() {
        TenantContextHolder.setTenantId(7L);
        try {
            List<TestEntityDO> rows = mapper.selectList(null);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).getId()).isEqualTo(1L);
            assertThat(rows.get(0).getTenantId()).isEqualTo(7L);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void selectListRejectsUnscopedQuery() {
        // No tenant bound and not inside runIgnore → the interceptor must fail closed rather
        // than let the query leak across tenants. mybatis-spring wraps the interceptor's
        // TenantNotScopedException in MyBatisSystemException, so assert on the root cause.
        assertThatThrownBy(() -> mapper.selectList(null))
                .hasRootCauseInstanceOf(TenantNotScopedException.class);
    }

    @Test
    void selectListReturnsAllRowsInsideRunIgnore() {
        TenantContextHolder.runIgnoreAction(() -> {
            List<TestEntityDO> rows = mapper.selectList(null);
            assertThat(rows).hasSize(2);
        });
    }
}

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

package io.github.pnoker.common.auth.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the dynamic SQL shape of {@code listToolCatalog}. The optional
 * filters previously used a "(? IS NULL OR ? = '' OR ...)" pattern that breaks
 * on Postgres with {@code stringtype=unspecified} — a bare bind parameter in
 * an {@code IS NULL} context has no inferable type. The dynamic {@code <if>}
 * form must emit the filter clauses only when a non-blank value is bound.
 */
class OAuthMcpToolCatalogSqlTest {

    private static final String STATEMENT_ID = "io.github.pnoker.common.auth.mapper.OAuthMcpMapper.listToolCatalog";

    private static final String AUDIT_STATEMENT_ID = "io.github.pnoker.common.auth.mapper.OAuthMcpMapper.listAudit";

    private static final String AUDIT_COUNT_ID = "io.github.pnoker.common.auth.mapper.OAuthMcpMapper.countAudit";

    private Configuration parseMapper() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream in = OAuthMcpToolCatalogSqlTest.class.getResourceAsStream("/mapping/OAuthMcpMapper.xml")) {
            assertThat(in).isNotNull();
            new XMLMapperBuilder(in, configuration, "mapping/OAuthMcpMapper.xml", configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    @Test
    void blankFiltersOmitBothClauses() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "");
        params.put("riskLevel", "");
        params.put("limit", 500);
        String sql = parseMapper().getMappedStatement(STATEMENT_ID).getBoundSql(params).getSql();
        assertThat(sql).doesNotContain("risk_level").doesNotContain("LIKE");
    }

    @Test
    void presentFiltersBindBothClauses() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", "driver");
        params.put("riskLevel", "LOW");
        params.put("limit", 500);
        String sql = parseMapper().getMappedStatement(STATEMENT_ID).getBoundSql(params).getSql();
        assertThat(sql).contains("risk_level =").contains("LIKE");
    }

    @Test
    void auditPageAndCountShareTheSameFilterShape() throws Exception {
        Map<String, Object> blank = new HashMap<>();
        blank.put("tenantId", 1L);
        blank.put("principalId", null);
        blank.put("toolId", "");
        blank.put("status", "");
        blank.put("riskLevel", "");
        blank.put("limit", 12);
        blank.put("offset", 12L);

        String page = parseMapper().getMappedStatement(AUDIT_STATEMENT_ID).getBoundSql(blank).getSql();
        assertThat(page).contains("tenant_id =").contains("LIMIT ? OFFSET ?")
                .doesNotContain("principal_id =").doesNotContain("risk_level =");

        String count = parseMapper().getMappedStatement(AUDIT_COUNT_ID).getBoundSql(blank).getSql();
        assertThat(count).startsWith("SELECT COUNT(*)").contains("tenant_id =")
                .doesNotContain("LIMIT");
    }
}

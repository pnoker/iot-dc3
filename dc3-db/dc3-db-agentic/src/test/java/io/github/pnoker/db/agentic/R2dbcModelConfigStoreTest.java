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
package io.github.pnoker.db.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckProfile;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.ConnectivityStatusEnum;
import io.github.pnoker.db.core.dialect.StandardR2dbcDialect;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/** Regression coverage for timezone-safe timestamp binds across the model config statements. */
class R2dbcModelConfigStoreTest {

    private final ScriptedAgenticDatabase database = new ScriptedAgenticDatabase();
    private final R2dbcModelConfigStore store = new R2dbcModelConfigStore(
            database,
            ScriptedAgenticDatabase.passThroughOperator(),
            new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint"));

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(7L);
        header.setPrincipalId(42L);
        header.setPrincipalName("operator");
    }

    @Test
    void updateCheckProfileBindsTheCheckTimeAsAnInstant() {
        ConnectivityCheckProfile profile = new ConnectivityCheckProfile(
                ConnectivityStatusEnum.PASS, LocalDateTime.now(ZoneOffset.UTC), 120, null, null, null);

        StepVerifier.create(store.updateCheckProfile(1L, profile, header))
                .expectNext(true)
                .verifyComplete();

        ScriptedAgenticDatabase.Execution update = database.firstMatching("last_check_time = :last_check_time");
        assertThat(update.binds().get("last_check_time"))
                .as("TIMESTAMPTZ binds must carry an explicit instant, not a zone-less wall clock")
                .isInstanceOf(Instant.class);
    }

    @Test
    void deleteBindsOperateTimeAsAnInstant() {
        StepVerifier.create(store.delete(1L, header)).expectNext(true).verifyComplete();

        ScriptedAgenticDatabase.Execution delete = database.firstMatching("SET deleted = 1");
        assertThat(delete.binds().get("operate_time")).isInstanceOf(Instant.class);
    }
}

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.config.AgenticProperties;
import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.utils.AgenticKeyCipher;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticModelProviderTypeEnum;
import io.github.pnoker.common.enums.DefaultFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.r2dbc.spi.Row;
import java.util.Base64;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * API-key encryption wiring at the store boundary: writes bind ciphertext,
 * reads decrypt it, and legacy plaintext rows pass through for lazy
 * migration.
 */
@ExtendWith(MockitoExtension.class)
class R2dbcModelProviderStoreTest {

    private static final String MASTER_KEY = Base64.getEncoder().encodeToString(new byte[] {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31, 32
    });

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private ObjectProvider<AgenticProperties> propertiesProvider;

    private AgenticKeyCipher keyCipher;
    private R2dbcModelProviderStore store;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        AgenticProperties properties = new AgenticProperties();
        properties.setSecurityMasterKey(MASTER_KEY);
        lenient().when(propertiesProvider.getIfAvailable()).thenReturn(properties);
        keyCipher = new AgenticKeyCipher(properties);
        store = new R2dbcModelProviderStore(databaseClient, transactionalOperator, propertiesProvider);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
        lenient()
                .when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void insertBindsTheEncryptedApiKey() {
        DatabaseClient.GenericExecuteSpec insertSpec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
        FetchSpec fetchSpec = mock(FetchSpec.class);
        when(databaseClient.sql(argThat((String sql) -> sql != null && sql.startsWith("INSERT"))))
                .thenReturn(insertSpec);
        when(insertSpec.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));
        stubSelectChain();

        StepVerifier.create(store.insert(provider("sk-plain-secret"), header))
                .expectNextCount(1)
                .verifyComplete();

        // The api_key bind must carry ciphertext, not the plaintext key.
        verify(insertSpec).bind(argThat("api_key"::equals), argThat(value -> {
            String stored = String.valueOf(value);
            return stored.startsWith(AgenticKeyCipher.CIPHERTEXT_PREFIX)
                    && keyCipher.decrypt(stored).equals("sk-plain-secret");
        }));
    }

    @Test
    void readPathDecryptsEncryptedApiKeys() {
        String stored = keyCipher.encrypt("sk-encrypted-at-rest");
        BiFunction<Row, io.r2dbc.spi.RowMetadata, ModelProviderBO> mapper = stubSelectChain();

        ModelProviderBO mapped = mapper.apply(rowWithApiKey(stored), null);

        assertThat(mapped.getApiKey()).isEqualTo("sk-encrypted-at-rest");
    }

    @Test
    void readPathAcceptsLegacyPlaintextForLazyMigration() {
        BiFunction<Row, io.r2dbc.spi.RowMetadata, ModelProviderBO> mapper = stubSelectChain();

        ModelProviderBO mapped = mapper.apply(rowWithApiKey("legacy-plaintext"), null);

        assertThat(mapped.getApiKey()).isEqualTo("legacy-plaintext");
    }

    /** Stub the SELECT chain and return the captured row mapper. */
    @SuppressWarnings("unchecked")
    private BiFunction<Row, io.r2dbc.spi.RowMetadata, ModelProviderBO> stubSelectChain() {
        DatabaseClient.GenericExecuteSpec selectSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        RowsFetchSpec<ModelProviderBO> rowsSpec = mock(RowsFetchSpec.class);
        lenient()
                .when(databaseClient.sql(argThat((String sql) -> sql != null && sql.startsWith("SELECT"))))
                .thenReturn(selectSpec);
        lenient().when(selectSpec.bind(anyString(), any())).thenReturn(selectSpec);
        lenient().when(selectSpec.map(any(BiFunction.class))).thenReturn(rowsSpec);
        lenient().when(rowsSpec.one()).thenReturn(Mono.just(new ModelProviderBO()));

        store.get(100L, header);

        ArgumentCaptor<BiFunction<Row, io.r2dbc.spi.RowMetadata, ModelProviderBO>> mapperCaptor =
                ArgumentCaptor.forClass(BiFunction.class);
        verify(selectSpec).map(mapperCaptor.capture());
        return mapperCaptor.getValue();
    }

    private Row rowWithApiKey(String apiKey) {
        Row row = mock(Row.class);
        lenient().when(row.get("id", Long.class)).thenReturn(100L);
        lenient().when(row.get("name", String.class)).thenReturn("DeepSeek");
        lenient().when(row.get("provider_type", Number.class)).thenReturn((byte) 0);
        lenient().when(row.get("base_url", String.class)).thenReturn("https://api.deepseek.com");
        lenient().when(row.get("api_key", String.class)).thenReturn(apiKey);
        lenient().when(row.get("default_flag", Number.class)).thenReturn((byte) 0);
        lenient().when(row.get("enable_flag", Number.class)).thenReturn((byte) 0);
        lenient().when(row.get("tenant_id", Long.class)).thenReturn(1L);
        lenient().when(row.get("remark", String.class)).thenReturn("");
        lenient().when(row.get("creator_id", Long.class)).thenReturn(2L);
        lenient().when(row.get("creator_name", String.class)).thenReturn("admin");
        lenient().when(row.get("create_time")).thenReturn(null);
        lenient().when(row.get("operator_id", Long.class)).thenReturn(2L);
        lenient().when(row.get("operator_name", String.class)).thenReturn("admin");
        lenient().when(row.get("operate_time")).thenReturn(null);
        lenient().when(row.get("last_check_status", Number.class)).thenReturn(null);
        lenient().when(row.get("last_check_latency_ms", Integer.class)).thenReturn(null);
        lenient().when(row.get("last_check_error_type", String.class)).thenReturn(null);
        lenient().when(row.get("last_check_error_message", String.class)).thenReturn(null);
        lenient().when(row.get("last_check_model", String.class)).thenReturn(null);
        return row;
    }

    private ModelProviderBO provider(String apiKey) {
        ModelProviderBO provider = new ModelProviderBO();
        provider.setId(100L);
        provider.setName("DeepSeek");
        provider.setProviderType(AgenticModelProviderTypeEnum.OPENAI_COMPATIBLE);
        provider.setBaseUrl("https://api.deepseek.com");
        provider.setApiKey(apiKey);
        provider.setDefaultFlag(DefaultFlagEnum.NOT_DEFAULT);
        provider.setEnableFlag(EnableFlagEnum.ENABLE);
        provider.setRemark("");
        provider.setTenantId(1L);
        return provider;
    }
}

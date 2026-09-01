package io.github.pnoker.db.r2dbc.runtime.config;

import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.github.pnoker.db.r2dbc.core.dialect.StandardR2dbcDialect;
import io.github.pnoker.db.r2dbc.runtime.schema.SchemaFingerprintStartupValidator;
import io.github.pnoker.db.r2dbc.runtime.schema.SchemaFingerprintVerifier;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class R2dbcRuntimeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(R2dbcRuntimeAutoConfiguration.class))
            .withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class));

    @Test
    void configuresTransactionsAndMandatoryStartupValidation() {
        SchemaFingerprintVerifier verifier = mock(SchemaFingerprintVerifier.class);
        when(verifier.verify()).thenReturn(Mono.empty());

        contextRunner
                .withBean(R2dbcDialect.class,
                        () -> new StandardR2dbcDialect(
                                "postgres", "public.dc3_schema_fingerprint", '"', true))
                .withBean(SchemaFingerprintVerifier.class, () -> verifier)
                .run(context -> {
                    assertThat(context).hasSingleBean(ReactiveTransactionManager.class);
                    assertThat(context).hasSingleBean(TransactionalOperator.class);
                    assertThat(context).hasSingleBean(PageTransaction.class);
                    assertThat(context).hasSingleBean(SchemaFingerprintStartupValidator.class);
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    void refusesToStartWithoutADialect() {
        contextRunner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .hasMessageContaining("dc3.db.type must select exactly one R2DBC dialect adapter"));
    }

    @Test
    void refusesToStartWithMultipleDialectAdapters() {
        contextRunner
                .withBean("postgresDialect", R2dbcDialect.class,
                        () -> new StandardR2dbcDialect("postgres", "public.dc3_schema_fingerprint", '"', true))
                .withBean("mysqlDialect", R2dbcDialect.class,
                        () -> new StandardR2dbcDialect("mysql", "dc3_runtime.dc3_schema_fingerprint", '`', false))
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("must select exactly one R2DBC dialect adapter"));
    }

    @Test
    void refusesToStartWithoutAR2dbcConnectionFactory() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(R2dbcRuntimeAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("spring.r2dbc.url must be configured"));
    }
}

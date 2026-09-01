package io.github.pnoker.db.r2dbc.runtime.config;

import io.github.pnoker.db.r2dbc.runtime.schema.SchemaFingerprintVerifier;
import io.github.pnoker.db.r2dbc.runtime.schema.SchemaFingerprintStartupValidator;
import io.github.pnoker.db.r2dbc.runtime.operation.R2dbcOperationRepository;
import io.github.pnoker.db.r2dbc.core.transaction.PageTransaction;
import io.github.pnoker.db.r2dbc.runtime.transaction.SpringR2dbcPageTransaction;
import io.github.pnoker.db.r2dbc.core.operation.OperationRepository;
import io.github.pnoker.db.r2dbc.core.dialect.R2dbcDialect;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@AutoConfiguration(
        after = {R2dbcAutoConfiguration.class, R2dbcTransactionManagerAutoConfiguration.class},
        afterName = {
                "io.github.pnoker.db.r2dbc.postgres.PostgresR2dbcDialectConfiguration"
        })
@ConditionalOnClass({ConnectionFactory.class, R2dbcTransactionManager.class})
@EnableConfigurationProperties(R2dbcRuntimeProperties.class)
public class R2dbcRuntimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConnectionFactory.class)
    Object dc3MissingConnectionFactoryGuard(Environment environment) {
        String url = environment.getProperty("spring.r2dbc.url");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("spring.r2dbc.url must be configured; JDBC datasource is not supported");
        }
        throw new IllegalStateException("R2DBC ConnectionFactory could not be created from spring.r2dbc.url");
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    Object dc3DialectCardinalityGuard(ObjectProvider<R2dbcDialect> dialects) {
        long count = dialects.stream().count();
        if (count != 1) {
            throw new IllegalStateException("dc3.db.type must select exactly one R2DBC dialect adapter; found " + count);
        }
        return new Object();
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    @ConditionalOnMissingBean(ReactiveTransactionManager.class)
    ReactiveTransactionManager dc3TransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    @ConditionalOnBean(ReactiveTransactionManager.class)
    @ConditionalOnMissingBean
    TransactionalOperator dc3TransactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    @Bean
    @ConditionalOnBean(ReactiveTransactionManager.class)
    @ConditionalOnMissingBean
    PageTransaction dc3PageTransaction(ReactiveTransactionManager transactionManager) {
        return new SpringR2dbcPageTransaction(transactionManager);
    }

    @Bean
    @ConditionalOnBean({DatabaseClient.class, TransactionalOperator.class, R2dbcDialect.class})
    @ConditionalOnMissingBean(OperationRepository.class)
    OperationRepository dc3OperationRepository(DatabaseClient databaseClient,
                                                TransactionalOperator transactionalOperator,
                                                R2dbcDialect dialect) {
        return new R2dbcOperationRepository(databaseClient, transactionalOperator, dialect);
    }

    @Bean
    @ConditionalOnBean({ConnectionFactory.class, R2dbcDialect.class})
    @ConditionalOnMissingBean
    SchemaFingerprintVerifier schemaFingerprintVerifier(
            ConnectionFactory connectionFactory,
            R2dbcRuntimeProperties properties,
            ObjectProvider<R2dbcDialect> dialects) {
        R2dbcDialect dialect = dialects.getIfUnique();
        if (dialect == null) {
            throw new IllegalStateException("dc3.db.type must select exactly one R2DBC dialect adapter");
        }
        return new SchemaFingerprintVerifier(connectionFactory, properties, dialect);
    }

    @Bean
    @ConditionalOnBean({ConnectionFactory.class, SchemaFingerprintVerifier.class})
    @ConditionalOnMissingBean
    SchemaFingerprintStartupValidator schemaFingerprintStartupValidator(
            SchemaFingerprintVerifier verifier, R2dbcRuntimeProperties properties) {
        return new SchemaFingerprintStartupValidator(verifier, properties);
    }
}

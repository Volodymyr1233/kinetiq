package com.kinetiq.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.fixture.domain.input.PingUseCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@SpringJUnitConfig
class TransactionConfigTest {

    @Autowired
    private PingUseCase useCase;

    @Autowired
    private Runnable notAUseCase;

    @Autowired
    private RecordingTransactionManager transactions;

    @BeforeEach
    void forgetEarlierTransactions() {
        transactions.events.clear();
    }

    @Test
    void shouldCommit_whenUseCaseSucceeds() {
        useCase.ping();

        assertThat(transactions.events).containsExactly("begin", "commit");
    }

    @Test
    void shouldRollBack_whenUseCaseThrows() {
        assertThatThrownBy(() -> useCase.fail()).isInstanceOf(IllegalStateException.class);

        assertThat(transactions.events).containsExactly("begin", "rollback");
    }

    @Test
    void shouldNotOpenTransaction_whenBeanIsNotAUseCase() {
        notAUseCase.run();

        assertThat(transactions.events).isEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransactionConfig.class)
    static class Fixture {

        @Bean
        RecordingTransactionManager transactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        PingUseCase pingUseCase() {
            return new PingUseCase() {
                @Override
                public void ping() {}

                @Override
                public void fail() {
                    throw new IllegalStateException("boom");
                }
            };
        }

        @Bean
        Runnable notAUseCase() {
            return () -> {};
        }
    }

    static class RecordingTransactionManager implements PlatformTransactionManager {

        final List<String> events = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            events.add("begin");
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            events.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status) {
            events.add("rollback");
        }
    }
}

package com.damianhoward.stocks.config;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PersistenceConfigTest {

    @Test
    void buildsTransactionTemplateAroundGivenManager() {
        PlatformTransactionManager manager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };

        TransactionTemplate template = new PersistenceConfig().transactionTemplate(manager);

        assertNotNull(template);
        assertSame(manager, template.getTransactionManager());
    }
}

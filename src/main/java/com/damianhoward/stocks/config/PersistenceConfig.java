package com.damianhoward.stocks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class PersistenceConfig {

    /**
     * Programmatic transaction helper used by the retriever stages to perform an
     * atomic delete-then-save swap. External data is fetched and parsed first,
     * outside any transaction; only the in-memory swap runs inside this template,
     * so a failed fetch never deletes existing good data and a failed save rolls
     * back the delete.
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}

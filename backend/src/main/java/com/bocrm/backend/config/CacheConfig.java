/*
 * BoringOldCRM - Open-source multi-tenant CRM
 * Copyright (C) 2026 Ricardo Salvador
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Source: https://github.com/N0t4R0b0t/BoringOldCRM
 */
package com.bocrm.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Step 8: Caching Configuration
 * Configures Spring Boot cache abstraction with two backends:
 * - Development: In-memory ConcurrentMapCache (fast, local-only)
 * - Production: Redis (distributed, persistent across instances)
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * Development cache manager - in-memory using ConcurrentHashMap
     * Used for local development and testing
     */
    @Bean
    @ConditionalOnProperty(name = "cache.type", havingValue = "local", matchIfMissing = true)
    public CacheManager devCacheManager() {
        log.info("Initializing development cache manager (in-memory)");
        return new ConcurrentMapCacheManager(
                "CustomerDetail",
                "CustomerTable",
                "OpportunityDetail",
                "OpportunityTable",
                "ContactDetail",
                "ContactTable",
                "ActivityDetail",
                "ActivityTable",
                "DocumentDetail",
                "DocumentList",
                "DocumentTemplateDetail",
                "DocumentTemplateList",
                "CustomRecordDetail",
                "CustomRecordTable",
                "OrderDetail",
                "OrderTable",
                "InvoiceDetail",
                "InvoiceTable",
                "PolicyRules"
        );
    }

    /**
     * Production cache manager - Redis-backed for distributed caching
     * Used in production environments for cross-instance cache sharing
     */
    @Bean
    @Profile("prod")
    @ConditionalOnProperty(name = "cache.type", havingValue = "redis")
    public CacheManager prodCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Initializing production cache manager (Redis)");
        return RedisCacheManager.create(connectionFactory);
    }

}

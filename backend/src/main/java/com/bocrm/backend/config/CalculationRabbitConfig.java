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

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
/**
 * CalculationRabbitConfig.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Configuration
public class CalculationRabbitConfig {

    public static final String QUEUE = "calculated-field-refresh";
    public static final String EXCHANGE = "calculated-field-refresh.exchange";
    public static final String ROUTING_KEY = "calculated-field-refresh";

    @Bean
    public Queue calculationQueue() {
        return new Queue(QUEUE, true); // durable=true: messages survive broker restart
    }

    @Bean
    public DirectExchange calculationExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding calculationBinding(Queue calculationQueue, DirectExchange calculationExchange) {
        return BindingBuilder.bind(calculationQueue).to(calculationExchange).with(ROUTING_KEY);
    }

    @Bean
    @ConditionalOnBean(ConnectionFactory.class)
    public RabbitTemplate calculationRabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    /**
     * Listener container factory that allows deserialization of our internal DTO classes.
     * Scoped to com.bocrm.backend.dto.* — only our own trusted message payloads.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory calculationListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of("com.bocrm.backend.dto.*"));

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        return factory;
    }
}

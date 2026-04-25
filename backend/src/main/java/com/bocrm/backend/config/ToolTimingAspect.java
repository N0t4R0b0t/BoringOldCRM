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

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
/**
 * ToolTimingAspect.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Aspect
@Component
@Slf4j
public class ToolTimingAspect {

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object timeToolCall(ProceedingJoinPoint pjp) throws Throwable {
        String name = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        long start = System.nanoTime();
        log.info("tool.start name={}", name);
        try {
            Object result = pjp.proceed();
            long ms = (System.nanoTime() - start) / 1_000_000L;
            log.info("tool.end name={} durationMs={}", name, ms);
            return result;
        } catch (Throwable t) {
            long ms = (System.nanoTime() - start) / 1_000_000L;
            log.warn("tool.error name={} durationMs={} error={}", name, ms, t.toString());
            throw t;
        }
    }
}

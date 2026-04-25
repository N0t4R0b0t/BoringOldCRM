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
package com.bocrm.backend.controller;

import com.bocrm.backend.dto.OnboardingSuggestionsDTO;
import com.bocrm.backend.dto.OnboardingSuggestionsRequest;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.service.OnboardingService;
import com.bocrm.backend.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Onboarding endpoints for generating AI-powered setup suggestions.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@RestController
@RequestMapping("/assistant")
@Tag(name = "Onboarding", description = "Onboarding and setup suggestions")
@Slf4j
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * Generates AI suggestions for custom fields, policies, and sample data
     * based on the organization's name and bio.
     */
    @PostMapping("/onboarding-suggestions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate onboarding suggestions",
               description = "Get AI-powered suggestions for custom fields, policies, and sample data")
    public ResponseEntity<OnboardingSuggestionsDTO> getOnboardingSuggestions(
            @RequestBody OnboardingSuggestionsRequest request) {

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context not set");
        }

        OnboardingSuggestionsDTO suggestions = onboardingService.generateSuggestions(tenantId, request);
        return ResponseEntity.ok(suggestions);
    }
}

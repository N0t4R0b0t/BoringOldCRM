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

import com.bocrm.backend.dto.*;
import com.bocrm.backend.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
/**
 * ActivityController.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@RestController
@RequestMapping("/activities")
@Tag(name = "Activities", description = "Activity management")
@Slf4j
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @Operation(summary = "Create a new activity")
    public ResponseEntity<ActivityDTO> createActivity(@Valid @RequestBody CreateActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.createActivity(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID")
    public ResponseEntity<ActivityDTO> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivity(id));
    }

        @GetMapping
        @Operation(summary = "List all activities")
        public ResponseEntity<PagedResponse<ActivityDTO>> listActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> type,
            @RequestParam(required = false) List<String> status,
            @RequestParam Map<String, List<String>> allParams
        ) {
        // Extract custom field filters (cf_*)
        Map<String, List<String>> customFieldFilters = allParams.entrySet().stream()
            .filter(e -> e.getKey().startsWith("cf_"))
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getKey().substring(3),
                Map.Entry::getValue
            ));
        return ResponseEntity.ok(activityService.listActivities(page, size, sortBy, sortOrder, search, type, status, customFieldFilters));
        }

    @GetMapping("/search")
    @Operation(summary = "Search activities by text")
    public ResponseEntity<PagedResponse<ActivityDTO>> searchActivities(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activityService.search(term, page, size));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an activity")
    public ResponseEntity<ActivityDTO> updateActivity(@PathVariable Long id, @Valid @RequestBody UpdateActivityRequest request) {
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an activity")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}

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
package com.bocrm.backend.service;

import com.bocrm.backend.dto.UpdatePreferencesRequest;
import com.bocrm.backend.dto.UserDTO;
import com.bocrm.backend.entity.User;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.UserRepository;
import com.bocrm.backend.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * UserService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserDTO updatePreferences(UpdatePreferencesRequest request) {
        Long userId = TenantContext.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPreferences(request.getPreferences());
        user = userRepository.save(user);

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .preferences(user.getPreferences())
                .build();
    }
}

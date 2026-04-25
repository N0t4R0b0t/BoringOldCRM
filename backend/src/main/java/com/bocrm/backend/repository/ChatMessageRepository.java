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
package com.bocrm.backend.repository;

import com.bocrm.backend.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
/**
 * ChatMessageRepository.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByTenantIdAndUserIdOrderByCreatedAtAsc(Long tenantId, Long userId);
    List<ChatMessage> findByTenantIdAndUserIdAndContextEntityTypeAndContextEntityIdOrderByCreatedAtAsc(
            Long tenantId, Long userId, String contextEntityType, Long contextEntityId);

    @Query("SELECT m FROM ChatMessage m WHERE m.tenantId = :tenantId AND m.userId = :userId AND m.sessionId = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByTenantIdAndUserIdAndSessionIdOrderByCreatedAtAsc(
            @Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("sessionId") String sessionId);

    @Query("SELECT m FROM ChatMessage m WHERE m.tenantId = :tenantId AND m.userId = :userId AND m.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentBySession(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                                          @Param("sessionId") String sessionId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.tenantId = :tenantId AND m.userId = :userId AND m.sessionId = :sessionId AND m.role = 'assistant' ORDER BY m.createdAt DESC")
    List<ChatMessage> findLastAssistantMessageBySession(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                                                        @Param("sessionId") String sessionId, Pageable pageable);

    void deleteByTenantIdAndUserId(Long tenantId, Long userId);
    List<ChatMessage> findByTenantId(Long tenantId);
}

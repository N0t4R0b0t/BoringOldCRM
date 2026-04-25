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

import com.bocrm.backend.dto.AccessGrantDTO;
import com.bocrm.backend.dto.RecordAccessSummaryDTO;
import com.bocrm.backend.entity.RecordAccessGrant;
import com.bocrm.backend.entity.RecordAccessPolicy;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.exception.ResourceNotFoundException;
import com.bocrm.backend.repository.RecordAccessGrantRepository;
import com.bocrm.backend.repository.RecordAccessPolicyRepository;
import com.bocrm.backend.repository.UserGroupRepository;
import com.bocrm.backend.repository.UserGroupMembershipRepository;
import com.bocrm.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
/**
 * AccessControlService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class AccessControlService {

    private final RecordAccessPolicyRepository policyRepository;
    private final RecordAccessGrantRepository grantRepository;
    private final UserGroupMembershipRepository groupMembershipRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    // ─── Owner lookup ─────────────────────────────────────────────────────────

    private Long lookupOwnerId(String entityType, Long entityId) {
        String table = switch (entityType) {
            case "Opportunity" -> "opportunities";
            case "Activity" -> "activities";
            case "CustomRecord" -> "custom_records";
            case "TenantDocument" -> "tenant_documents";
            default -> null;
        };
        if (table == null) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT owner_id FROM " + table + " WHERE id = ?", Long.class, entityId);
        } catch (Exception e) {
            log.debug("Could not look up owner_id for {}/{}: {}", entityType, entityId, e.getMessage());
            return null;
        }
    }

    // ─── Role helpers ────────────────────────────────────────────────────────

    public boolean isManager(String role) {
        return "manager".equalsIgnoreCase(role);
    }

    // ─── Access checks ───────────────────────────────────────────────────────

    /**
     * Returns true if the user can VIEW the entity (i.e., it is not hidden from them).
     */
    public boolean canView(Long userId, String role, Long tenantId,
                            String entityType, Long entityId, Long ownerId) {
        if (isManager(role)) return true;
        Optional<RecordAccessPolicy> policy = policyRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        if (policy.isEmpty()) return true;
        if (userId != null && userId.equals(ownerId)) return true;

        String mode = policy.get().getAccessMode();
        if ("READ_ONLY".equals(mode)) return true; // read-only: still visible

        // HIDDEN — check for any grant
        return hasAnyGrant(userId, tenantId, entityType, entityId);
    }

    /**
     * Returns true if the user can WRITE (edit/delete) the entity.
     */
    public boolean canWrite(Long userId, String role, Long tenantId,
                             String entityType, Long entityId, Long ownerId) {
        if (isManager(role)) return true;
        Optional<RecordAccessPolicy> policy = policyRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        if (policy.isEmpty()) return true;
        if (userId != null && userId.equals(ownerId)) return true;

        // Check for a WRITE grant (direct or group)
        return hasWriteGrant(userId, tenantId, entityType, entityId);
    }

    /**
     * Returns the set of entity IDs from the candidate list that the user CANNOT see
     * (HIDDEN records with no access grant for this user).
     */
    @Transactional(readOnly = true)
    public Set<Long> getHiddenEntityIds(Long userId, String role, Long tenantId,
                                         String entityType, List<Long> candidateIds) {
        if (isManager(role) || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptySet();
        }

        // Get HIDDEN policy entity IDs from the candidate list
        List<Long> hiddenIds = policyRepository.findHiddenEntityIds(tenantId, entityType, candidateIds);
        if (hiddenIds.isEmpty()) return Collections.emptySet();

        // Remove IDs where the user is the owner — owner always sees their own records
        // (We can't filter by ownerId here without loading entities, so we use grants + the owner check
        //  happens upstream when the entity is loaded individually. For list view we conservatively
        //  check grants and let owners through by checking their userId against ownerId at DTO mapping.)
        // For simplicity: get granted IDs the user can see via explicit grants
        Set<Long> userGroupIds = groupMembershipRepository.findGroupIdsByUserIdAndTenantId(userId, tenantId);
        Set<Long> grantedIds = grantRepository.findGrantedEntityIds(
                tenantId, entityType, hiddenIds, userId, userGroupIds);

        // hidden - granted = truly hidden for this user
        Set<Long> result = new HashSet<>(hiddenIds);
        result.removeAll(grantedIds);
        return result;
    }

    // ─── Policy management ───────────────────────────────────────────────────

    @Transactional
    public void setPolicy(Long userId, String role, Long tenantId,
                           String entityType, Long entityId, Long ownerId, String accessMode) {
        if (ownerId == null) ownerId = lookupOwnerId(entityType, entityId);
        requireCanManage(userId, role, ownerId);
        validateAccessMode(accessMode);

        Optional<RecordAccessPolicy> existing = policyRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        if (existing.isPresent()) {
            existing.get().setAccessMode(accessMode);
            policyRepository.save(existing.get());
        } else {
            policyRepository.save(RecordAccessPolicy.builder()
                    .tenantId(tenantId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .accessMode(accessMode)
                    .createdBy(userId)
                    .build());
        }
        log.debug("Access policy set: {}/{} → {} by user {}", entityType, entityId, accessMode, userId);
    }

    @Transactional
    public void removePolicy(Long userId, String role, Long tenantId,
                              String entityType, Long entityId, Long ownerId) {
        if (ownerId == null) ownerId = lookupOwnerId(entityType, entityId);
        requireCanManage(userId, role, ownerId);
        policyRepository.deleteByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        grantRepository.deleteByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        log.debug("Access policy removed: {}/{} by user {}", entityType, entityId, userId);
    }

    // ─── Grant management ────────────────────────────────────────────────────

    @Transactional
    public AccessGrantDTO addGrant(Long userId, String role, Long tenantId,
                                    String entityType, Long entityId, Long ownerId,
                                    String granteeType, Long granteeId, String permission) {
        if (ownerId == null) ownerId = lookupOwnerId(entityType, entityId);
        requireCanManage(userId, role, ownerId);
        validateGranteeType(granteeType);
        validatePermission(permission);

        // Ensure a policy exists; default to READ_ONLY if not set
        Optional<RecordAccessPolicy> policy = policyRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        if (policy.isEmpty()) {
            policyRepository.save(RecordAccessPolicy.builder()
                    .tenantId(tenantId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .accessMode("READ_ONLY")
                    .createdBy(userId)
                    .build());
        }

        Optional<RecordAccessGrant> existing = grantRepository
                .findByTenantIdAndEntityTypeAndEntityIdAndGranteeTypeAndGranteeId(
                        tenantId, entityType, entityId, granteeType, granteeId);
        RecordAccessGrant grant;
        if (existing.isPresent()) {
            existing.get().setPermission(permission);
            grant = grantRepository.save(existing.get());
        } else {
            grant = grantRepository.save(RecordAccessGrant.builder()
                    .tenantId(tenantId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .granteeType(granteeType)
                    .granteeId(granteeId)
                    .permission(permission)
                    .build());
        }

        if ("USER".equals(granteeType)) {
            notificationService.notifyAccessGranted(tenantId, granteeId, userId, "System",
                    entityType, entityId, entityType, permission);
        }

        return toGrantDTO(grant);
    }

    @Transactional
    public void removeGrant(Long userId, String role, Long tenantId,
                             String entityType, Long entityId, Long ownerId, Long grantId) {
        if (ownerId == null) ownerId = lookupOwnerId(entityType, entityId);
        requireCanManage(userId, role, ownerId);
        RecordAccessGrant grant = grantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Grant not found"));
        if (!grant.getTenantId().equals(tenantId)
                || !grant.getEntityType().equals(entityType)
                || !grant.getEntityId().equals(entityId)) {
            throw new ForbiddenException("Access denied");
        }
        grantRepository.delete(grant);
    }

    // ─── Summary ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RecordAccessSummaryDTO getSummary(Long userId, String role, Long tenantId,
                                              String entityType, Long entityId, Long ownerId) {
        if (ownerId == null) ownerId = lookupOwnerId(entityType, entityId);
        Optional<RecordAccessPolicy> policy = policyRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        List<RecordAccessGrant> grants = grantRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);

        String ownerEmail = null;
        if (ownerId != null) {
            ownerEmail = userRepository.findById(ownerId)
                    .map(u -> u.getEmail())
                    .orElse(null);
        }

        List<AccessGrantDTO> grantDTOs = grants.stream()
                .map(this::toGrantDTO)
                .collect(Collectors.toList());

        boolean canManage = isManager(role) || (userId != null && userId.equals(ownerId));

        return RecordAccessSummaryDTO.builder()
                .entityType(entityType)
                .entityId(entityId)
                .accessMode(policy.map(RecordAccessPolicy::getAccessMode).orElse(null))
                .ownerId(ownerId)
                .ownerEmail(ownerEmail)
                .grants(grantDTOs)
                .canManage(canManage)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean hasAnyGrant(Long userId, Long tenantId, String entityType, Long entityId) {
        Set<Long> groupIds = groupMembershipRepository.findGroupIdsByUserIdAndTenantId(userId, tenantId);
        Set<Long> granted = grantRepository.findGrantedEntityIds(
                tenantId, entityType, List.of(entityId), userId, groupIds);
        return granted.contains(entityId);
    }

    private boolean hasWriteGrant(Long userId, Long tenantId, String entityType, Long entityId) {
        List<RecordAccessGrant> grants = grantRepository
                .findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
        Set<Long> userGroupIds = groupMembershipRepository
                .findGroupIdsByUserIdAndTenantId(userId, tenantId);
        return grants.stream()
                .filter(g -> "WRITE".equals(g.getPermission()))
                .anyMatch(g -> ("USER".equals(g.getGranteeType()) && userId.equals(g.getGranteeId()))
                        || ("GROUP".equals(g.getGranteeType()) && userGroupIds.contains(g.getGranteeId())));
    }

    private void requireCanManage(Long userId, String role, Long ownerId) {
        if (!isManager(role) && (userId == null || !userId.equals(ownerId))) {
            throw new ForbiddenException("Only the owner or a manager can manage access control");
        }
    }

    private void validateAccessMode(String mode) {
        if (!"READ_ONLY".equals(mode) && !"HIDDEN".equals(mode)) {
            throw new IllegalArgumentException("accessMode must be READ_ONLY or HIDDEN");
        }
    }

    private void validateGranteeType(String type) {
        if (!"USER".equals(type) && !"GROUP".equals(type)) {
            throw new IllegalArgumentException("granteeType must be USER or GROUP");
        }
    }

    private void validatePermission(String permission) {
        if (!"READ".equals(permission) && !"WRITE".equals(permission)) {
            throw new IllegalArgumentException("permission must be READ or WRITE");
        }
    }

    private AccessGrantDTO toGrantDTO(RecordAccessGrant grant) {
        String granteeName = null;
        if ("USER".equals(grant.getGranteeType())) {
            granteeName = userRepository.findById(grant.getGranteeId())
                    .map(u -> u.getEmail()).orElse(null);
        } else if ("GROUP".equals(grant.getGranteeType())) {
            granteeName = userGroupRepository.findById(grant.getGranteeId())
                    .map(g -> g.getName()).orElse(null);
        }
        return AccessGrantDTO.builder()
                .id(grant.getId())
                .granteeType(grant.getGranteeType())
                .granteeId(grant.getGranteeId())
                .granteeName(granteeName)
                .permission(grant.getPermission())
                .createdAt(grant.getCreatedAt())
                .build();
    }
}

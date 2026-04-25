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

import com.bocrm.backend.dto.TenantDTO;
import com.bocrm.backend.entity.Tenant;
import com.bocrm.backend.entity.TenantSettings;
import com.bocrm.backend.repository.TenantRepository;
import com.bocrm.backend.repository.TenantSettingsRepository;
import com.bocrm.backend.repository.TenantMembershipRepository;
import com.bocrm.backend.repository.TenantSubscriptionRepository;
import com.bocrm.backend.exception.ForbiddenException;
import com.bocrm.backend.shared.TenantContext;
import com.bocrm.backend.shared.TenantSchema;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
/**
 * TenantAdminService.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Service
@Slf4j
public class TenantAdminService {

    static final String DEFAULT_SETTINGS_JSON = "{\"hiddenModules\":[\"Order\",\"Invoice\"],\"reportBuilderEnabled\":false}";

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final ObjectMapper objectMapper;
    private final TenantSchemaProvisioningService tenantSchemaProvisioningService;
    private final TenantFlywayMigrationService tenantFlywayMigrationService;
    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient;
    private final HttpClient tlsv12HttpClient;

    public TenantAdminService(TenantRepository tenantRepository,
                              TenantSettingsRepository tenantSettingsRepository,
                              TenantMembershipRepository tenantMembershipRepository,
                              TenantSubscriptionRepository tenantSubscriptionRepository,
                              ObjectMapper objectMapper,
                              TenantSchemaProvisioningService tenantSchemaProvisioningService,
                              TenantFlywayMigrationService tenantFlywayMigrationService,
                              JdbcTemplate jdbcTemplate,
                              HttpClient lenientHttpClient,
                              @org.springframework.beans.factory.annotation.Qualifier("tlsv12HttpClient") HttpClient tlsv12HttpClient) {
        this.tenantRepository = tenantRepository;
        this.tenantSettingsRepository = tenantSettingsRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.objectMapper = objectMapper;
        this.tenantSchemaProvisioningService = tenantSchemaProvisioningService;
        this.tenantFlywayMigrationService = tenantFlywayMigrationService;
        this.jdbcTemplate = jdbcTemplate;
        this.httpClient = lenientHttpClient;
        this.tlsv12HttpClient = tlsv12HttpClient;
    }

    private void requireAdmin(Long userId) {
        if (userId == null) throw new ForbiddenException("Not authenticated");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ROLE_SYSTEM_ADMIN".equals(a));
        if (!hasAdmin) throw new ForbiddenException("Access denied");
    }

    /**
     * Holder for image download result with content type.
     */
    private static class ImageDownloadResult {
        final byte[] bytes;
        final String contentType;
        ImageDownloadResult(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType;
        }
    }

    /**
     * Download image with SSL handshake retry fallback.
     * Tries: (1) default lenient client, (2) TLSv1.2 client.
     * Returns bytes + content-type on success, or throws on failure.
     */
    private ImageDownloadResult downloadImageWithRetry(URI uri, String imageUrl) throws Exception {
        Exception lastError = null;

        // Attempt 1: Try default lenient client
        try {
            return downloadImage(httpClient, uri, imageUrl);
        } catch (Exception e) {
            log.debug("Logo download attempt 1 failed (lenient client): {}", e.getClass().getSimpleName());
            lastError = e;
        }

        // Attempt 2: Try TLSv1.2 fallback
        try {
            log.debug("Retrying logo download with TLSv1.2 client: {}", imageUrl);
            return downloadImage(tlsv12HttpClient, uri, imageUrl);
        } catch (Exception e) {
            log.debug("Logo download attempt 2 failed (TLSv1.2): {}", e.getClass().getSimpleName());
            lastError = e;
        }

        // Both attempts failed
        log.warn("Failed to download logo after all retry attempts: {}", imageUrl, lastError);
        throw new IllegalArgumentException("Could not download image from URL after retries: " +
                (lastError != null ? lastError.getClass().getSimpleName() : "unknown error"));
    }

    /**
     * Download image from URI using given HttpClient.
     * Validates HTTP status and Content-Type.
     */
    private ImageDownloadResult downloadImage(HttpClient client, URI uri, String imageUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("image/png");
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Not an image (Content-Type: " + contentType + ")");
        }

        return new ImageDownloadResult(response.body(), contentType);
    }

    @Transactional(readOnly = true)
    public List<TenantDTO> listTenants(int page, int size) {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.ASC, "name"));
        Page<Tenant> p = tenantRepository.findAll(pageable);
        return p.getContent().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantDTO getTenant(Long id) {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);
        Tenant t = tenantRepository.findById(id).orElse(null);
        if (t == null) return null;
        return toDTO(t);
    }

    @Transactional
    public TenantDTO createTenant(String name) {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);
        Tenant t = Tenant.builder().name(name).status("active").build();
        Tenant saved = tenantRepository.save(t);
        tenantSettingsRepository.save(TenantSettings.builder()
                .tenantId(saved.getId())
                .settingsJsonb(DEFAULT_SETTINGS_JSON)
                .build());
        tenantFlywayMigrationService.migrateSchema(saved.getId());
        tenantSchemaProvisioningService.seedDefaultDocumentTemplates(saved.getId(), TenantSchema.fromTenantId(saved.getId()));
        tenantSchemaProvisioningService.seedDefaultFieldOptions(saved.getId(), TenantSchema.fromTenantId(saved.getId()));

        // Auto-add the creating admin as SYSTEM_ADMIN member of the new tenant
        com.bocrm.backend.entity.TenantMembership membership = com.bocrm.backend.entity.TenantMembership.builder()
                .tenantId(saved.getId())
                .userId(userId)
                .role("SYSTEM_ADMIN")
                .status("active")
                .build();
        tenantMembershipRepository.save(membership);

        return toDTO(saved);
    }

    @Transactional
    public TenantDTO updateTenant(Long id, String name, JsonNode settings) {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);
        Tenant t = tenantRepository.findById(id).orElseThrow(() -> new RuntimeException("Tenant not found"));
        if (name != null && !name.isBlank()) t.setName(name);
        Tenant saved = tenantRepository.save(t);

        if (settings != null) {
            TenantSettings ts = tenantSettingsRepository.findByTenantId(id).orElseGet(() -> TenantSettings.builder().tenantId(id).settingsJsonb("{}").build());
            try {
                ts.setSettingsJsonb(objectMapper.writeValueAsString(settings));
            } catch (Exception e) {
                ts.setSettingsJsonb("{}");
            }
            tenantSettingsRepository.save(ts);
        }

        return toDTO(saved);
    }

    /**
     * Downloads an image from the given URL, validates it, converts to a base64
     * data URI, and saves it as the tenant logo.
     *
     * SSRF protection: only http/https allowed; loopback, private, and link-local
     * addresses are rejected.
     */
    @Transactional
    public String setLogoFromUrl(Long tenantId, String imageUrl) throws Exception {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);

        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL: " + imageUrl);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http and https URLs are allowed");
        }

        // SSRF guard: resolve the hostname and reject private/internal addresses
        try {
            InetAddress resolved = InetAddress.getByName(uri.getHost());
            if (resolved.isLoopbackAddress() || resolved.isSiteLocalAddress()
                    || resolved.isLinkLocalAddress() || resolved.isAnyLocalAddress()
                    || resolved.isMulticastAddress()) {
                throw new IllegalArgumentException("URL resolves to a private or internal address");
            }
        } catch (Exception e) {
            log.warn("Failed to validate logo URL: {}", imageUrl, e);
            throw new IllegalArgumentException("Could not validate URL: " + e.getMessage());
        }

        ImageDownloadResult result = downloadImageWithRetry(uri, imageUrl);
        byte[] imageBytes = result.bytes;
        String contentType = result.contentType;

        if (imageBytes.length > 512 * 1024) {
            log.warn("Logo image exceeds 512 KB limit ({} bytes): {}", imageBytes.length, imageUrl);
            throw new IllegalArgumentException("Image exceeds 512 KB limit");
        }

        String mimeType = contentType.split(";")[0].trim();
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantSettings.builder().tenantId(tenantId).settingsJsonb("{}").build());
        try {
            tools.jackson.databind.node.ObjectNode merged =
                    (tools.jackson.databind.node.ObjectNode) objectMapper.readTree(ts.getSettingsJsonb());
            merged.put("logoUrl", dataUri);
            ts.setSettingsJsonb(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            tools.jackson.databind.node.ObjectNode fresh = objectMapper.createObjectNode();
            fresh.put("logoUrl", dataUri);
            ts.setSettingsJsonb(objectMapper.writeValueAsString(fresh));
        }
        tenantSettingsRepository.save(ts);
        return dataUri;
    }

    /**
     * Accepts an uploaded image file (up to 10 MB), compresses it to ≤512 KB,
     * and saves it as the tenant logo data URI.
     */
    @Transactional
    public String setLogoFromUpload(Long tenantId, MultipartFile file) throws Exception {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        long maxUploadBytes = 10 * 1024 * 1024; // 10 MB
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException("File exceeds 10 MB upload limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file is not an image");
        }

        byte[] imageBytes = compressToLimit(file.getBytes(), contentType, 512 * 1024);
        String mimeType = contentType.split(";")[0].trim();
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);

        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> TenantSettings.builder().tenantId(tenantId).settingsJsonb("{}").build());
        try {
            tools.jackson.databind.node.ObjectNode merged =
                    (tools.jackson.databind.node.ObjectNode) objectMapper.readTree(ts.getSettingsJsonb());
            merged.put("logoUrl", dataUri);
            ts.setSettingsJsonb(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            tools.jackson.databind.node.ObjectNode fresh = objectMapper.createObjectNode();
            fresh.put("logoUrl", dataUri);
            ts.setSettingsJsonb(objectMapper.writeValueAsString(fresh));
        }
        tenantSettingsRepository.save(ts);
        return dataUri;
    }

    /**
     * Compresses imageBytes to fit within maxBytes.
     * Strategy: iteratively reduce JPEG quality; if still too large, halve dimensions and retry.
     * PNG/GIF/WEBP are converted to JPEG for compression (except SVG which is returned as-is).
     */
    private byte[] compressToLimit(byte[] original, String contentType, int maxBytes) throws Exception {
        if (original.length <= maxBytes) {
            return original;
        }

        // SVG is text-based — cannot compress via ImageIO; just return as-is
        if (contentType.contains("svg")) {
            return original;
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));
        if (image == null) {
            throw new IllegalArgumentException("Could not decode image");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Try progressively lower quality levels first (keeping original dimensions)
        float[] qualities = {0.85f, 0.70f, 0.55f, 0.40f, 0.25f};
        for (float quality : qualities) {
            byte[] compressed = encodeAsJpeg(image, quality);
            if (compressed.length <= maxBytes) {
                log.debug("Logo compressed to {}KB at quality {}", compressed.length / 1024, quality);
                return compressed;
            }
        }

        // If still too large, halve dimensions and retry until it fits or becomes tiny
        while (width > 64 && height > 64) {
            width /= 2;
            height /= 2;
            image = resizeImage(image, width, height);
            byte[] compressed = encodeAsJpeg(image, 0.7f);
            if (compressed.length <= maxBytes) {
                log.debug("Logo compressed to {}KB at {}x{}", compressed.length / 1024, width, height);
                return compressed;
            }
        }

        // Last resort: encode at lowest quality
        return encodeAsJpeg(image, 0.1f);
    }

    private byte[] encodeAsJpeg(BufferedImage image, float quality) throws Exception {
        // Ensure RGB (JPEG does not support alpha)
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setBackground(java.awt.Color.WHITE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IllegalStateException("No JPEG writer available");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(rgb, null, null), param);
        writer.dispose();
        return out.toByteArray();
    }

    private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setBackground(java.awt.Color.WHITE);
        g.clearRect(0, 0, targetWidth, targetHeight);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    @Transactional(readOnly = true)
    public JsonNode getSettings(Long tenantId) {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);
        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId).orElse(null);
        if (ts == null) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(ts.getSettingsJsonb());
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    @Transactional
    public void deleteTenant(Long id) {
        Long userId = TenantContext.getUserId();
        requireAdmin(userId);
        Tenant t = tenantRepository.findById(id).orElseThrow(() -> new RuntimeException("Tenant not found"));
        String schema = TenantSchema.fromTenantId(id);
        // Delete dependent admin-schema rows first
        tenantMembershipRepository.deleteAll(tenantMembershipRepository.findByTenantId(id));
        tenantSettingsRepository.findByTenantId(id).ifPresent(tenantSettingsRepository::delete);
        tenantSubscriptionRepository.findByTenantId(id).ifPresent(tenantSubscriptionRepository::delete);
        tenantRepository.delete(t);
        // Drop the tenant schema (all CRM data)
        if (!TenantSchema.PUBLIC_SCHEMA.equals(schema)) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            log.info("Dropped tenant schema: {}", schema);
        }
    }

    @Transactional(readOnly = true)
    public TenantDTO getCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return null;
        Tenant t = tenantRepository.findById(tenantId).orElse(null);
        if (t == null) return null;
        return toDTO(t);
    }

    /**
     * Returns the orgBio for the given tenant from settings_jsonb.
     * No admin check — used internally by the assistant to build system prompts.
     */
    @Transactional(readOnly = true)
    public String getOrgBio(Long tenantId) {
        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId).orElse(null);
        if (ts == null) return null;
        try {
            JsonNode node = objectMapper.readTree(ts.getSettingsJsonb());
            JsonNode bio = node.get("orgBio");
            return (bio != null && !bio.isNull() && !bio.asText().isBlank()) ? bio.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the logoUrl for the given tenant from settings_jsonb.
     * No admin check — used internally for document and email generation.
     */
    @Transactional(readOnly = true)
    public String getLogoUrl(Long tenantId) {
        TenantSettings ts = tenantSettingsRepository.findByTenantId(tenantId).orElse(null);
        if (ts == null) return null;
        try {
            JsonNode node = objectMapper.readTree(ts.getSettingsJsonb());
            JsonNode logo = node.get("logoUrl");
            return (logo != null && !logo.isNull() && !logo.asText().isBlank()) ? logo.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private TenantDTO toDTO(Tenant t) {
        TenantSettings ts = tenantSettingsRepository.findByTenantId(t.getId()).orElse(null);
        JsonNode settingsNode = null;
        try {
            settingsNode = ts != null ? objectMapper.readTree(ts.getSettingsJsonb()) : objectMapper.createObjectNode();
        } catch (Exception e) {
            settingsNode = objectMapper.createObjectNode();
        }
        return TenantDTO.builder()
                .id(t.getId())
                .name(t.getName())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .settings(settingsNode)
                .build();
    }
}

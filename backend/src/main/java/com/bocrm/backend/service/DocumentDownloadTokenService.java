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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues and verifies short-lived, HMAC-signed tokens for unauthenticated document downloads.
 * Token layout: "{tenantId}.{userId}.{docId}.{expEpochSec}.{sig}" where sig = base64url(HMAC-SHA256(payload, key)).
 * Key is derived from {@code app.encryption.secret} (the same 32-byte key used by {@link EncryptionService}).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class DocumentDownloadTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] key;

    public DocumentDownloadTokenService(@Value("${app.encryption.secret:}") String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("app.encryption.secret is not configured — required for signed download URLs.");
        }
        this.key = Base64.getDecoder().decode(secretBase64);
    }

    public String sign(long tenantId, long userId, long docId, Duration ttl) {
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String payload = tenantId + "." + userId + "." + docId + "." + exp;
        return payload + "." + B64.encodeToString(hmac(payload));
    }

    /**
     * @throws InvalidTokenException if signature mismatch, expired, malformed, or document id mismatch
     */
    public Payload verify(String token, long expectedDocId) {
        if (token == null || token.isBlank()) throw new InvalidTokenException("empty token");
        String[] parts = token.split("\\.");
        if (parts.length != 5) throw new InvalidTokenException("malformed token");

        String payload = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        byte[] given;
        try { given = B64D.decode(parts[4]); } catch (IllegalArgumentException e) { throw new InvalidTokenException("bad signature encoding"); }
        byte[] expected = hmac(payload);
        if (!MessageDigest.isEqual(given, expected)) throw new InvalidTokenException("signature mismatch");

        long tenantId, userId, docId, exp;
        try {
            tenantId = Long.parseLong(parts[0]);
            userId = Long.parseLong(parts[1]);
            docId = Long.parseLong(parts[2]);
            exp = Long.parseLong(parts[3]);
        } catch (NumberFormatException e) { throw new InvalidTokenException("bad numeric fields"); }

        if (docId != expectedDocId) throw new InvalidTokenException("token not valid for this document");
        if (Instant.now().getEpochSecond() > exp) throw new InvalidTokenException("token expired");

        return new Payload(tenantId, userId, docId, exp);
    }

    private byte[] hmac(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    public record Payload(long tenantId, long userId, long docId, long exp) {}

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String msg) { super(msg); }
    }
}

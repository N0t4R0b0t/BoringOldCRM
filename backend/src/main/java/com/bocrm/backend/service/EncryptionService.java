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

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for sensitive credentials.
 * Key is read from app.encryption.secret (base64-encoded 32 bytes).
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Service
@Slf4j
public class EncryptionService {
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 16 bytes
    private static final int GCM_IV_LENGTH = 96; // 12 bytes, recommended for GCM

    private final byte[] encryptionKey;

    public EncryptionService(@Value("${app.encryption.secret:}") String encryptionSecretBase64) {
        if (encryptionSecretBase64 == null || encryptionSecretBase64.isEmpty()) {
            throw new IllegalStateException("app.encryption.secret is not configured. Set to a base64-encoded 32-byte key.");
        }

        try {
            this.encryptionKey = Base64.getDecoder().decode(encryptionSecretBase64);
            if (this.encryptionKey.length != 32) {
                throw new IllegalStateException("Encryption key must be exactly 32 bytes (256 bits), got " + this.encryptionKey.length);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption.secret must be valid base64", e);
        }
    }

    /**
     * Encrypt plaintext using AES-256-GCM.
     * Returns a base64-encoded string: [base64(IV + ciphertext + tag)]
     */
    public String encrypt(String plaintext) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, 0, encryptionKey.length, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Concatenate IV + ciphertext (which includes the authentication tag)
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt base64-encoded ciphertext using AES-256-GCM.
     * Expected format: [base64(IV + ciphertext + tag)]
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // Remaining bytes are ciphertext + tag
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, 0, encryptionKey.length, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

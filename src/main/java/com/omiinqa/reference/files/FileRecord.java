package com.omiinqa.reference.files;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Immutable metadata record for a single uploaded file in the reference
 * {@link FileStorageService}. Holds the generated file ID, original name,
 * MIME type, byte length, SHA-256 hex checksum and the owner user ID so that
 * per-user quota calculations can be performed without scanning raw bytes.
 *
 * <p>The content bytes themselves are stored separately in the service so that
 * {@link #getSizeBytes()} can be queried without materialising the full payload.</p>
 */
@Data
@Builder
public class FileRecord {

    /** Globally unique file identifier (monotonically increasing). */
    private final String fileId;

    /** Original filename supplied by the caller at upload time. */
    private final String name;

    /** MIME type declared by the caller (validated against the allow-list). */
    private final String mimeType;

    /** File extension (lower-cased, without the leading dot). */
    private final String extension;

    /** Byte length of the file content. */
    private final long sizeBytes;

    /** SHA-256 hex digest (lower-case) of the file content at upload time. */
    private final String sha256Checksum;

    /** User/owner key; used for per-user quota enforcement. */
    private final String ownerId;

    /** Timestamp at which the file was accepted by the service. */
    @Builder.Default
    private final Instant uploadedAt = Instant.now();

    /** Version counter — incremented each time the file is replaced (versioning policy). */
    @Builder.Default
    private final int version = 1;
}

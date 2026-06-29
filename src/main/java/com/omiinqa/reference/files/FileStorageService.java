package com.omiinqa.reference.files;

import com.omiinqa.reference.core.DomainException;
import com.omiinqa.reference.core.Validations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory file-storage service with real, asserted business rules.
 *
 * <h2>Upload rules (enforced in order)</h2>
 * <ol>
 *   <li>Blank file name → {@code FILE_BLANK_NAME}</li>
 *   <li>Empty content (0 bytes) → {@code FILE_EMPTY}</li>
 *   <li>Extension / MIME type not in the allow-list → {@code FILE_BAD_TYPE}</li>
 *   <li>Content exceeds {@link #MAX_FILE_BYTES} → {@code FILE_TOO_LARGE}</li>
 *   <li>Per-owner quota ({@link #QUOTA_PER_USER_BYTES}) exceeded by this upload
 *       → {@code FILE_QUOTA_EXCEEDED}</li>
 *   <li>Duplicate name owned by the same user → {@code FILE_DUPLICATE}</li>
 * </ol>
 *
 * <p>A SHA-256 hex digest is computed deterministically from the supplied
 * {@code contentBytes} so every BDD assertion can verify the exact checksum
 * without randomness.</p>
 *
 * <h2>Versioning policy</h2>
 * <p>Duplicate-name detection raises {@code FILE_DUPLICATE} by default.
 * Callers that want to replace an existing file must call
 * {@link #replace(String, String, byte[], String)} which increments the
 * version counter and stores the new content under the same logical name.</p>
 *
 * <h2>Error codes (asserted by scenarios)</h2>
 * {@code FILE_BLANK_NAME}, {@code FILE_EMPTY}, {@code FILE_BAD_TYPE},
 * {@code FILE_TOO_LARGE}, {@code FILE_QUOTA_EXCEEDED}, {@code FILE_DUPLICATE},
 * {@code FILE_NOT_FOUND}.
 */
public class FileStorageService {

    // ------------------------------------------------------------------
    //  Constants
    // ------------------------------------------------------------------

    /** Maximum allowed upload size: 5 MiB. */
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    /** Per-user storage quota: 20 MiB. */
    public static final long QUOTA_PER_USER_BYTES = 20L * 1024 * 1024;

    /** Allowed lower-case file extensions. */
    public static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "png", "jpg", "csv", "txt", "docx");

    /** Allowed MIME types mapped to the extensions above. */
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/csv",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // ------------------------------------------------------------------
    //  Internal state
    // ------------------------------------------------------------------

    /** fileId → record metadata. */
    private final Map<String, FileRecord> records = new ConcurrentHashMap<>();

    /** fileId → raw bytes. */
    private final Map<String, byte[]> contents = new ConcurrentHashMap<>();

    /** ownerId → "name" → fileId  (for duplicate detection per user). */
    private final Map<String, Map<String, String>> ownerNameIndex = new ConcurrentHashMap<>();

    private final AtomicLong idSeq = new AtomicLong(1);

    // ------------------------------------------------------------------
    //  Public API — upload
    // ------------------------------------------------------------------

    /**
     * Upload a new file.
     *
     * @param name         original filename (must not be blank; must have allowed extension)
     * @param contentBytes file payload (must be non-empty and within size limit)
     * @param mimeType     declared MIME type (must be in the allow-list)
     * @param ownerId      owner / user identifier used for quota checks
     * @return the created {@link FileRecord}
     * @throws DomainException with a stable code on any rule violation
     */
    public FileRecord upload(final String name,
                             final byte[] contentBytes,
                             final String mimeType,
                             final String ownerId) {
        validateName(name);
        validateNotEmpty(contentBytes);
        validateType(name, mimeType);
        validateSize(contentBytes);
        validateQuota(ownerId, contentBytes.length);
        validateNoDuplicate(ownerId, name);

        final String fileId = String.valueOf(idSeq.getAndIncrement());
        final String checksum = sha256Hex(contentBytes);
        final String ext = extension(name);

        final FileRecord record = FileRecord.builder()
                .fileId(fileId)
                .name(name)
                .mimeType(mimeType)
                .extension(ext)
                .sizeBytes(contentBytes.length)
                .sha256Checksum(checksum)
                .ownerId(ownerId)
                .version(1)
                .build();

        records.put(fileId, record);
        contents.put(fileId, contentBytes.clone());
        ownerIndex(ownerId).put(name.toLowerCase(), fileId);
        return record;
    }

    /**
     * Replace an existing file (same name, same owner) with new content.
     * The version counter is incremented; all other validation rules still apply.
     *
     * @return updated {@link FileRecord}
     * @throws DomainException {@code FILE_NOT_FOUND} if no matching file exists;
     *         other codes if the new payload fails validation
     */
    public FileRecord replace(final String ownerId,
                              final String name,
                              final byte[] contentBytes,
                              final String mimeType) {
        validateName(name);
        validateNotEmpty(contentBytes);
        validateType(name, mimeType);
        validateSize(contentBytes);

        final Map<String, String> idx = ownerIndex(ownerId);
        final String fileId = idx.get(name.toLowerCase());
        if (fileId == null) {
            throw new DomainException("FILE_NOT_FOUND",
                    "No file named '" + name + "' found for owner: " + ownerId);
        }

        final FileRecord existing = records.get(fileId);
        final long sizeChange = (long) contentBytes.length - existing.getSizeBytes();
        if (sizeChange > 0) {
            validateQuota(ownerId, (int) sizeChange);
        }

        final String checksum = sha256Hex(contentBytes);
        final FileRecord updated = FileRecord.builder()
                .fileId(fileId)
                .name(name)
                .mimeType(mimeType)
                .extension(extension(name))
                .sizeBytes(contentBytes.length)
                .sha256Checksum(checksum)
                .ownerId(ownerId)
                .version(existing.getVersion() + 1)
                .build();

        records.put(fileId, updated);
        contents.put(fileId, contentBytes.clone());
        return updated;
    }

    // ------------------------------------------------------------------
    //  Public API — download / access
    // ------------------------------------------------------------------

    /**
     * Download the raw bytes for a file by its ID.
     *
     * @throws DomainException {@code FILE_NOT_FOUND} if the ID is unknown
     */
    public byte[] download(final String fileId) {
        if (!contents.containsKey(fileId)) {
            throw new DomainException("FILE_NOT_FOUND",
                    "File not found: " + fileId);
        }
        return contents.get(fileId).clone();
    }

    /**
     * Retrieve the metadata record for a file without fetching its content.
     *
     * @throws DomainException {@code FILE_NOT_FOUND} if the ID is unknown
     */
    public FileRecord getRecord(final String fileId) {
        return Optional.ofNullable(records.get(fileId))
                .orElseThrow(() -> new DomainException("FILE_NOT_FOUND",
                        "File not found: " + fileId));
    }

    /**
     * Find the file ID owned by {@code ownerId} with the given original name,
     * or empty if no such file exists.
     */
    public Optional<String> findFileId(final String ownerId, final String name) {
        return Optional.ofNullable(ownerIndex(ownerId).get(name.toLowerCase()));
    }

    // ------------------------------------------------------------------
    //  Public API — delete / list / stats
    // ------------------------------------------------------------------

    /**
     * Delete a file by ID.
     *
     * @throws DomainException {@code FILE_NOT_FOUND} if the ID is unknown
     */
    public void delete(final String fileId) {
        final FileRecord record = Optional.ofNullable(records.remove(fileId))
                .orElseThrow(() -> new DomainException("FILE_NOT_FOUND",
                        "File not found: " + fileId));
        contents.remove(fileId);
        ownerIndex(record.getOwnerId()).values().remove(fileId);
    }

    /** List all stored file records (snapshot). */
    public List<FileRecord> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(records.values()));
    }

    /** List all file records owned by {@code ownerId}. */
    public List<FileRecord> listByOwner(final String ownerId) {
        return records.values().stream()
                .filter(r -> r.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    /** Total bytes currently stored across all files. */
    public long totalStoredBytes() {
        return records.values().stream()
                .mapToLong(FileRecord::getSizeBytes)
                .sum();
    }

    /** Total bytes currently stored for a specific owner. */
    public long usedQuotaBytes(final String ownerId) {
        return listByOwner(ownerId).stream()
                .mapToLong(FileRecord::getSizeBytes)
                .sum();
    }

    /** Number of files currently stored. */
    public int fileCount() {
        return records.size();
    }

    // ------------------------------------------------------------------
    //  Internal helpers
    // ------------------------------------------------------------------

    private void validateName(final String name) {
        if (Validations.isBlank(name)) {
            throw new DomainException("FILE_BLANK_NAME",
                    "File name must not be blank");
        }
    }

    private void validateNotEmpty(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new DomainException("FILE_EMPTY",
                    "File content must not be empty");
        }
    }

    private void validateType(final String name, final String mimeType) {
        final String ext = extension(name);
        if (!ALLOWED_EXTENSIONS.contains(ext) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new DomainException("FILE_BAD_TYPE",
                    "File type not allowed: extension='" + ext
                            + "', mimeType='" + mimeType + "'");
        }
    }

    private void validateSize(final byte[] bytes) {
        if (bytes.length > MAX_FILE_BYTES) {
            throw new DomainException("FILE_TOO_LARGE",
                    "File size " + bytes.length + " bytes exceeds maximum "
                            + MAX_FILE_BYTES + " bytes");
        }
    }

    private void validateQuota(final String ownerId, final long additionalBytes) {
        final long used = usedQuotaBytes(ownerId);
        if (used + additionalBytes > QUOTA_PER_USER_BYTES) {
            throw new DomainException("FILE_QUOTA_EXCEEDED",
                    "Quota exceeded for owner '" + ownerId + "': used=" + used
                            + " bytes, requested=" + additionalBytes
                            + " bytes, limit=" + QUOTA_PER_USER_BYTES + " bytes");
        }
    }

    private void validateNoDuplicate(final String ownerId, final String name) {
        if (ownerIndex(ownerId).containsKey(name.toLowerCase())) {
            throw new DomainException("FILE_DUPLICATE",
                    "A file named '" + name + "' already exists for owner: " + ownerId
                            + ". Use replace() to overwrite.");
        }
    }

    private Map<String, String> ownerIndex(final String ownerId) {
        return ownerNameIndex.computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>());
    }

    /** Lower-case extension of {@code name}, without the dot. Returns "" if none. */
    static String extension(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    /**
     * Compute a SHA-256 hex digest of {@code data}.
     * Deterministic: same bytes always produce the same checksum.
     * Public so BDD step classes can verify downloaded bytes independently.
     */
    public static String sha256Hex(final byte[] data) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] hash = md.digest(data);
            final StringBuilder sb = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Build a deterministic byte array of exactly {@code size} bytes.
     * Each byte at index {@code i} is {@code (byte)(i % 251)} (251 is prime,
     * giving a visible cycling pattern with no randomness).
     *
     * @param size number of bytes to produce (must be &gt;= 0)
     * @return deterministic byte array
     */
    public static byte[] deterministicBytes(final int size) {
        final byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }

    /**
     * Build a deterministic byte array from a UTF-8 string.
     *
     * @param content source string
     * @return UTF-8 encoded bytes
     */
    public static byte[] fromString(final String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}

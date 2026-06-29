package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.files.FileRecord;
import com.omiinqa.reference.files.FileStorageService;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference <em>file-download</em> domain.
 *
 * <p>Reuses the {@link FileStorageService} instance shared via
 * {@link DomainWorld} (the {@code "files.service"} key) that is created by
 * {@code FileUploadSteps}'s Background step. Download operations are captured
 * through {@link DomainWorld#capture} so that shared assertions
 * ("a domain error X is raised", "the operation succeeds") work out of the box.</p>
 *
 * <p>Step text is prefixed with the noun {@code "file download"} / {@code "downloaded file"}
 * to stay globally unique across all domain step classes.</p>
 */
public class FileDownloadSteps {

    // ------------------------------------------------------------------
    //  Keys (shared with FileUploadSteps where appropriate)
    // ------------------------------------------------------------------
    private static final String SVC             = "files.service";
    private static final String LAST_RECORD     = "files.lastRecord";
    private static final String DOWNLOADED_BYTES = "files.downloadedBytes";

    // ------------------------------------------------------------------
    //  Service accessor
    // ------------------------------------------------------------------

    private FileStorageService service() {
        return DomainWorld.service(SVC, FileStorageService::new);
    }

    // ------------------------------------------------------------------
    //  When — actions
    // ------------------------------------------------------------------

    /** Download the file whose ID was stored by the most recent upload step. */
    @When("I download the last uploaded file")
    public void downloadLastUploadedFile() {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        DomainWorld.capture(() -> {
            final byte[] bytes = service().download(rec.getFileId());
            DomainWorld.put(DOWNLOADED_BYTES, bytes);
            return bytes;
        });
    }

    /** Download a file by an explicit file ID (covers not-found scenarios). */
    @When("I download file with id {string}")
    public void downloadFileById(final String fileId) {
        DomainWorld.capture(() -> {
            final byte[] bytes = service().download(fileId);
            DomainWorld.put(DOWNLOADED_BYTES, bytes);
            return bytes;
        });
    }

    /**
     * Download the file whose ID is resolved by looking up owner + name in the
     * service index. Raises FILE_NOT_FOUND if no match.
     */
    @When("I download file {string} owned by {string}")
    public void downloadFileByName(final String name, final String owner) {
        DomainWorld.capture(() -> {
            final String fileId = service().findFileId(owner, name)
                    .orElseThrow(() -> new com.omiinqa.reference.core.DomainException(
                            "FILE_NOT_FOUND",
                            "No file named '" + name + "' found for owner: " + owner));
            final byte[] bytes = service().download(fileId);
            DomainWorld.put(DOWNLOADED_BYTES, bytes);
            return bytes;
        });
    }

    /** Retrieve the metadata record for a file (no bytes transferred). */
    @When("I retrieve metadata for file {string} owned by {string}")
    public void retrieveFileMetadata(final String name, final String owner) {
        DomainWorld.capture(() -> {
            final String fileId = service().findFileId(owner, name)
                    .orElseThrow(() -> new com.omiinqa.reference.core.DomainException(
                            "FILE_NOT_FOUND",
                            "No file named '" + name + "' found for owner: " + owner));
            final FileRecord record = service().getRecord(fileId);
            DomainWorld.put(LAST_RECORD, record);
            return record;
        });
    }

    /** Retrieve the list of all files for a given owner. */
    @When("I list files for owner {string}")
    public void listFilesForOwner(final String owner) {
        DomainWorld.capture(() -> {
            final List<FileRecord> files = service().listByOwner(owner);
            DomainWorld.put("files.ownerList", files);
            return files;
        });
    }

    // ------------------------------------------------------------------
    //  Then — assertions on downloaded bytes
    // ------------------------------------------------------------------

    /** The downloaded content has exactly {@code expected} bytes. */
    @Then("the downloaded file has {int} bytes")
    public void downloadedFileHasBytes(final int expected) {
        final byte[] bytes = DomainWorld.get(DOWNLOADED_BYTES);
        assertThat(bytes).as("downloaded bytes must not be null").isNotNull();
        assertThat(bytes.length).as("downloaded byte count").isEqualTo(expected);
    }

    /**
     * The SHA-256 checksum of the downloaded bytes matches the expected value
     * (allows cross-verifying that round-trip fidelity is preserved).
     */
    @Then("the downloaded file checksum matches {string}")
    public void downloadedChecksumMatches(final String expected) {
        final byte[] bytes = DomainWorld.get(DOWNLOADED_BYTES);
        assertThat(bytes).isNotNull();
        final String actual = FileStorageService.sha256Hex(bytes);
        assertThat(actual)
                .as("SHA-256 of downloaded bytes")
                .isEqualToIgnoringCase(expected);
    }

    /**
     * The SHA-256 of the downloaded bytes matches the checksum that was recorded
     * in the last uploaded {@link FileRecord} — verifying round-trip integrity.
     */
    @Then("the downloaded file checksum matches the uploaded record checksum")
    public void downloadedChecksumMatchesRecord() {
        final byte[] bytes = DomainWorld.get(DOWNLOADED_BYTES);
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(bytes).isNotNull();
        assertThat(rec).isNotNull();
        final String actualChecksum = FileStorageService.sha256Hex(bytes);
        assertThat(actualChecksum)
                .as("downloaded checksum vs stored record checksum")
                .isEqualToIgnoringCase(rec.getSha256Checksum());
    }

    /** The downloaded bytes start with the given UTF-8 prefix string. */
    @Then("the downloaded file content starts with {string}")
    public void downloadedContentStartsWith(final String prefix) {
        final byte[] bytes = DomainWorld.get(DOWNLOADED_BYTES);
        assertThat(bytes).isNotNull();
        final String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(content)
                .as("downloaded content prefix")
                .startsWith(prefix);
    }

    /** The downloaded bytes are an exact match of a deterministic array of given size. */
    @Then("the downloaded file matches deterministic content of {int} bytes")
    public void downloadedMatchesDeterministicBytes(final int size) {
        final byte[] bytes = DomainWorld.get(DOWNLOADED_BYTES);
        assertThat(bytes).isNotNull();
        final byte[] expected = FileStorageService.deterministicBytes(size);
        assertThat(bytes)
                .as("downloaded bytes must exactly match deterministic content of %d bytes", size)
                .isEqualTo(expected);
    }

    // ------------------------------------------------------------------
    //  Then — assertions on metadata / listing
    // ------------------------------------------------------------------

    /** The metadata record for the last retrieved file has the expected MIME type. */
    @Then("the file metadata mime type is {string}")
    public void fileMetadataMimeTypeIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).as("file record must not be null").isNotNull();
        assertThat(rec.getMimeType()).as("metadata mime type").isEqualTo(expected);
    }

    /** The metadata record has the expected size in bytes. */
    @Then("the file metadata size is {int} bytes")
    public void fileMetadataSizeIs(final int expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getSizeBytes()).as("metadata size bytes").isEqualTo(expected);
    }

    /** The metadata record's checksum matches the expected hex string. */
    @Then("the file metadata checksum is {string}")
    public void fileMetadataChecksumIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getSha256Checksum())
                .as("metadata checksum")
                .isEqualToIgnoringCase(expected);
    }

    /** The listing for the last queried owner contains exactly {@code expected} entries. */
    @Then("the file listing for owner contains {int} files")
    public void fileListingContains(final int expected) {
        final List<FileRecord> files = DomainWorld.get("files.ownerList");
        assertThat(files).as("owner file listing").isNotNull().hasSize(expected);
    }

    /** The listing contains a file with the given name. */
    @Then("the file listing includes {string}")
    public void fileListingIncludes(final String name) {
        final List<FileRecord> files = DomainWorld.get("files.ownerList");
        assertThat(files).isNotNull();
        assertThat(files)
                .as("listing should contain a file named '%s'", name)
                .anyMatch(r -> r.getName().equals(name));
    }

    /** The listing does not contain a file with the given name. */
    @Then("the file listing does not include {string}")
    public void fileListingDoesNotInclude(final String name) {
        final List<FileRecord> files = DomainWorld.get("files.ownerList");
        assertThat(files).isNotNull();
        assertThat(files)
                .as("listing should not contain a file named '%s'", name)
                .noneMatch(r -> r.getName().equals(name));
    }
}

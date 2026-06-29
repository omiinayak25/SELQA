package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.files.FileRecord;
import com.omiinqa.reference.files.FileStorageService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the reference <em>file-upload</em> domain.
 *
 * <p>All mutating operations are funnelled through {@link DomainWorld#run} or
 * {@link DomainWorld#capture} so that the shared {@code CommonDomainSteps}
 * assertions ("a domain error X is raised", "the operation succeeds") work
 * without any additional wiring. The service is lazily created via
 * {@link DomainWorld#service} so every BDD scenario starts with a clean,
 * isolated instance.</p>
 *
 * <p>Step text is prefixed with the noun {@code "file upload"} / {@code "file"}
 * to avoid ambiguous step definitions across domains.</p>
 */
public class FileUploadSteps {

    // ------------------------------------------------------------------
    //  Keys used to share state through DomainWorld / ScenarioContext
    // ------------------------------------------------------------------
    private static final String SVC         = "files.service";
    private static final String LAST_RECORD = "files.lastRecord";

    // ------------------------------------------------------------------
    //  Service accessor
    // ------------------------------------------------------------------

    private FileStorageService service() {
        return DomainWorld.service(SVC, FileStorageService::new);
    }

    // ------------------------------------------------------------------
    //  Given — state setup
    // ------------------------------------------------------------------

    /** Reset the file storage service to an empty state for this scenario. */
    @Given("a clean file storage service")
    public void cleanFileStorageService() {
        DomainWorld.put(SVC, new FileStorageService());
    }

    /**
     * Pre-seed an already-uploaded file without going through the
     * error-capture path (used as background / Given state).
     */
    @Given("file {string} with mime {string} and {int} bytes has been uploaded by owner {string}")
    public void fileAlreadyUploaded(final String name,
                                    final String mime,
                                    final int size,
                                    final String owner) {
        final byte[] bytes = FileStorageService.deterministicBytes(size);
        final FileRecord record = service().upload(name, bytes, mime, owner);
        DomainWorld.put(LAST_RECORD, record);
    }

    /** Pre-seed a file from a plain-text string payload. */
    @Given("file {string} with mime {string} containing text {string} has been uploaded by owner {string}")
    public void fileFromTextAlreadyUploaded(final String name,
                                            final String mime,
                                            final String text,
                                            final String owner) {
        final byte[] bytes = FileStorageService.fromString(text);
        final FileRecord record = service().upload(name, bytes, mime, owner);
        DomainWorld.put(LAST_RECORD, record);
    }

    // ------------------------------------------------------------------
    //  When — actions
    // ------------------------------------------------------------------

    /** Upload a deterministic byte array of {@code size} bytes. */
    @When("I upload file {string} with mime {string} and {int} bytes as owner {string}")
    public void uploadFileBytesAsOwner(final String name,
                                       final String mime,
                                       final int size,
                                       final String owner) {
        final byte[] bytes = FileStorageService.deterministicBytes(size);
        DomainWorld.run(() ->
                DomainWorld.put(LAST_RECORD, service().upload(name, bytes, mime, owner)));
    }

    /** Upload a file whose content is derived from a plain-text string. */
    @When("I upload file {string} with mime {string} containing text {string} as owner {string}")
    public void uploadFileTextAsOwner(final String name,
                                      final String mime,
                                      final String text,
                                      final String owner) {
        final byte[] bytes = FileStorageService.fromString(text);
        DomainWorld.run(() ->
                DomainWorld.put(LAST_RECORD, service().upload(name, bytes, mime, owner)));
    }

    /** Upload an empty file (0 bytes) — expected to raise FILE_EMPTY. */
    @When("I upload file {string} with mime {string} and empty content as owner {string}")
    public void uploadEmptyFileAsOwner(final String name,
                                       final String mime,
                                       final String owner) {
        DomainWorld.run(() ->
                DomainWorld.put(LAST_RECORD,
                        service().upload(name, new byte[0], mime, owner)));
    }

    /** Upload a file with a blank name (empty string). */
    @When("I upload a file with blank name and mime {string} and {int} bytes as owner {string}")
    public void uploadBlankNameFile(final String mime,
                                    final int size,
                                    final String owner) {
        final byte[] bytes = FileStorageService.deterministicBytes(size);
        DomainWorld.run(() ->
                DomainWorld.put(LAST_RECORD, service().upload("", bytes, mime, owner)));
    }

    /** Delete a file by its stored file ID (taken from the last uploaded record). */
    @When("I delete the last uploaded file")
    public void deleteLastUploadedFile() {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        DomainWorld.run(() -> service().delete(rec.getFileId()));
    }

    /** Delete a file by explicit ID. */
    @When("I delete file with id {string}")
    public void deleteFileById(final String fileId) {
        DomainWorld.run(() -> service().delete(fileId));
    }

    /** Replace the named file owned by {@code owner} with new deterministic bytes. */
    @When("I replace file {string} owned by {string} with {int} bytes and mime {string}")
    public void replaceFile(final String name,
                            final String owner,
                            final int size,
                            final String mime) {
        final byte[] bytes = FileStorageService.deterministicBytes(size);
        DomainWorld.run(() ->
                DomainWorld.put(LAST_RECORD,
                        service().replace(owner, name, bytes, mime)));
    }

    // ------------------------------------------------------------------
    //  Then — assertions
    // ------------------------------------------------------------------

    /** The last uploaded record exists and has the expected file name. */
    @Then("the uploaded file name is {string}")
    public void uploadedFileNameIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).as("last record must not be null").isNotNull();
        assertThat(rec.getName()).as("file name").isEqualTo(expected);
    }

    /** The last uploaded record has the expected MIME type. */
    @Then("the uploaded file mime type is {string}")
    public void uploadedFileMimeTypeIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getMimeType()).as("mime type").isEqualTo(expected);
    }

    /** The last uploaded record has exactly {@code expected} bytes. */
    @Then("the uploaded file size is {int} bytes")
    public void uploadedFileSizeIs(final int expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getSizeBytes()).as("file size in bytes").isEqualTo(expected);
    }

    /** The last uploaded record has a non-blank SHA-256 checksum of 64 hex chars. */
    @Then("the uploaded file has a valid SHA-256 checksum")
    public void uploadedFileHasValidChecksum() {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getSha256Checksum())
                .as("checksum must be 64 lowercase hex chars")
                .matches("[0-9a-f]{64}");
    }

    /** The last uploaded record has a specific exact SHA-256 checksum. */
    @Then("the uploaded file checksum is {string}")
    public void uploadedFileChecksumIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getSha256Checksum())
                .as("SHA-256 checksum")
                .isEqualToIgnoringCase(expected);
    }

    /** The last uploaded record has a non-blank generated file ID. */
    @Then("the uploaded file has a non-blank file id")
    public void uploadedFileHasNonBlankId() {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getFileId()).as("file id").isNotBlank();
    }

    /** The service now holds exactly {@code expected} files. */
    @Then("the file storage contains {int} files")
    public void fileStorageContainsCount(final int expected) {
        assertThat(service().fileCount())
                .as("total file count")
                .isEqualTo(expected);
    }

    /** The total bytes stored across all files equals {@code expected}. */
    @Then("the total stored size is {int} bytes")
    public void totalStoredSizeIs(final int expected) {
        assertThat(service().totalStoredBytes())
                .as("total stored bytes")
                .isEqualTo(expected);
    }

    /** The used quota for {@code owner} equals {@code expected} bytes. */
    @Then("owner {string} has used {int} bytes of quota")
    public void ownerUsedQuotaIs(final String owner, final int expected) {
        assertThat(service().usedQuotaBytes(owner))
                .as("used quota bytes for owner " + owner)
                .isEqualTo(expected);
    }

    /** Owner has exactly {@code expected} files. */
    @Then("owner {string} has {int} files stored")
    public void ownerHasFilesStored(final String owner, final int expected) {
        final List<FileRecord> files = service().listByOwner(owner);
        assertThat(files).as("files for owner " + owner).hasSize(expected);
    }

    /** The last replaced file has a version number of {@code expected}. */
    @Then("the replaced file has version {int}")
    public void replacedFileHasVersion(final int expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getVersion()).as("file version").isEqualTo(expected);
    }

    /** The last uploaded record has the expected extension. */
    @Then("the uploaded file extension is {string}")
    public void uploadedFileExtensionIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getExtension()).as("file extension").isEqualTo(expected);
    }

    /** The last uploaded record has the expected owner. */
    @Then("the uploaded file owner is {string}")
    public void uploadedFileOwnerIs(final String expected) {
        final FileRecord rec = DomainWorld.get(LAST_RECORD);
        assertThat(rec).isNotNull();
        assertThat(rec.getOwnerId()).as("file owner").isEqualTo(expected);
    }
}

@domain @files @file-download
Feature: File Download
  As the file storage platform
  I want to allow retrieval of uploaded files by ID or name+owner lookup
  So that callers receive byte-exact content with verifiable checksums and correct metadata

  Background:
    Given a clean file storage service

  # ------------------------------------------------------------------ #
  #  Positive — happy-path downloads                                     #
  # ------------------------------------------------------------------ #

  @smoke @positive
  Scenario: Downloading a previously uploaded file returns its exact bytes
    Given file "report.pdf" with mime "application/pdf" and 1024 bytes has been uploaded by owner "user1"
    When I download the last uploaded file
    Then the operation succeeds
    And the downloaded file has 1024 bytes

  @positive
  Scenario: Downloaded bytes exactly match the deterministic content used at upload
    Given file "data.csv" with mime "text/csv" and 200 bytes has been uploaded by owner "user1"
    When I download the last uploaded file
    Then the operation succeeds
    And the downloaded file matches deterministic content of 200 bytes

  @positive
  Scenario: Downloaded file checksum matches the checksum stored in the upload record
    Given file "notes.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "user1"
    When I download the last uploaded file
    Then the operation succeeds
    And the downloaded file checksum matches the uploaded record checksum

  @positive
  Scenario: Downloading by name and owner returns the correct bytes
    Given file "notes.txt" with mime "text/plain" containing text "Hello World" has been uploaded by owner "alice"
    When I download file "notes.txt" owned by "alice"
    Then the operation succeeds
    And the downloaded file has 11 bytes
    And the downloaded file checksum matches "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"

  @positive
  Scenario: Downloaded text-based file content matches original string payload
    Given file "greeting.txt" with mime "text/plain" containing text "Hello World" has been uploaded by owner "user1"
    When I download file "greeting.txt" owned by "user1"
    Then the operation succeeds
    And the downloaded file content starts with "Hello World"

  @positive @business
  Scenario: Files from different owners with the same name are returned independently
    Given file "shared.txt" with mime "text/plain" containing text "Hello World" has been uploaded by owner "alice"
    And file "shared.txt" with mime "text/plain" containing text "sample csv data" has been uploaded by owner "bob"
    When I download file "shared.txt" owned by "alice"
    Then the operation succeeds
    And the downloaded file has 11 bytes
    And the downloaded file checksum matches "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"

  @positive @business
  Scenario: Downloading a replaced file returns the new content not the original
    Given file "draft.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "user1"
    When I replace file "draft.txt" owned by "user1" with 200 bytes and mime "text/plain"
    And I download file "draft.txt" owned by "user1"
    Then the operation succeeds
    And the downloaded file has 200 bytes
    And the downloaded file matches deterministic content of 200 bytes

  @positive
  Scenario: Retrieving file metadata returns the correct MIME type and size without downloading bytes
    Given file "photo.png" with mime "image/png" and 1024 bytes has been uploaded by owner "user1"
    When I retrieve metadata for file "photo.png" owned by "user1"
    Then the operation succeeds
    And the file metadata mime type is "image/png"
    And the file metadata size is 1024 bytes
    And the file metadata checksum is "2bce1ba628720664be4b9fdd77aae0678e5f0f3f02fc6ff641ec879094f6a404"

  @positive
  Scenario: Listing files for an owner returns only that owner's files
    Given file "a.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "alice"
    And file "b.csv" with mime "text/csv" and 200 bytes has been uploaded by owner "alice"
    And file "c.pdf" with mime "application/pdf" and 300 bytes has been uploaded by owner "bob"
    When I list files for owner "alice"
    Then the operation succeeds
    And the file listing for owner contains 2 files
    And the file listing includes "a.txt"
    And the file listing includes "b.csv"
    And the file listing does not include "c.pdf"

  @positive
  Scenario: Listing files for an owner with no uploads returns an empty list
    When I list files for owner "empty-owner"
    Then the operation succeeds
    And the file listing for owner contains 0 files

  @positive
  Scenario: After deletion the file listing no longer contains the deleted file
    Given file "to-delete.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "user1"
    And file "to-keep.csv" with mime "text/csv" and 200 bytes has been uploaded by owner "user1"
    When I delete the last uploaded file
    And I list files for owner "user1"
    Then the operation succeeds
    And the file listing for owner contains 1 files
    And the file listing includes "to-delete.txt"

  # ------------------------------------------------------------------ #
  #  Negative — FILE_NOT_FOUND                                           #
  # ------------------------------------------------------------------ #

  @negative
  Scenario: Downloading a non-existent file ID raises FILE_NOT_FOUND
    When I download file with id "nonexistent-id-999"
    Then a domain error "FILE_NOT_FOUND" is raised
    And the domain error message contains "nonexistent-id-999"

  @negative
  Scenario: Downloading a file by name when no file with that name exists for the owner raises FILE_NOT_FOUND
    Given file "other.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "alice"
    When I download file "missing.txt" owned by "alice"
    Then a domain error "FILE_NOT_FOUND" is raised
    And the domain error message contains "missing.txt"

  @negative
  Scenario: Retrieving metadata for a non-existent name raises FILE_NOT_FOUND
    When I retrieve metadata for file "ghost.pdf" owned by "user1"
    Then a domain error "FILE_NOT_FOUND" is raised

  @negative
  Scenario: Downloading a file by name under a different owner raises FILE_NOT_FOUND
    Given file "private.pdf" with mime "application/pdf" and 100 bytes has been uploaded by owner "alice"
    When I download file "private.pdf" owned by "bob"
    Then a domain error "FILE_NOT_FOUND" is raised
    And the domain error message contains "bob"

  @negative
  Scenario: Deleting an already-deleted file raises FILE_NOT_FOUND
    Given file "once.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "user1"
    When I delete the last uploaded file
    And I delete file with id "1"
    Then a domain error "FILE_NOT_FOUND" is raised

  # ------------------------------------------------------------------ #
  #  Scenario Outline — round-trip fidelity across allowed file types   #
  # ------------------------------------------------------------------ #

  @positive @boundary @regression
  Scenario Outline: Download round-trip preserves exact byte count and checksum for each allowed type
    Given file <filename> with mime <mime> and <size> bytes has been uploaded by owner "roundtrip-user"
    When I download file <filename> owned by "roundtrip-user"
    Then the operation succeeds
    And the downloaded file has <size> bytes
    And the downloaded file checksum matches the uploaded record checksum

    Examples:
      | filename       | mime                                                                        | size |
      | "r.pdf"        | "application/pdf"                                                           | 512  |
      | "r.png"        | "image/png"                                                                 | 1024 |
      | "r.jpg"        | "image/jpeg"                                                                | 2048 |
      | "r.csv"        | "text/csv"                                                                  | 256  |
      | "r.txt"        | "text/plain"                                                                | 128  |
      | "r.docx"       | "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  | 4096 |

  # ------------------------------------------------------------------ #
  #  Scenario Outline — size boundaries at/near the 5 MB limit          #
  # ------------------------------------------------------------------ #

  @positive @boundary @sanity
  Scenario Outline: Downloading a file stored at a boundary size returns exact byte count
    Given file "boundary.txt" with mime "text/plain" and <size> bytes has been uploaded by owner "bnd-user"
    When I download the last uploaded file
    Then the operation succeeds
    And the downloaded file has <size> bytes
    And the downloaded file checksum matches the uploaded record checksum

    Examples:
      | size    |
      | 1       |
      | 1024    |
      | 1000000 |
      | 5242880 |

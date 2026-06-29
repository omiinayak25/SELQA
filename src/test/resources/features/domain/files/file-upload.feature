@domain @files @file-upload
Feature: File Upload
  As the file storage platform
  I want to enforce upload rules on file type, size, emptiness, name and per-user quota
  So that only safe, correctly bounded files are accepted and stored with verified checksums

  Background:
    Given a clean file storage service

  # ------------------------------------------------------------------ #
  #  Positive — happy-path uploads                                       #
  # ------------------------------------------------------------------ #

  @smoke @positive
  Scenario: Uploading a valid PDF succeeds and returns a non-blank file ID
    When I upload file "report.pdf" with mime "application/pdf" and 1024 bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file has a non-blank file id
    And the uploaded file name is "report.pdf"
    And the uploaded file extension is "pdf"
    And the uploaded file mime type is "application/pdf"

  @positive
  Scenario: Uploaded file size is recorded accurately
    When I upload file "data.csv" with mime "text/csv" and 100 bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file size is 100 bytes

  @positive
  Scenario: SHA-256 checksum is computed and stored correctly for known content
    When I upload file "data.csv" with mime "text/csv" and 100 bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file checksum is "bce0aff19cf5aa6a7469a30d61d04e4376e4bbf6381052ee9e7f33925c954d52"

  @positive
  Scenario: Uploading a plain-text file produces the correct checksum for string content
    When I upload file "hello.txt" with mime "text/plain" containing text "Hello World" as owner "user1"
    Then the operation succeeds
    And the uploaded file size is 11 bytes
    And the uploaded file checksum is "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"

  @positive
  Scenario: Uploading a file at exactly the maximum allowed size succeeds
    When I upload file "big.pdf" with mime "application/pdf" and 5242880 bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file size is 5242880 bytes
    And the uploaded file checksum is "16b632f11cf950dda67dc4c184a3f9e0aa1ffa4c18927bb8977e7da97ca25bca"

  @positive
  Scenario: Multiple distinct files by the same owner are all stored
    When I upload file "a.txt" with mime "text/plain" and 100 bytes as owner "user1"
    And I upload file "b.csv" with mime "text/csv" and 200 bytes as owner "user1"
    Then the operation succeeds
    And the file storage contains 2 files
    And owner "user1" has 2 files stored

  @positive
  Scenario: Different owners can upload files with the same name without conflict
    When I upload file "notes.txt" with mime "text/plain" and 100 bytes as owner "alice"
    And I upload file "notes.txt" with mime "text/plain" and 200 bytes as owner "bob"
    Then the operation succeeds
    And the file storage contains 2 files

  @positive
  Scenario: Total stored size is the sum of all uploaded file sizes
    When I upload file "a.csv" with mime "text/csv" and 100 bytes as owner "user1"
    And I upload file "b.csv" with mime "text/csv" and 200 bytes as owner "user1"
    Then the operation succeeds
    And the total stored size is 300 bytes

  @positive
  Scenario: Deleting an uploaded file removes it from storage
    Given file "temp.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "user1"
    When I delete the last uploaded file
    Then the operation succeeds
    And the file storage contains 0 files

  @positive @business
  Scenario: Replacing an existing file increments the version counter
    Given file "draft.txt" with mime "text/plain" and 100 bytes has been uploaded by owner "user1"
    When I replace file "draft.txt" owned by "user1" with 200 bytes and mime "text/plain"
    Then the operation succeeds
    And the replaced file has version 2
    And the uploaded file size is 200 bytes

  @positive
  Scenario: Uploading the minimum valid file (one byte) is accepted
    When I upload file "tiny.txt" with mime "text/plain" and 1 bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file size is 1 bytes
    And the uploaded file checksum is "6e340b9cffb37a989ca544e6bb780a2c78901d3fb33738768511a30617afa01d"

  @positive
  Scenario: Uploaded file owner is stored correctly on the record
    When I upload file "doc.docx" with mime "application/vnd.openxmlformats-officedocument.wordprocessingml.document" and 1024 bytes as owner "carol"
    Then the operation succeeds
    And the uploaded file owner is "carol"

  # ------------------------------------------------------------------ #
  #  Scenario Outline — allowed extension / MIME type whitelist         #
  # ------------------------------------------------------------------ #

  @positive @boundary @validation
  Scenario Outline: Uploading a file with an allowed extension and matching MIME type succeeds
    When I upload file <filename> with mime <mime> and 1024 bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file has a valid SHA-256 checksum

    Examples:
      | filename       | mime                                                                                   |
      | "report.pdf"   | "application/pdf"                                                                      |
      | "image.png"    | "image/png"                                                                            |
      | "photo.jpg"    | "image/jpeg"                                                                           |
      | "export.csv"   | "text/csv"                                                                             |
      | "readme.txt"   | "text/plain"                                                                           |
      | "doc.docx"     | "application/vnd.openxmlformats-officedocument.wordprocessingml.document"              |

  # ------------------------------------------------------------------ #
  #  Scenario Outline — size boundary cases                             #
  # ------------------------------------------------------------------ #

  @positive @boundary
  Scenario Outline: Uploading at or below the size limit succeeds
    When I upload file "file.txt" with mime "text/plain" and <size> bytes as owner "user1"
    Then the operation succeeds
    And the uploaded file size is <size> bytes

    Examples:
      | size    |
      | 1       |
      | 1024    |
      | 1000000 |
      | 5242880 |

  # ------------------------------------------------------------------ #
  #  Negative — validation errors                                        #
  # ------------------------------------------------------------------ #

  @negative @validation
  Scenario: Uploading with a blank file name raises FILE_BLANK_NAME
    When I upload a file with blank name and mime "text/plain" and 100 bytes as owner "user1"
    Then a domain error "FILE_BLANK_NAME" is raised

  @negative @validation
  Scenario: Uploading empty content raises FILE_EMPTY
    When I upload file "empty.txt" with mime "text/plain" and empty content as owner "user1"
    Then a domain error "FILE_EMPTY" is raised

  @negative @boundary
  Scenario: Uploading a file one byte over the size limit raises FILE_TOO_LARGE
    When I upload file "toobig.pdf" with mime "application/pdf" and 5242881 bytes as owner "user1"
    Then a domain error "FILE_TOO_LARGE" is raised
    And the domain error message contains "5242881"

  @negative @validation
  Scenario: Uploading a duplicate file name for the same owner raises FILE_DUPLICATE
    Given file "report.pdf" with mime "application/pdf" and 100 bytes has been uploaded by owner "user1"
    When I upload file "report.pdf" with mime "application/pdf" and 200 bytes as owner "user1"
    Then a domain error "FILE_DUPLICATE" is raised
    And the domain error message contains "report.pdf"

  # ------------------------------------------------------------------ #
  #  Scenario Outline — disallowed extension / MIME type raises FILE_BAD_TYPE #
  # ------------------------------------------------------------------ #

  @negative @validation
  Scenario Outline: Uploading a file with a disallowed extension or MIME type raises FILE_BAD_TYPE
    When I upload file <filename> with mime <mime> and 1024 bytes as owner "user1"
    Then a domain error "FILE_BAD_TYPE" is raised

    Examples:
      | filename        | mime                    |
      | "script.js"     | "application/javascript"|
      | "archive.zip"   | "application/zip"       |
      | "virus.exe"     | "application/x-msdownload" |
      | "page.html"     | "text/html"             |
      | "shell.sh"      | "application/x-sh"      |
      | "report.pdf"    | "application/zip"       |

  # ------------------------------------------------------------------ #
  #  Negative — quota enforcement                                        #
  # ------------------------------------------------------------------ #

  @negative @boundary @business
  Scenario: Uploading beyond the per-user quota raises FILE_QUOTA_EXCEEDED
    Given file "f1.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-user"
    And file "f2.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-user"
    And file "f3.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-user"
    And file "f4.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-user"
    When I upload file "f5.pdf" with mime "application/pdf" and 1 bytes as owner "quota-user"
    Then a domain error "FILE_QUOTA_EXCEEDED" is raised
    And the domain error message contains "quota-user"

  @positive @boundary @business
  Scenario: Uploading up to the exact quota limit succeeds
    Given file "q1.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "exact-quota-user"
    And file "q2.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "exact-quota-user"
    And file "q3.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "exact-quota-user"
    When I upload file "q4.pdf" with mime "application/pdf" and 5242880 bytes as owner "exact-quota-user"
    Then the operation succeeds
    And owner "exact-quota-user" has used 20971520 bytes of quota

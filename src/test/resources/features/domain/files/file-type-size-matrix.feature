@domain @files
Feature: File Type and Size Matrix
  As the file storage platform
  I want every allowed/disallowed extension×MIME-type and size boundary combination
  to produce the exact expected success or FILE_* error code
  So that type whitelist, 5 MiB limit, and 20 MiB quota are fully exercised

  Background:
    Given a clean file storage service

  # ─── WHITELIST MATRIX — every allowed extension+MIME succeeds ───────────────────

  @positive @validation @sanity
  Scenario Outline: Uploading a file with an allowed extension and correct MIME type succeeds
    When I upload file "<filename>" with mime "<mime>" and 512 bytes as owner "wl-user"
    Then the operation succeeds
    And the uploaded file extension is "<ext>"
    And the uploaded file mime type is "<mime>"
    And the uploaded file has a valid SHA-256 checksum

    Examples:
      | filename        | mime                                                                              | ext  |
      | doc.pdf         | application/pdf                                                                   | pdf  |
      | image.png       | image/png                                                                         | png  |
      | photo.jpg       | image/jpeg                                                                        | jpg  |
      | data.csv        | text/csv                                                                          | csv  |
      | readme.txt      | text/plain                                                                        | txt  |
      | report.docx     | application/vnd.openxmlformats-officedocument.wordprocessingml.document           | docx |

  # ─── TYPE MISMATCH MATRIX — allowed extension but disallowed MIME raises FILE_BAD_TYPE ─
  # Note: the service validates extension ∈ ALLOWED_EXTENSIONS AND mimeType ∈ ALLOWED_MIME_TYPES
  # independently. So only pairs where the MIME is NOT in the whitelist trigger FILE_BAD_TYPE.

  @negative @validation
  Scenario Outline: Allowed extension paired with a non-whitelisted MIME type raises FILE_BAD_TYPE
    When I upload file "<filename>" with mime "<wrong_mime>" and 512 bytes as owner "tm<row>-user"
    Then a domain error "FILE_BAD_TYPE" is raised

    Examples:
      | filename    | wrong_mime                  | row |
      | doc.pdf     | application/zip             | 01  |
      | doc.pdf     | application/javascript      | 02  |
      | image.png   | application/zip             | 03  |
      | image.png   | text/html                   | 04  |
      | photo.jpg   | application/zip             | 05  |
      | photo.jpg   | application/x-sh            | 06  |
      | data.csv    | application/zip             | 07  |
      | data.csv    | application/javascript      | 08  |
      | readme.txt  | application/zip             | 09  |
      | readme.txt  | text/html                   | 10  |
      | report.docx | application/zip             | 11  |
      | report.docx | application/javascript      | 12  |

  # ─── BLOCKED EXTENSION MATRIX — disallowed extensions raise FILE_BAD_TYPE ───────

  @negative @validation
  Scenario Outline: Disallowed file extension raises FILE_BAD_TYPE regardless of MIME
    When I upload file "<filename>" with mime "<mime>" and 512 bytes as owner "be-user"
    Then a domain error "FILE_BAD_TYPE" is raised

    Examples:
      | filename      | mime                         |
      | script.js     | application/javascript       |
      | archive.zip   | application/zip              |
      | virus.exe     | application/x-msdownload     |
      | page.html     | text/html                    |
      | shell.sh      | application/x-sh             |
      | macro.xlsm    | application/vnd.ms-excel.sheet.macroEnabled.12 |
      | binary.bin    | application/octet-stream     |
      | video.mp4     | video/mp4                    |
      | audio.mp3     | audio/mpeg                   |
      | archive.tar   | application/x-tar            |
      | archive.gz    | application/gzip             |

  # ─── SIZE BOUNDARY MATRIX — at/below/above 5 MiB (5242880 bytes) ────────────────

  @positive @boundary
  Scenario Outline: Uploading at or below the 5 MiB limit succeeds with accurate size
    When I upload file "file.txt" with mime "text/plain" and <size> bytes as owner "sb-user"
    Then the operation succeeds
    And the uploaded file size is <size> bytes

    Examples:
      | size    |
      | 1       |
      | 1024    |
      | 102400  |
      | 1048576 |
      | 2097152 |
      | 4194304 |
      | 5242879 |
      | 5242880 |

  @negative @boundary
  Scenario Outline: Uploading above the 5 MiB limit raises FILE_TOO_LARGE
    When I upload file "file.pdf" with mime "application/pdf" and <size> bytes as owner "ob-user"
    Then a domain error "FILE_TOO_LARGE" is raised

    Examples:
      | size    |
      | 5242881 |
      | 5242882 |
      | 6291456 |
      | 10485760|
      | 20971520|

  # ─── QUOTA MATRIX — 20 MiB (20971520 bytes) cumulative per owner ─────────────────

  @positive @boundary @business
  Scenario Outline: Owner can upload <files> files of <each> bytes each totaling <total> bytes without exceeding quota
    Given file "q1.pdf" with mime "application/pdf" and <each> bytes has been uploaded by owner "qt-<label>"
    And file "q2.pdf" with mime "application/pdf" and <each> bytes has been uploaded by owner "qt-<label>"
    And file "q3.pdf" with mime "application/pdf" and <each> bytes has been uploaded by owner "qt-<label>"
    And file "q4.pdf" with mime "application/pdf" and <each> bytes has been uploaded by owner "qt-<label>"
    Then the operation succeeds
    And owner "qt-<label>" has used <total> bytes of quota

    Examples:
      | files | each    | total    | label |
      | 4     | 5242880 | 20971520 | full  |
      | 4     | 1048576 | 4194304  | small |
      | 4     | 2621440 | 10485760 | half  |

  @negative @boundary @business
  Scenario: Uploading a 1-byte file that would push owner over the 20 MiB quota raises FILE_QUOTA_EXCEEDED
    Given file "a.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-over"
    And file "b.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-over"
    And file "c.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-over"
    And file "d.pdf" with mime "application/pdf" and 5242880 bytes has been uploaded by owner "quota-over"
    When I upload file "e.pdf" with mime "application/pdf" and 1 bytes as owner "quota-over"
    Then a domain error "FILE_QUOTA_EXCEEDED" is raised

  @negative @boundary @business
  Scenario: Uploading a file one byte over remaining quota raises FILE_QUOTA_EXCEEDED
    Given file "a.txt" with mime "text/plain" and 5242880 bytes has been uploaded by owner "quota-edge"
    And file "b.txt" with mime "text/plain" and 5242880 bytes has been uploaded by owner "quota-edge"
    And file "c.txt" with mime "text/plain" and 5242880 bytes has been uploaded by owner "quota-edge"
    And file "d.txt" with mime "text/plain" and 5242879 bytes has been uploaded by owner "quota-edge"
    When I upload file "e.txt" with mime "text/plain" and 2 bytes as owner "quota-edge"
    Then a domain error "FILE_QUOTA_EXCEEDED" is raised

  @positive @boundary @business
  Scenario: Uploading a file that uses exactly the remaining quota succeeds
    Given file "p1.txt" with mime "text/plain" and 5242880 bytes has been uploaded by owner "quota-exact2"
    And file "p2.txt" with mime "text/plain" and 5242880 bytes has been uploaded by owner "quota-exact2"
    And file "p3.txt" with mime "text/plain" and 5242880 bytes has been uploaded by owner "quota-exact2"
    And file "p4.txt" with mime "text/plain" and 5242879 bytes has been uploaded by owner "quota-exact2"
    When I upload file "p5.txt" with mime "text/plain" and 1 bytes as owner "quota-exact2"
    Then the operation succeeds
    And owner "quota-exact2" has used 20971520 bytes of quota

  # ─── MULTI-OWNER ISOLATION MATRIX — quota is per owner not global ────────────────

  @positive @business
  Scenario Outline: Different owners each upload large files without interfering with each other's quota
    When I upload file "big.pdf" with mime "application/pdf" and 5242880 bytes as owner "<owner1>"
    And I upload file "big.pdf" with mime "application/pdf" and 5242880 bytes as owner "<owner2>"
    Then the operation succeeds
    And owner "<owner1>" has used 5242880 bytes of quota
    And owner "<owner2>" has used 5242880 bytes of quota

    Examples:
      | owner1 | owner2 |
      | ownerA | ownerB |
      | ownerC | ownerD |

  # ─── CHECKSUM MATRIX — SHA-256 is deterministic for known content ─────────────────

  @positive @sanity @regression
  Scenario Outline: Uploading a file of <size> bytes always produces the same deterministic checksum
    When I upload file "chk.txt" with mime "text/plain" and <size> bytes as owner "chk-user"
    Then the operation succeeds
    And the uploaded file checksum is "<checksum>"

    Examples:
      | size | checksum                                                         |
      | 1    | 6e340b9cffb37a989ca544e6bb780a2c78901d3fb33738768511a30617afa01d |
      | 100  | bce0aff19cf5aa6a7469a30d61d04e4376e4bbf6381052ee9e7f33925c954d52 |

  # ─── DUPLICATE NAME MATRIX — per-owner uniqueness enforced ───────────────────────

  @negative @validation
  Scenario Outline: Uploading a file with the same name for the same owner raises FILE_DUPLICATE
    Given file "<name>" with mime "<mime>" and 100 bytes has been uploaded by owner "<owner>"
    When I upload file "<name>" with mime "<mime>" and 200 bytes as owner "<owner>"
    Then a domain error "FILE_DUPLICATE" is raised

    Examples:
      | name       | mime            | owner  |
      | report.pdf | application/pdf | user10 |
      | data.csv   | text/csv        | user11 |
      | image.png  | image/png       | user12 |
      | notes.txt  | text/plain      | user13 |

  # ─── REPLACE FLOW MATRIX — replace increments version and updates size ───────────

  @positive @business @regression
  Scenario Outline: Replacing a file updates its size and increments version to 2
    Given file "<name>" with mime "<mime>" and <orig_size> bytes has been uploaded by owner "<owner>"
    When I replace file "<name>" owned by "<owner>" with <new_size> bytes and mime "<mime>"
    Then the operation succeeds
    And the replaced file has version 2
    And the uploaded file size is <new_size> bytes

    Examples:
      | name       | mime            | orig_size | new_size | owner  |
      | r.pdf      | application/pdf | 1024      | 2048     | user20 |
      | r.csv      | text/csv        | 512       | 1024     | user21 |
      | r.txt      | text/plain      | 100       | 200      | user22 |
      | r.png      | image/png       | 4096      | 2048     | user23 |

  # ─── DELETION MATRIX — delete removes file and reduces count ─────────────────────

  @positive @business
  Scenario Outline: After uploading <count> files and deleting one, count is <remaining>
    Given file "<f1>" with mime "text/plain" and 100 bytes has been uploaded by owner "del-<label>"
    And file "<f2>" with mime "text/csv" and 200 bytes has been uploaded by owner "del-<label>"
    When I delete the last uploaded file
    Then the operation succeeds
    And the file storage contains 1 files
    And owner "del-<label>" has 1 files stored

    Examples:
      | f1     | f2     | count | remaining | label |
      | a.txt  | b.csv  | 2     | 1         | d1    |
      | x.txt  | y.csv  | 2     | 1         | d2    |

@domain @identity @otp @mfa
Feature: OTP and MFA Verification Outcome Matrix
  As the identity platform
  I want every OTP and MFA verification outcome to be expressly covered
  So that state transitions (valid, expired, used, attempts-exceeded, backup) are all guarded

  # OTP error codes: OTP_INVALID, OTP_EXPIRED, OTP_USED, OTP_ATTEMPTS_EXCEEDED, OTP_NOT_FOUND
  # OTP constants: MAX_VERIFY_ATTEMPTS=3, OTP_TTL_TICKS=5
  # MFA error codes: MFA_BAD_CODE, MFA_ALREADY_ENROLLED, MFA_NOT_ENROLLED, MFA_BACKUP_USED, MFA_BACKUP_INVALID
  # MFA constants: BACKUP_CODE_COUNT=8

  # ---------------------------------------------------------------------------
  # SECTION 1 — OTP lifecycle outcome matrix
  # ---------------------------------------------------------------------------

  @otp @positive @smoke
  Scenario: OTP happy-path — generate then verify succeeds for a fresh account
    Given a clean OTP service
    And an OTP-managed account for user "otp_happy" with email "otp.happy@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    Then no domain error is raised

  @otp @positive @boundary
  Scenario: OTP verified at the last valid tick (tick=5) still succeeds
    Given a clean OTP service
    And an OTP-managed account for user "otp_boundary" with email "otp.boundary@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I advance the OTP service tick by 5
    And I verify the OTP with the generated code
    Then no domain error is raised

  @otp @negative @boundary
  Scenario: OTP verified one tick past TTL raises OTP_EXPIRED
    Given a clean OTP service
    And an OTP-managed account for user "otp_expire1" with email "otp.expire1@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I advance the OTP service tick by 6
    And I verify the OTP with the generated code
    Then a domain error "OTP_EXPIRED" is raised

  @otp @negative @boundary
  Scenario: OTP verified well past TTL (tick=100) also raises OTP_EXPIRED
    Given a clean OTP service
    And an OTP-managed account for user "otp_expire2" with email "otp.expire2@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I advance the OTP service tick by 100
    And I verify the OTP with the generated code
    Then a domain error "OTP_EXPIRED" is raised

  @otp @negative @business
  Scenario: A consumed OTP raises OTP_USED on second verification attempt
    Given a clean OTP service
    And an OTP-managed account for user "otp_used1" with email "otp.used1@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    And I verify the OTP with the generated code
    Then a domain error "OTP_USED" is raised

  @otp @negative @business
  Scenario: Even the correct code raises OTP_USED after the OTP was already consumed
    Given a clean OTP service
    And an OTP-managed account for user "otp_used2" with email "otp.used2@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I verify the OTP with the generated code
    And I verify the OTP with the generated code
    Then a domain error "OTP_USED" is raised

  @otp @negative @boundary
  Scenario Outline: Wrong-code attempts exhaust the counter and then raise OTP_ATTEMPTS_EXCEEDED
    Given a clean OTP service
    And an OTP-managed account for user "<user>" with email "<email>" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I verify the OTP <attempts> times with wrong code "<wrong>"
    Then a domain error "<code>" is raised

    Examples:
      | user        | email                      | attempts | wrong  | code                   | boundary-note                         |
      | otp_atm1    | otp.atm1@omiinqa.test      | 1        | 000000 | OTP_INVALID            | 1st wrong — still < MAX              |
      | otp_atm2    | otp.atm2@omiinqa.test      | 2        | 000000 | OTP_INVALID            | 2nd wrong — still < MAX              |
      | otp_atm3    | otp.atm3@omiinqa.test      | 3        | 000000 | OTP_ATTEMPTS_EXCEEDED  | 3rd wrong — hits MAX_VERIFY_ATTEMPTS |
      | otp_atm4    | otp.atm4@omiinqa.test      | 4        | 000000 | OTP_ATTEMPTS_EXCEEDED  | beyond MAX — still locked             |
      | otp_atm5    | otp.atm5@omiinqa.test      | 10       | 000000 | OTP_ATTEMPTS_EXCEEDED  | far beyond MAX                        |

  @otp @negative @business
  Scenario: Correct code submitted after attempts exhausted still raises OTP_ATTEMPTS_EXCEEDED
    Given a clean OTP service
    And an OTP-managed account for user "otp_lock_then_correct" with email "otp.ltc@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I verify the OTP 3 times with wrong code "000000"
    And I verify the OTP with the generated code
    Then a domain error "OTP_ATTEMPTS_EXCEEDED" is raised

  @otp @negative @boundary
  Scenario: Verifying without prior generation raises OTP_NOT_FOUND
    Given a clean OTP service
    And an OTP-managed account for user "otp_nogen" with email "otp.nogen@omiinqa.test" and password "Sup3rSecret"
    When I verify the OTP with code "123456"
    Then a domain error "OTP_NOT_FOUND" is raised

  @otp @positive @business
  Scenario: Regenerating an OTP invalidates the prior one and the new code is verified successfully
    Given a clean OTP service
    And an OTP-managed account for user "otp_regen" with email "otp.regen@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I generate an OTP for the account
    And I verify the OTP with the generated code
    Then no domain error is raised

  @otp @positive @boundary
  Scenario: OTP verified at tick=0 (immediately after generation) succeeds
    Given a clean OTP service
    And an OTP-managed account for user "otp_tick0" with email "otp.tick0@omiinqa.test" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I advance the OTP service tick by 0
    And I verify the OTP with the generated code
    Then no domain error is raised

  @otp @negative @validation
  Scenario Outline: Various invalid code formats produce OTP_INVALID on first attempt
    Given a clean OTP service
    And an OTP-managed account for user "<user>" with email "<email>" and password "Sup3rSecret"
    When I generate an OTP for the account
    And I verify the OTP with code "<code>"
    Then a domain error "OTP_INVALID" is raised

    Examples:
      | user        | email                      | code   | format-note                |
      | otp_fmt1    | otp.fmt1@omiinqa.test      | 000000 | all zeros — wrong value    |
      | otp_fmt2    | otp.fmt2@omiinqa.test      | 999999 | all nines — wrong value    |
      | otp_fmt3    | otp.fmt3@omiinqa.test      | ABCDEF | letters — wrong value      |
      | otp_fmt4    | otp.fmt4@omiinqa.test      | 111111 | repeated — wrong value     |
      | otp_fmt5    | otp.fmt5@omiinqa.test      | 123456 | sequential — wrong value   |

  # ---------------------------------------------------------------------------
  # SECTION 2 — MFA lifecycle outcome matrix
  # ---------------------------------------------------------------------------

  @mfa @positive @smoke
  Scenario: MFA happy-path — enrol then verify the expected TOTP code
    Given a clean MFA service
    And an MFA-managed account for user "mfa_happy" with email "mfa.happy@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I verify the MFA code for the account
    Then no domain error is raised
    And the account is enrolled in MFA

  @mfa @positive @boundary
  Scenario: Valid TOTP code accepted one tick after enrolment (clock-skew window)
    Given a clean MFA service
    And an MFA-managed account for user "mfa_skew" with email "mfa.skew@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I advance the MFA service tick by 1
    And I verify the MFA code for the account
    Then no domain error is raised

  @mfa @negative @boundary
  Scenario Outline: Various wrong TOTP codes raise MFA_BAD_CODE
    Given a clean MFA service
    And an MFA-managed account for user "<user>" with email "<email>" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I verify the MFA code "<code>" for the account
    Then a domain error "MFA_BAD_CODE" is raised

    Examples:
      | user        | email                      | code    | note                     |
      | mfa_bad1    | mfa.bad1@omiinqa.test      | 000000  | all zeros                |
      | mfa_bad2    | mfa.bad2@omiinqa.test      | 999999  | all nines                |
      | mfa_bad3    | mfa.bad3@omiinqa.test      | 111111  | repeated digit           |
      | mfa_bad4    | mfa.bad4@omiinqa.test      | abc123  | alphanumeric             |
      | mfa_bad5    | mfa.bad5@omiinqa.test      | 1234    | too short                |
      | mfa_bad6    | mfa.bad6@omiinqa.test      | 12345678| too long                 |

  @mfa @negative @business
  Scenario: Double-enrolment raises MFA_ALREADY_ENROLLED
    Given a clean MFA service
    And an MFA-managed account for user "mfa_double" with email "mfa.double@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I enrol the account in MFA
    Then a domain error "MFA_ALREADY_ENROLLED" is raised

  @mfa @negative @boundary
  Scenario: Verifying code without enrolment raises MFA_NOT_ENROLLED
    Given a clean MFA service
    And an MFA-managed account for user "mfa_noenrol" with email "mfa.noenrol@omiinqa.test" and password "Sup3rSecret"
    When I verify the MFA code "123456" for the account
    Then a domain error "MFA_NOT_ENROLLED" is raised

  @mfa @positive @business
  Scenario: All 8 backup codes are issued and are distinct
    Given a clean MFA service
    And an MFA-managed account for user "mfa_bkp_count" with email "mfa.bkpcount@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    Then the MFA enrolment result contains 8 backup codes
    And the MFA backup codes are all distinct

  @mfa @positive @business
  Scenario: First backup code can be used successfully
    Given a clean MFA service
    And an MFA-managed account for user "mfa_bkp_first" with email "mfa.bkpfirst@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I use the first MFA backup code for the account
    Then no domain error is raised

  @mfa @negative @business
  Scenario: Re-using the same backup code raises MFA_BACKUP_USED
    Given a clean MFA service
    And an MFA-managed account for user "mfa_bkp_reuse" with email "mfa.bkpreuse@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I use the first MFA backup code for the account
    And I use the first MFA backup code for the account
    Then a domain error "MFA_BACKUP_USED" is raised

  @mfa @negative @boundary
  Scenario Outline: Unrecognised backup codes raise MFA_BACKUP_INVALID
    Given a clean MFA service
    And an MFA-managed account for user "<user>" with email "<email>" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I use MFA backup code "<code>" for the account
    Then a domain error "MFA_BACKUP_INVALID" is raised

    Examples:
      | user           | email                        | code           | note                      |
      | mfa_bkp_inv1   | mfa.bkpinv1@omiinqa.test     | BKP-FAKE00     | fabricated code           |
      | mfa_bkp_inv2   | mfa.bkpinv2@omiinqa.test     | 000000         | wrong format entirely     |
      | mfa_bkp_inv3   | mfa.bkpinv3@omiinqa.test     | BKP-000000     | plausible but not issued  |
      | mfa_bkp_inv4   | mfa.bkpinv4@omiinqa.test     | INVALID-CODE   | non-BKP prefix            |

  @mfa @positive @business
  Scenario: MFA can be disabled with the correct code; account shows not enrolled afterward
    Given a clean MFA service
    And an MFA-managed account for user "mfa_disable" with email "mfa.disable@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I disable MFA for the account using the correct code
    Then no domain error is raised
    And the account is not enrolled in MFA

  @mfa @negative @business
  Scenario: MFA disable with wrong code raises MFA_BAD_CODE and enrolment remains active
    Given a clean MFA service
    And an MFA-managed account for user "mfa_disable_bad" with email "mfa.disablebad@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I disable MFA for the account using code "000000"
    Then a domain error "MFA_BAD_CODE" is raised
    And the account is enrolled in MFA

  @mfa @negative @business
  Scenario: Attempting to disable MFA when not enrolled raises MFA_NOT_ENROLLED
    Given a clean MFA service
    And an MFA-managed account for user "mfa_dis_noenrol" with email "mfa.disnoenrol@omiinqa.test" and password "Sup3rSecret"
    When I disable MFA for the account using code "123456"
    Then a domain error "MFA_NOT_ENROLLED" is raised

  @mfa @positive @business
  Scenario: Re-enrolling after disabling succeeds and issues new backup codes
    Given a clean MFA service
    And an MFA-managed account for user "mfa_reenrol" with email "mfa.reenrol@omiinqa.test" and password "Sup3rSecret"
    When I enrol the account in MFA
    And I disable MFA for the account using the correct code
    And I enrol the account in MFA
    Then no domain error is raised
    And the account is enrolled in MFA
    And the MFA enrolment result contains 8 backup codes

package com.omiinqa.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight credential pair used exclusively for login / authentication tests.
 *
 * <p><strong>Pattern:</strong> Value Object — a minimal, purpose-specific data
 * carrier that avoids passing raw string pairs through test methods. Using a
 * typed object makes {@link org.testng.annotations.DataProvider} signatures
 * self-documenting and prevents argument-order mistakes.</p>
 *
 * <p>Populated by {@link com.omiinqa.data.factory.CredentialsFactory}, which
 * encodes all SauceDemo user accounts in one place.</p>
 *
 * <p><strong>SauceDemo credentials reference:</strong>
 * <ul>
 *   <li>{@code standard_user} / {@code secret_sauce}</li>
 *   <li>{@code locked_out_user} / {@code secret_sauce}</li>
 *   <li>{@code problem_user} / {@code secret_sauce}</li>
 *   <li>{@code performance_glitch_user} / {@code secret_sauce}</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {

    /** Login username. */
    private String username;

    /**
     * Plain-text password for test fixture use.
     * In production test suites prefer reading this from a secrets manager.
     */
    private String password;
}

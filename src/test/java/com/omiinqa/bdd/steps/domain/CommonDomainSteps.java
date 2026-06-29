package com.omiinqa.bdd.steps.domain;

import com.omiinqa.bdd.support.DomainWorld;
import com.omiinqa.reference.core.DomainException;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic, reusable outcome assertions shared by ALL reference-domain features.
 *
 * <p>Defined once here so no domain step class redefines them (avoids Cucumber
 * duplicate/ambiguous step errors). Domain {@code When} steps record their
 * outcome via {@link DomainWorld}; these {@code Then} steps assert it.</p>
 */
public class CommonDomainSteps {

    @Then("the operation succeeds")
    public void theOperationSucceeds() {
        final DomainException error = DomainWorld.lastError();
        assertThat(error)
                .as("expected success but got error: %s",
                        error == null ? "none" : error.code() + " / " + error.getMessage())
                .isNull();
    }

    @Then("no domain error is raised")
    public void noDomainErrorIsRaised() {
        theOperationSucceeds();
    }

    @Then("a domain error {string} is raised")
    public void aDomainErrorIsRaised(final String expectedCode) {
        final DomainException error = DomainWorld.lastError();
        assertThat(error).as("a domain error was expected but none occurred").isNotNull();
        assertThat(error.code()).as("error code").isEqualTo(expectedCode);
    }

    @Then("the domain error message contains {string}")
    public void theDomainErrorMessageContains(final String fragment) {
        final DomainException error = DomainWorld.lastError();
        assertThat(error).as("a domain error was expected").isNotNull();
        assertThat(error.getMessage()).containsIgnoringCase(fragment);
    }
}

package com.omiinqa.api.graphql;

import com.omiinqa.api.validator.ResponseValidator;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.response.Response;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GraphQL country-by-code query tests for the Countries API.
 *
 * <p>Verifies that the {@code country(code:)} field returns correct data for
 * known countries, that nested {@code languages} are populated, and that the
 * response meets reasonable SLA constraints.</p>
 *
 * <p>Tests do NOT extend {@code BaseTest} (no WebDriver needed) and do NOT
 * extend {@code AbstractApiTest} (as required by the scope contract); REST
 * Assured is configured via the framework's existing static initialisation in
 * {@link com.omiinqa.api.client.ApiClient}.</p>
 *
 * @see GraphQlClient
 * @see CountriesQueries
 */
@Feature("GraphQL Countries API")
@Story("Country Query")
public class CountryQueryTest {

    private static final Logger LOG = LoggerFactory.getLogger(CountryQueryTest.class);

    private GraphQlClient client;
    private FrameworkConfig config;

    /**
     * Initialises the {@link GraphQlClient} and {@link FrameworkConfig} once
     * per test class.
     */
    @BeforeClass(alwaysRun = true)
    public void initClient() {
        config = FrameworkConfig.get();
        client = new GraphQlClient();
        LOG.info("CountryQueryTest initialised against: {}", config.apiUrl("countries.graphql"));
    }

    // -----------------------------------------------------------------------
    //  Data provider — data-driven country assertions
    // -----------------------------------------------------------------------

    /**
     * Supplies ISO code → expected-name pairs for data-driven country lookup tests.
     *
     * @return a 2-D array of [code, expectedName, expectedCapital, expectedEmoji]
     */
    @DataProvider(name = "countryCodes")
    public Object[][] countryCodes() {
        return new Object[][]{
            {"US", "United States", "Washington D.C.", "🇺🇸"},
            {"IN", "India",         "New Delhi",       "🇮🇳"},
            {"BR", "Brazil",        "Brasília",        "🇧🇷"},
        };
    }

    // -----------------------------------------------------------------------
    //  Positive: name, capital, currency, emoji
    // -----------------------------------------------------------------------

    /**
     * Data-driven test: queries each country by code and asserts the name
     * returned in {@code data.country.name} matches the expected value.
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "countryCodes",
          description = "country(code) returns correct country name for US, IN, BR")
    @Description("Validates data.country.name for data-driven country codes")
    public void countryByCode_name_matchesExpected(
            final String code, final String expectedName,
            final String expectedCapital, final String expectedEmoji) {

        final Response response = client.query(
                CountriesQueries.COUNTRY_BY_CODE_WITH_VARIABLE,
                java.util.Map.of("code", code));

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("data.country.name", expectedName);

        LOG.info("country({}).name = {}", code, response.jsonPath().getString("data.country.name"));
    }

    /**
     * Data-driven test: asserts {@code data.country.capital} for US, IN, BR.
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "countryCodes",
          description = "country(code) returns correct capital for US, IN, BR")
    @Description("Validates data.country.capital for data-driven country codes")
    public void countryByCode_capital_matchesExpected(
            final String code, final String expectedName,
            final String expectedCapital, final String expectedEmoji) {

        final Response response = client.query(CountriesQueries.countryByCode(code));

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("data.country.capital", expectedCapital);
    }

    // -----------------------------------------------------------------------
    //  United States — detailed field assertions
    // -----------------------------------------------------------------------

    /**
     * Verifies that querying country code "US" returns currency "USD,USN,USS"
     * (the Countries API concatenates multiple currencies with commas).
     */
    @Test(groups = {"api", "regression"},
          description = "US country query returns currency containing USD")
    @Description("Validates data.country.currency contains 'USD' for the United States")
    public void countryUS_currency_containsUSD() {
        final Response response = client.query(CountriesQueries.COUNTRY_US);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathContains("data.country.currency", "USD");
    }

    /**
     * Verifies that the US country response includes a non-empty languages list.
     */
    @Test(groups = {"api", "regression"},
          description = "US country query returns at least one language")
    @Description("Validates data.country.languages list is non-empty for United States")
    public void countryUS_languages_listIsNotEmpty() {
        final Response response = client.query(CountriesQueries.COUNTRY_US);

        ResponseValidator.of(response).statusCode(200);
        final List<?> languages = response.jsonPath().getList("data.country.languages");
        Assertions.assertThat(languages)
                .as("data.country.languages must not be empty for US")
                .isNotEmpty();
    }

    /**
     * Verifies that the US country response includes a language with code "en".
     */
    @Test(groups = {"api", "regression"},
          description = "US country languages include English (code 'en')")
    @Description("Validates data.country.languages contains English for United States")
    public void countryUS_languages_includeEnglish() {
        final Response response = client.query(CountriesQueries.COUNTRY_US);

        ResponseValidator.of(response).statusCode(200);
        final List<String> codes = response.jsonPath().getList("data.country.languages.code");
        Assertions.assertThat(codes)
                .as("Languages for US must include 'en'")
                .contains("en");
    }

    // -----------------------------------------------------------------------
    //  India — nested language assertions
    // -----------------------------------------------------------------------

    /**
     * Verifies that the India country response includes Hindi (language code "hi").
     */
    @Test(groups = {"api", "regression"},
          description = "India country languages include Hindi (code 'hi')")
    @Description("Validates that India's languages contain Hindi")
    public void countryIN_languages_includeHindi() {
        final Response response = client.query(CountriesQueries.COUNTRY_IN);

        ResponseValidator.of(response).statusCode(200);
        final List<String> codes = response.jsonPath().getList("data.country.languages.code");
        Assertions.assertThat(codes)
                .as("Languages for IN must include 'hi'")
                .contains("hi");
    }

    // -----------------------------------------------------------------------
    //  SLA assertion
    // -----------------------------------------------------------------------

    /**
     * Verifies that a single country query responds within the 5-second SLA.
     */
    @Test(groups = {"api", "regression"},
          description = "country query responds within 5 seconds SLA")
    @Description("Validates response time SLA for a single country query")
    public void countryQuery_respondsWithinSla() {
        final Response response = client.query(CountriesQueries.COUNTRY_US);

        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  Brazil
    // -----------------------------------------------------------------------

    /**
     * Verifies that Brazil's country name is returned correctly.
     */
    @Test(groups = {"api", "regression"},
          description = "Brazil country query returns correct name 'Brazil'")
    @Description("Validates data.country.name equals 'Brazil' for code BR")
    public void countryBR_name_equalsBrazil() {
        final Response response = client.query(CountriesQueries.COUNTRY_BR);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("data.country.name", "Brazil");
    }

}

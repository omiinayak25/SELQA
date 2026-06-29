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

/**
 * GraphQL continent query tests for the Countries API.
 *
 * <p>Verifies that the {@code continents} root field returns exactly 7 entries,
 * that each continent has a non-null code and name, that nested country lists
 * are non-empty, and that specific continents can be identified by code.</p>
 *
 * <p>Tests do NOT extend {@code BaseTest} or {@code AbstractApiTest}.</p>
 *
 * @see GraphQlClient
 * @see CountriesQueries
 */
@Feature("GraphQL Countries API")
@Story("Continent Query")
public class ContinentQueryTest {

    /** The Countries API exposes exactly seven continents. */
    private static final int EXPECTED_CONTINENT_COUNT = 7;

    private static final Logger LOG = LoggerFactory.getLogger(ContinentQueryTest.class);

    private GraphQlClient client;

    /**
     * Initialises the {@link GraphQlClient} once per test class.
     */
    @BeforeClass(alwaysRun = true)
    public void initClient() {
        client = new GraphQlClient();
        LOG.info("ContinentQueryTest initialised against: {}",
                FrameworkConfig.get().apiUrl("countries.graphql"));
    }

    // -----------------------------------------------------------------------
    //  Continents list — structural assertions
    // -----------------------------------------------------------------------

    /**
     * Verifies that the {@code continents} query returns exactly 7 continents.
     */
    @Test(groups = {"api", "regression"},
          description = "continents query returns exactly 7 continents")
    @Description("Validates data.continents list size equals 7")
    public void continents_listSize_equals7() {
        final Response response = client.query(CountriesQueries.CONTINENTS);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathListSize("data.continents", EXPECTED_CONTINENT_COUNT);

        LOG.info("Continents count: {}", response.jsonPath().getList("data.continents").size());
    }

    /**
     * Verifies that every continent in the response has a non-null code.
     */
    @Test(groups = {"api", "regression"},
          description = "every continent has a non-null code field")
    @Description("Validates all continents have non-null code values")
    public void continents_eachContinent_hasNonNullCode() {
        final Response response = client.query(CountriesQueries.CONTINENTS);

        ResponseValidator.of(response).statusCode(200);

        final List<String> codes = response.jsonPath().getList("data.continents.code");
        Assertions.assertThat(codes)
                .as("All continent codes must be non-null")
                .allMatch(c -> c != null && !c.isBlank());
    }

    /**
     * Verifies that every continent in the response has a non-null name.
     */
    @Test(groups = {"api", "regression"},
          description = "every continent has a non-null name field")
    @Description("Validates all continents have non-null name values")
    public void continents_eachContinent_hasNonNullName() {
        final Response response = client.query(CountriesQueries.CONTINENTS);

        ResponseValidator.of(response).statusCode(200);

        final List<String> names = response.jsonPath().getList("data.continents.name");
        Assertions.assertThat(names)
                .as("All continent names must be non-null and non-blank")
                .allMatch(n -> n != null && !n.isBlank());
    }

    // -----------------------------------------------------------------------
    //  Specific continent code assertions
    // -----------------------------------------------------------------------

    /**
     * Supplies well-known continent codes for data-driven existence checks.
     *
     * @return array of [continentCode, continentName] pairs
     */
    @DataProvider(name = "continentData")
    public Object[][] continentData() {
        return new Object[][]{
            {"EU", "Europe"},
            {"AS", "Asia"},
            {"NA", "North America"},
            {"AF", "Africa"},
        };
    }

    /**
     * Data-driven test: verifies that each well-known continent code is present
     * in the continents list returned by the API.
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "continentData",
          description = "continents list contains expected continent codes (EU, AS, NA, AF)")
    @Description("Validates specific continent codes appear in the continents response")
    public void continents_expectedCode_isPresentInList(
            final String continentCode, final String continentName) {

        final Response response = client.query(CountriesQueries.CONTINENTS);

        ResponseValidator.of(response).statusCode(200);

        final List<String> codes = response.jsonPath().getList("data.continents.code");
        Assertions.assertThat(codes)
                .as("Continent code '%s' must appear in the continents list", continentCode)
                .contains(continentCode);
    }

    // -----------------------------------------------------------------------
    //  Nested countries within continents
    // -----------------------------------------------------------------------

    /**
     * Verifies that continents with nested countries return at least one country
     * for the European continent.
     */
    @Test(groups = {"api", "regression"},
          description = "continents with nested countries: Europe has at least 1 country")
    @Description("Validates that European continent has non-empty nested country list")
    public void continentsWithCountries_europe_hasNonEmptyCountryList() {
        final Response response = client.query(CountriesQueries.CONTINENTS_WITH_COUNTRIES);

        ResponseValidator.of(response).statusCode(200);

        // Find the index of EU in the list and assert its countries are non-empty
        final List<String> codes = response.jsonPath().getList("data.continents.code");
        Assertions.assertThat(codes).as("EU must exist in continents").contains("EU");

        final int euIndex = codes.indexOf("EU");
        final List<?> euCountries = response.jsonPath()
                .getList("data.continents[" + euIndex + "].countries");
        Assertions.assertThat(euCountries)
                .as("Europe must have at least one country")
                .isNotEmpty();
        LOG.info("Europe has {} countries", euCountries.size());
    }

    /**
     * Verifies that the countries within a continent each have a non-null code.
     */
    @Test(groups = {"api", "regression"},
          description = "countries nested in continents all have non-null codes")
    @Description("Validates that nested country codes are non-null across all continents")
    public void continentsWithCountries_nestedCountryCodes_areNotNull() {
        final Response response = client.query(CountriesQueries.CONTINENTS_WITH_COUNTRIES);

        ResponseValidator.of(response).statusCode(200);

        // Flat list of all nested country codes across all continents
        final List<String> allCodes = response.jsonPath()
                .getList("data.continents.countries.code.flatten()");
        Assertions.assertThat(allCodes)
                .as("All nested country codes must be non-null")
                .isNotEmpty();
    }
}

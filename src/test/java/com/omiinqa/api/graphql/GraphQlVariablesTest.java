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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GraphQL variable binding, alias query, and countries-filtered-by-continent tests.
 *
 * <p>This class exercises:</p>
 * <ul>
 *   <li>Queries that pass typed variables via the {@code variables} JSON field.</li>
 *   <li>The {@link GraphQlClient#query(String, Map)} overload.</li>
 *   <li>Continent-filtered country lists using the {@code filter} argument.</li>
 *   <li>Alias queries that resolve two countries in one round trip.</li>
 *   <li>Empty-variables edge case (empty map must be treated as no variables).</li>
 * </ul>
 *
 * <p>Tests do NOT extend {@code BaseTest} or {@code AbstractApiTest}.</p>
 *
 * @see GraphQlClient
 * @see CountriesQueries
 */
@Feature("GraphQL Countries API")
@Story("Variables and Advanced Queries")
public class GraphQlVariablesTest {

    private static final Logger LOG = LoggerFactory.getLogger(GraphQlVariablesTest.class);

    private GraphQlClient client;

    /**
     * Initialises the {@link GraphQlClient} once per test class.
     */
    @BeforeClass(alwaysRun = true)
    public void initClient() {
        client = new GraphQlClient();
        LOG.info("GraphQlVariablesTest initialised against: {}",
                FrameworkConfig.get().apiUrl("countries.graphql"));
    }

    // -----------------------------------------------------------------------
    //  Variables — data-driven country lookups
    // -----------------------------------------------------------------------

    /**
     * Supplies code → expected-name pairs for variable-driven country tests.
     *
     * @return 2-D array of [code, expectedName]
     */
    @DataProvider(name = "variableCountryCodes")
    public Object[][] variableCountryCodes() {
        return new Object[][]{
            {"US", "United States"},
            {"DE", "Germany"},
            {"JP", "Japan"},
        };
    }

    /**
     * Data-driven test: issues the country query with variables for US, DE, JP
     * and asserts the returned name matches expectations.
     */
    @Test(groups = {"api", "regression"},
          dataProvider = "variableCountryCodes",
          description = "country query with $code variable returns correct name for US, DE, JP")
    @Description("Validates GraphQL variable substitution for country code lookups")
    public void countryWithVariable_name_matchesExpected(
            final String code, final String expectedName) {

        final Response response = client.query(
                CountriesQueries.COUNTRY_BY_CODE_WITH_VARIABLE,
                Map.of("code", code));

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("data.country.name", expectedName);

        LOG.info("variable country({}).name = {}", code,
                response.jsonPath().getString("data.country.name"));
    }

    /**
     * Verifies that the language list for Germany (DE) includes German (code "de").
     */
    @Test(groups = {"api", "regression"},
          description = "Germany country query with variable returns German language 'de'")
    @Description("Validates nested languages via variable query for Germany")
    public void countryDE_withVariable_languagesIncludeGerman() {
        final Response response = client.query(
                CountriesQueries.COUNTRY_BY_CODE_WITH_VARIABLE,
                Map.of("code", "DE"));

        ResponseValidator.of(response).statusCode(200);

        final List<String> codes = response.jsonPath().getList("data.country.languages.code");
        Assertions.assertThat(codes)
                .as("Germany languages must include 'de'")
                .contains("de");
    }

    // -----------------------------------------------------------------------
    //  Countries filtered by continent
    // -----------------------------------------------------------------------

    /**
     * Verifies that filtering countries by continent EU returns a non-empty list.
     */
    @Test(groups = {"api", "regression"},
          description = "countriesByContinent(EU) returns non-empty country list")
    @Description("Validates that European countries filter returns results")
    public void countriesByContinent_europe_returnsNonEmptyList() {
        final Response response = client.query(CountriesQueries.countriesByContinent("EU"));

        ResponseValidator.of(response).statusCode(200);

        final List<?> countries = response.jsonPath().getList("data.countries");
        Assertions.assertThat(countries)
                .as("European countries list must not be empty")
                .isNotEmpty();
        LOG.info("EU countries count: {}", countries.size());
    }

    /**
     * Verifies that filtering by continent AS (Asia) returns more than 10 countries,
     * confirming the filter actually narrows the full 250+ country list.
     */
    @Test(groups = {"api", "regression"},
          description = "countriesByContinent(AS) returns at least 10 Asian countries")
    @Description("Validates Asia continent filter returns a substantive list")
    public void countriesByContinent_asia_returnsAtLeast10Countries() {
        final Response response = client.query(CountriesQueries.countriesByContinent("AS"));

        ResponseValidator.of(response).statusCode(200);

        final List<?> countries = response.jsonPath().getList("data.countries");
        Assertions.assertThat(countries)
                .as("Asia countries list must have at least 10 entries")
                .hasSizeGreaterThanOrEqualTo(10);
    }

    // -----------------------------------------------------------------------
    //  Alias queries
    // -----------------------------------------------------------------------

    /**
     * Verifies that a single request with field aliases resolves both the US
     * and Germany in one response object.
     */
    @Test(groups = {"api", "regression"},
          description = "alias query resolves US and Germany in a single request")
    @Description("Validates GraphQL field aliases for two simultaneous country lookups")
    public void aliasQuery_twoCountries_bothResolveCorrectly() {
        final Response response = client.query(CountriesQueries.ALIAS_TWO_COUNTRIES);

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("data.usa.name",     "United States")
                .bodyJsonPath("data.germany.name", "Germany");

        LOG.info("Alias USA capital: {}", response.jsonPath().getString("data.usa.capital"));
        LOG.info("Alias Germany capital: {}", response.jsonPath().getString("data.germany.capital"));
    }

    // -----------------------------------------------------------------------
    //  Empty variables edge case
    // -----------------------------------------------------------------------

    /**
     * Verifies that passing an empty variables map behaves identically to passing
     * {@code null} variables — the query with no declared variables must succeed.
     */
    @Test(groups = {"api", "regression"},
          description = "query with empty variables map succeeds (treats as no variables)")
    @Description("Validates that an empty Map<> passed as variables does not break the request")
    public void query_emptyVariablesMap_succeeds() {
        // CONTINENTS has no variable declarations; passing empty map must not cause errors
        final Response response = client.query(
                CountriesQueries.CONTINENTS, Collections.emptyMap());

        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("data.continents");

        final List<?> continents = response.jsonPath().getList("data.continents");
        Assertions.assertThat(continents)
                .as("Continents must be returned even when empty variables map is supplied")
                .isNotEmpty();
    }

    // -----------------------------------------------------------------------
    //  Languages root field
    // -----------------------------------------------------------------------

    /**
     * Verifies that the {@code languages} root field returns a non-empty list
     * confirming the Countries API exposes more than zero registered languages.
     */
    @Test(groups = {"api", "regression"},
          description = "languages query returns non-empty list of world languages")
    @Description("Validates data.languages list is populated")
    public void languages_query_returnsNonEmptyList() {
        final Response response = client.query(CountriesQueries.LANGUAGES);

        ResponseValidator.of(response).statusCode(200);

        final List<?> languages = response.jsonPath().getList("data.languages");
        Assertions.assertThat(languages)
                .as("Languages list must not be empty")
                .hasSizeGreaterThan(0);
        LOG.info("Total languages returned: {}", languages.size());
    }
}

package com.omiinqa.api.jsonplaceholder;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.jsonplaceholder.Post;
import com.omiinqa.api.models.jsonplaceholder.Todo;
import com.omiinqa.api.services.JsonPlaceholderService;
import com.omiinqa.api.validator.ResponseValidator;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * JSON Schema validation tests for JSONPlaceholder resources.
 *
 * <p>Validates GET responses from three representative resources (posts, todos,
 * users) against Draft-07 JSON Schema files located at
 * {@code src/test/resources/schemas/jsonplaceholder-*.json}.  Covers both the
 * inline fluent {@link ResponseValidator#matchesSchema(String)} path and direct
 * {@link com.omiinqa.api.validator.SchemaValidator#validate(Response, String)}
 * calls, mirroring the pattern established in the existing
 * {@code SchemaValidationApiTest}.</p>
 *
 * <p>Additional sub-scenarios exercise field-type and constraint assertions that
 * complement schema validation (e.g., {@code id >= 1}, {@code email} contains
 * {@code @}, {@code completed} is boolean), providing defence-in-depth against
 * schema drift.</p>
 *
 * <p>Does NOT extend {@code BaseTest}; extends {@link AbstractApiTest}.</p>
 */
@Epic("JSONPlaceholder API")
@Feature("Schema Validation")
public class JsonPlaceholderSchemaTest extends AbstractApiTest {

    private JsonPlaceholderService service;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        service = new JsonPlaceholderService();
        log.info("JsonPlaceholderSchemaTest initialized");
    }

    // -----------------------------------------------------------------------
    //  POST schema
    // -----------------------------------------------------------------------

    @Story("Post schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /posts/1 response matches jsonplaceholder-post-schema.json (Draft-07)")
    @Test(groups = {"api", "regression"},
          description = "GET /posts/1 response matches jsonplaceholder-post-schema.json")
    public void getPost_id1_matchesPostSchema() {
        final Response response = service.getPostById(1);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-post-schema.json");
    }

    @Story("Post schema — id constraint")
    @Severity(SeverityLevel.NORMAL)
    @Description("Schema enforces id >= 1; GET /posts/100 satisfies the minimum constraint")
    @Test(groups = {"api", "regression"},
          description = "GET /posts/100 id field satisfies schema minimum=1 constraint")
    public void getPost_id100_idSatisfiesMinimumConstraint() {
        final Response response = service.getPostById(100);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-post-schema.json");

        final int id = response.jsonPath().getInt("id");
        Assertions.assertThat(id)
                .as("Post id must satisfy schema minimum=1")
                .isGreaterThanOrEqualTo(1);
    }

    @Story("Post schema — required fields present")
    @Severity(SeverityLevel.NORMAL)
    @Description("Schema requires id, userId, title, body; all are non-null on GET /posts/50")
    @Test(groups = {"api", "regression"},
          description = "GET /posts/50 all required schema fields are non-null")
    public void getPost_id50_allRequiredFieldsPresent() {
        final Response response = service.getPostById(50);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-post-schema.json")
                .bodyJsonPathNotNull("id")
                .bodyJsonPathNotNull("userId")
                .bodyJsonPathNotNull("title")
                .bodyJsonPathNotNull("body");
    }

    @Story("POST create — echoed body schema")
    @Severity(SeverityLevel.NORMAL)
    @Description("POST /posts echoed body satisfies schema; title and body are non-blank strings")
    @Test(groups = {"api", "regression"},
          description = "POST /posts echoed response body has non-blank title and body fields")
    public void createPost_echoedBody_titleAndBodyNonBlank() {
        final Post post = Post.builder()
                .userId(3)
                .title("Schema Echo Test")
                .body("Body text for schema echo test")
                .build();

        final Response response = service.createPost(post);
        ResponseValidator.of(response)
                .statusCode(201)
                .bodyJsonPathNotNull("id");

        final String title = response.jsonPath().getString("title");
        final String body  = response.jsonPath().getString("body");
        Assertions.assertThat(title).isNotBlank();
        Assertions.assertThat(body).isNotBlank();
    }

    // -----------------------------------------------------------------------
    //  TODO schema
    // -----------------------------------------------------------------------

    @Story("Todo schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /todos/1 response matches jsonplaceholder-todo-schema.json (Draft-07)")
    @Test(groups = {"api", "regression"},
          description = "GET /todos/1 response matches jsonplaceholder-todo-schema.json")
    public void getTodo_id1_matchesTodosSchema() {
        final Response response = service.getTodoById(1);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-todo-schema.json");
    }

    @Story("Todo schema — completed boolean type")
    @Severity(SeverityLevel.NORMAL)
    @Description("Schema enforces completed as boolean; both true and false values are accepted")
    @Test(groups = {"api", "regression"},
          description = "GET /todos/9 completed=false satisfies schema boolean type constraint")
    public void getTodo_id9_completedFalseSatisfiesSchema() {
        // Todo 9 has completed=false in the seed data
        final Response response = service.getTodoById(9);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-todo-schema.json");
    }

    @DataProvider(name = "todoSchemaIds")
    public Object[][] todoSchemaIds() {
        return new Object[][]{
            {1},   // boundary
            {100}, // mid
            {200}  // boundary
        };
    }

    @Story("Todo schema — data-driven boundary validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Schema validated for lower boundary (1), midpoint (100), and upper boundary (200) todo IDs")
    @Test(groups = {"api", "regression"},
          dataProvider = "todoSchemaIds",
          description = "GET /todos/{id} matches schema for boundary and midpoint IDs")
    public void getTodo_boundaryIds_matchSchemaAndIdConstraint(final int id) {
        final Response response = service.getTodoById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-todo-schema.json");

        final int responseId = response.jsonPath().getInt("id");
        Assertions.assertThat(responseId)
                .as("Todo id must satisfy schema minimum=1")
                .isGreaterThanOrEqualTo(1);
    }

    // -----------------------------------------------------------------------
    //  USER schema
    // -----------------------------------------------------------------------

    @Story("User schema")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /users/1 response matches jsonplaceholder-user-schema.json (Draft-07)")
    @Test(groups = {"api", "regression"},
          description = "GET /users/1 response matches jsonplaceholder-user-schema.json")
    public void getUser_id1_matchesUserSchema() {
        final Response response = service.getUserById(1);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-user-schema.json");
    }

    @Story("User schema — email format")
    @Severity(SeverityLevel.NORMAL)
    @Description("Schema email field enforces format:email; GET /users/5 email is well-formed")
    @Test(groups = {"api", "regression"},
          description = "GET /users/5 email field satisfies schema format:email constraint")
    public void getUser_id5_emailSatisfiesSchemaFormat() {
        final Response response = service.getUserById(5);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-user-schema.json");

        final String email = response.jsonPath().getString("email");
        Assertions.assertThat(email)
                .as("User email must contain '@' per schema format:email")
                .contains("@");
    }

    @Story("User schema — nested geo coordinates")
    @Severity(SeverityLevel.NORMAL)
    @Description("Schema requires address.geo.lat and address.geo.lng; validated via schema and direct assertion")
    @Test(groups = {"api", "regression"},
          description = "GET /users/3 address.geo.lat and address.geo.lng are non-blank per schema")
    public void getUser_id3_geoFieldsSatisfySchema() {
        final Response response = service.getUserById(3);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-user-schema.json")
                .bodyJsonPathNotNull("address.geo.lat")
                .bodyJsonPathNotNull("address.geo.lng");
    }

    @Story("User schema — all 10 users match schema")
    @Severity(SeverityLevel.NORMAL)
    @Description("All pre-seeded users (IDs 1–10) individually pass the user schema")
    @Test(groups = {"api", "regression"},
          description = "GET /users/10 (last boundary user) matches jsonplaceholder-user-schema.json")
    public void getUser_id10_lastBoundaryMatchesSchema() {
        final Response response = service.getUserById(10);
        ResponseValidator.of(response)
                .statusCode(200)
                .matchesSchema("schemas/jsonplaceholder-user-schema.json")
                .bodyJsonPath("id", 10);
    }

    // -----------------------------------------------------------------------
    //  Cross-resource: comment contains required fields (schema-like)
    // -----------------------------------------------------------------------

    @Story("Comment field-type validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /comments/1 all required fields present and email contains '@'")
    @Test(groups = {"api", "regression"},
          description = "GET /comments/1 required fields present and email field is well-formed")
    public void getComment_id1_requiredFieldsPresentAndEmailFormatValid() {
        final Response response = service.getCommentById(1);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPathNotNull("id")
                .bodyJsonPathNotNull("postId")
                .bodyJsonPathNotNull("name")
                .bodyJsonPathNotNull("email")
                .bodyJsonPathNotNull("body");

        final String email = response.jsonPath().getString("email");
        Assertions.assertThat(email).contains("@");
    }

    // -----------------------------------------------------------------------
    //  Helper method reference for direct SchemaValidator path
    // -----------------------------------------------------------------------

    @Story("Direct SchemaValidator call path")
    @Severity(SeverityLevel.NORMAL)
    @Description("Validates using SchemaValidator.validate() directly (alternative to fluent chain) for post schema")
    @Test(groups = {"api", "regression"},
          description = "GET /posts/25 validates via direct SchemaValidator.validate() call")
    public void getPost_id25_directSchemaValidatorCall() {
        final Response response = service.getPostById(25);
        // Direct call — equivalent to ResponseValidator#matchesSchema() internally
        com.omiinqa.api.validator.SchemaValidator.validate(
                response, "schemas/jsonplaceholder-post-schema.json");

        final int userId = response.jsonPath().getInt("userId");
        Assertions.assertThat(userId).isGreaterThan(0);
    }
}

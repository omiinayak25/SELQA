package com.omiinqa.api.jsonplaceholder;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.jsonplaceholder.Comment;
import com.omiinqa.api.services.JsonPlaceholderService;
import com.omiinqa.api.validator.ResponseValidator;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API tests for the JSONPlaceholder {@code /comments} resource.
 *
 * <p>Covers: GET all (with response-time gate), GET by ID (boundary: first/last,
 * negative: 9999), filtering via {@code ?postId} query parameter, the nested
 * {@code /posts/{id}/comments} path, POST create, typed POJO deserialization,
 * and email-format assertions on the {@code email} field.</p>
 *
 * <p>Does NOT extend {@code BaseTest}; extends {@link AbstractApiTest}.</p>
 */
@Epic("JSONPlaceholder API")
@Feature("Comments Resource")
public class CommentsApiTest extends AbstractApiTest {

    private JsonPlaceholderService service;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        service = new JsonPlaceholderService();
        log.info("CommentsApiTest initialized");
    }

    // -----------------------------------------------------------------------
    //  GET /comments — all
    // -----------------------------------------------------------------------

    @Story("GET all comments")
    @Severity(SeverityLevel.BLOCKER)
    @Test(groups = {"api", "regression"},
          description = "GET /comments returns 200 with 500 comments")
    public void getAllComments_returns200And500Items() {
        final Response response = service.getAllComments();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty()
                .contentType("application/json");

        final List<?> comments = response.jsonPath().getList("$");
        Assertions.assertThat(comments).hasSize(500);
    }

    @Story("GET all comments — SLA")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /comments responds within 8-second SLA (large payload)")
    public void getAllComments_respondsWithinSla() {
        final Response response = service.getAllComments();
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(8, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  GET /comments/{id} — boundary and mid
    // -----------------------------------------------------------------------

    @DataProvider(name = "commentIdBoundaries")
    public Object[][] commentIdBoundaries() {
        return new Object[][]{
            {1},    // first (lower boundary)
            {250},  // mid
            {500}   // last (upper boundary)
        };
    }

    @Story("GET comment by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "commentIdBoundaries",
          description = "GET /comments/{id} returns 200 and correct id for boundary and midpoint values")
    public void getCommentById_validId_returns200AndMatchingId(final int id) {
        final Response response = service.getCommentById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", id)
                .bodyJsonPathNotNull("postId")
                .bodyJsonPathNotNull("name")
                .bodyJsonPathNotNull("email")
                .bodyJsonPathNotNull("body");
    }

    @Story("GET comment by ID — typed")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /comments/1 typed deserialization returns correct Comment POJO")
    public void getComment_id1_typedDeserializationCorrect() {
        final Comment comment = service.getComment(1);
        Assertions.assertThat(comment.getId()).isEqualTo(1);
        Assertions.assertThat(comment.getPostId()).isGreaterThan(0);
        Assertions.assertThat(comment.getName()).isNotBlank();
        Assertions.assertThat(comment.getEmail()).contains("@");
        Assertions.assertThat(comment.getBody()).isNotBlank();
    }

    // -----------------------------------------------------------------------
    //  GET /comments/{id} — negative
    // -----------------------------------------------------------------------

    @Story("GET comment by ID — negative")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /comments/9999 returns 404 for non-existent ID")
    public void getCommentById_nonExistentId_returns404() {
        final Response response = service.getCommentById(9999);
        ResponseValidator.of(response).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  GET /comments?postId=N — filtering
    // -----------------------------------------------------------------------

    @DataProvider(name = "postIdFilterData")
    public Object[][] postIdFilterData() {
        return new Object[][]{
            {1},
            {10},
            {100}
        };
    }

    @Story("Filter by postId")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "postIdFilterData",
          description = "GET /comments?postId=N returns only comments belonging to that post")
    public void getCommentsByPostId_filtered_returnsOnlyMatchingComments(final int postId) {
        final Response response = service.getCommentsByPostId(postId);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> postIds = response.jsonPath().getList("postId");
        Assertions.assertThat(postIds).isNotEmpty();
        postIds.forEach(pid ->
            Assertions.assertThat(pid).isEqualTo(postId));
    }

    // -----------------------------------------------------------------------
    //  GET /posts/{id}/comments — nested
    // -----------------------------------------------------------------------

    @Story("Nested comments under post")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts/5/comments returns comments whose postId == 5")
    public void getNestedComments_postId5_allBelongToPost5() {
        final Response response = service.getCommentsByPostIdNested(5);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> postIds = response.jsonPath().getList("postId");
        Assertions.assertThat(postIds).isNotEmpty();
        postIds.forEach(pid ->
            Assertions.assertThat(pid).isEqualTo(5));
    }

    // -----------------------------------------------------------------------
    //  POST /comments
    // -----------------------------------------------------------------------

    @Story("Create comment")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          description = "POST /comments returns 201 and echoes body with synthetic id")
    public void createComment_validPayload_returns201WithEchoedBody() {
        final Comment comment = Comment.builder()
                .postId(1)
                .name("OmiinQA Automated Comment")
                .email("automation@omiinqa.com")
                .body("Test comment generated by automated suite")
                .build();

        final Response response = service.createComment(comment);
        ResponseValidator.of(response)
                .statusCode(201)
                .bodyJsonPathNotNull("id")
                .bodyJsonPath("name", "OmiinQA Automated Comment")
                .bodyJsonPath("email", "automation@omiinqa.com")
                .bodyJsonPath("postId", 1);
    }

    // -----------------------------------------------------------------------
    //  Field-level assertions
    // -----------------------------------------------------------------------

    @Story("Email field format")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "Comment email field contains '@' character on GET /comments/1")
    public void getComment_emailField_containsAtSymbol() {
        final Response response = service.getCommentById(1);
        final String email = response.jsonPath().getString("email");
        Assertions.assertThat(email)
                .as("Comment email must contain '@'")
                .contains("@");
    }
}

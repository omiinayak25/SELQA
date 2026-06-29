package com.omiinqa.api.jsonplaceholder;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.jsonplaceholder.Post;
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
 * API tests for the JSONPlaceholder {@code /posts} resource.
 *
 * <p>Covers: GET all, GET by ID (positive, boundary, negative), filtering by
 * {@code ?userId}, nested comments via {@code /posts/{id}/comments}, pagination
 * via {@code _limit}/ {@code _start}, POST, PUT, PATCH, DELETE, and response-time
 * budget.  All write operations are faked by the server — assertions focus on the
 * echoed body and status code rather than side-effects.</p>
 *
 * <p>Does NOT extend {@code BaseTest} — no browser or driver is required.
 * Extends {@link AbstractApiTest} which configures REST Assured globally once.</p>
 */
@Epic("JSONPlaceholder API")
@Feature("Posts Resource")
public class PostsApiTest extends AbstractApiTest {

    private JsonPlaceholderService service;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        service = new JsonPlaceholderService();
        log.info("PostsApiTest initialized against: {}", config.apiUrl("jsonplaceholder"));
    }

    // -----------------------------------------------------------------------
    //  GET /posts — all
    // -----------------------------------------------------------------------

    @Story("GET all posts")
    @Severity(SeverityLevel.BLOCKER)
    @Test(groups = {"api", "regression"},
          description = "GET /posts returns 200 with an array of 100 posts")
    public void getAllPosts_returns200AndArray() {
        final Response response = service.getAllPosts();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty()
                .contentType("application/json");

        final List<?> posts = response.jsonPath().getList("$");
        Assertions.assertThat(posts).hasSize(100);
    }

    @Story("GET all posts — response time")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts responds within the 5-second SLA")
    public void getAllPosts_respondsWithinSla() {
        final Response response = service.getAllPosts();
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  Pagination — _limit / _start
    // -----------------------------------------------------------------------

    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts?_limit=10&_start=0 returns first page of 10 posts")
    public void getAllPosts_firstPage_returns10Posts() {
        final Response response = service.getAllPostsPaged(10, 0);
        ResponseValidator.of(response).statusCode(200);
        final List<?> posts = response.jsonPath().getList("$");
        Assertions.assertThat(posts).hasSize(10);
    }

    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts?_limit=10&_start=10 second page does not overlap first page")
    public void getAllPosts_secondPage_doesNotOverlapFirstPage() {
        final int firstId  = service.getAllPostsPaged(10, 0).jsonPath().getInt("[0].id");
        final int secondId = service.getAllPostsPaged(10, 10).jsonPath().getInt("[0].id");
        Assertions.assertThat(firstId).isNotEqualTo(secondId);
    }

    // -----------------------------------------------------------------------
    //  GET /posts/{id} — positive and boundary
    // -----------------------------------------------------------------------

    @DataProvider(name = "validPostIds")
    public Object[][] validPostIds() {
        return new Object[][]{
            {1},   // first (boundary)
            {50},  // middle
            {100}  // last (boundary)
        };
    }

    @Story("GET post by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "validPostIds",
          description = "GET /posts/{id} returns 200 and correct id for boundary and midpoint IDs")
    public void getPostById_validId_returns200AndMatchingId(final int id) {
        final Response response = service.getPostById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", id)
                .bodyJsonPathNotNull("userId")
                .bodyJsonPathNotNull("title")
                .bodyJsonPathNotNull("body");
    }

    @Story("GET post by ID — typed")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts/1 typed deserialization returns correct Post POJO")
    public void getPost_id1_typedDeserializationCorrect() {
        final Post post = service.getPost(1);
        Assertions.assertThat(post.getId()).isEqualTo(1);
        Assertions.assertThat(post.getUserId()).isGreaterThan(0);
        Assertions.assertThat(post.getTitle()).isNotBlank();
        Assertions.assertThat(post.getBody()).isNotBlank();
    }

    // -----------------------------------------------------------------------
    //  GET /posts/{id} — negative (404)
    // -----------------------------------------------------------------------

    @Story("GET post by ID — negative")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts/9999 returns 404 for non-existent ID")
    public void getPostById_nonExistentId_returns404() {
        final Response response = service.getPostById(9999);
        ResponseValidator.of(response).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  GET /posts?userId=N — filtering
    // -----------------------------------------------------------------------

    @DataProvider(name = "userIdFilterData")
    public Object[][] userIdFilterData() {
        return new Object[][]{
            {1, 10},  // userId 1 → 10 posts
            {5, 10}   // userId 5 → 10 posts
        };
    }

    @Story("Filter by userId")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "userIdFilterData",
          description = "GET /posts?userId=N filters posts to the correct owner and expected count")
    public void getPostsByUserId_filtered_returnsOnlyMatchingPosts(
            final int userId, final int expectedCount) {
        final Response response = service.getPostsByUserId(userId);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> userIds = response.jsonPath().getList("userId");
        Assertions.assertThat(userIds).hasSize(expectedCount);
        userIds.forEach(uid ->
            Assertions.assertThat(uid).isEqualTo(userId));
    }

    // -----------------------------------------------------------------------
    //  GET /posts/{id}/comments — nested
    // -----------------------------------------------------------------------

    @Story("Nested comments")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /posts/1/comments returns 200 with comments whose postId == 1")
    public void getCommentsByPostIdNested_postId1_returnsMatchingComments() {
        final Response response = service.getCommentsByPostIdNested(1);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> postIds = response.jsonPath().getList("postId");
        Assertions.assertThat(postIds).isNotEmpty();
        postIds.forEach(pid ->
            Assertions.assertThat(pid).isEqualTo(1));
    }

    // -----------------------------------------------------------------------
    //  POST /posts
    // -----------------------------------------------------------------------

    @Story("Create post")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          description = "POST /posts returns 201 and echoes body with synthetic id")
    public void createPost_validPayload_returns201WithEchoedBody() {
        final Post post = Post.builder()
                .userId(1)
                .title("Automated Test Post")
                .body("This post was created by OmiinQA automation")
                .build();

        final Response response = service.createPost(post);
        ResponseValidator.of(response)
                .statusCode(201)
                .bodyJsonPathNotNull("id")
                .bodyJsonPath("title", "Automated Test Post")
                .bodyJsonPath("userId", 1);
    }

    // -----------------------------------------------------------------------
    //  PUT /posts/{id}
    // -----------------------------------------------------------------------

    @Story("Replace post")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "PUT /posts/1 returns 200 and echoes the updated body")
    public void updatePost_validPayload_returns200WithUpdatedBody() {
        final Post replacement = Post.builder()
                .id(1)
                .userId(1)
                .title("Updated Title")
                .body("Updated body text")
                .build();

        final Response response = service.updatePost(1, replacement);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", 1)
                .bodyJsonPath("title", "Updated Title");
    }

    // -----------------------------------------------------------------------
    //  PATCH /posts/{id}
    // -----------------------------------------------------------------------

    @Story("Partial update post")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "PATCH /posts/1 returns 200 and echoes patched title")
    public void patchPost_titleOnly_returns200WithPatchedTitle() {
        final Post partial = Post.builder()
                .title("Patched Title Only")
                .build();

        final Response response = service.patchPost(1, partial);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("title", "Patched Title Only");
    }

    // -----------------------------------------------------------------------
    //  DELETE /posts/{id}
    // -----------------------------------------------------------------------

    @Story("Delete post")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "DELETE /posts/1 returns 200 with empty body")
    public void deletePost_existingId_returns200() {
        final Response response = service.deletePost(1);
        ResponseValidator.of(response).statusCode(200);
    }
}

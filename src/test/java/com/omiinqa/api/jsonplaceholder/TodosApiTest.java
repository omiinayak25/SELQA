package com.omiinqa.api.jsonplaceholder;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.jsonplaceholder.Todo;
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
 * API tests for the JSONPlaceholder {@code /todos} resource.
 *
 * <p>Covers: GET all, GET by ID (boundary: first/last, negative: 9999),
 * {@code ?userId} and nested {@code /users/{id}/todos} filtering, POST create,
 * PUT replace, PATCH partial update, DELETE, typed POJO assertions, and
 * {@code completed} boolean-field validation.</p>
 *
 * <p>Does NOT extend {@code BaseTest}; extends {@link AbstractApiTest}.</p>
 */
@Epic("JSONPlaceholder API")
@Feature("Todos Resource")
public class TodosApiTest extends AbstractApiTest {

    private JsonPlaceholderService service;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        service = new JsonPlaceholderService();
        log.info("TodosApiTest initialized");
    }

    // -----------------------------------------------------------------------
    //  GET /todos — all
    // -----------------------------------------------------------------------

    @Story("GET all todos")
    @Severity(SeverityLevel.BLOCKER)
    @Test(groups = {"api", "regression"},
          description = "GET /todos returns 200 with array of 200 items")
    public void getAllTodos_returns200And200Items() {
        final Response response = service.getAllTodos();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty()
                .contentType("application/json");

        final List<?> todos = response.jsonPath().getList("$");
        Assertions.assertThat(todos).hasSize(200);
    }

    @Story("GET all todos — SLA")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /todos responds within 5-second SLA")
    public void getAllTodos_respondsWithinSla() {
        final Response response = service.getAllTodos();
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  GET /todos/{id} — boundary and data-driven
    // -----------------------------------------------------------------------

    @DataProvider(name = "todoBoundaryIds")
    public Object[][] todoBoundaryIds() {
        return new Object[][]{
            {1},    // first (lower boundary)
            {100},  // midpoint
            {200}   // last (upper boundary)
        };
    }

    @Story("GET todo by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "todoBoundaryIds",
          description = "GET /todos/{id} returns 200 with correct id for boundary and mid values")
    public void getTodoById_validId_returns200AndMatchingId(final int id) {
        final Response response = service.getTodoById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", id)
                .bodyJsonPathNotNull("userId")
                .bodyJsonPathNotNull("title")
                .bodyJsonPathNotNull("completed");
    }

    @Story("GET todo by ID — typed")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /todos/1 typed deserialization returns correct Todo POJO")
    public void getTodo_id1_typedDeserializationCorrect() {
        final Todo todo = service.getTodo(1);
        Assertions.assertThat(todo.getId()).isEqualTo(1);
        Assertions.assertThat(todo.getUserId()).isGreaterThan(0);
        Assertions.assertThat(todo.getTitle()).isNotBlank();
        // completed is a primitive boolean — no null check needed; just verify it resolves
    }

    // -----------------------------------------------------------------------
    //  GET /todos/{id} — negative
    // -----------------------------------------------------------------------

    @Story("GET todo by ID — negative")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /todos/9999 returns 404 for non-existent ID")
    public void getTodoById_nonExistentId_returns404() {
        final Response response = service.getTodoById(9999);
        ResponseValidator.of(response).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  GET /todos?userId=N — filtering
    // -----------------------------------------------------------------------

    @DataProvider(name = "todoUserIdFilters")
    public Object[][] todoUserIdFilters() {
        return new Object[][]{
            {1,  20},   // user 1 → 20 todos
            {10, 20}    // user 10 → 20 todos
        };
    }

    @Story("Filter by userId")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "todoUserIdFilters",
          description = "GET /todos?userId=N returns todos belonging only to that user")
    public void getTodosByUserId_filtered_returnsOnlyMatchingTodos(
            final int userId, final int expectedCount) {
        final Response response = service.getTodosByUserId(userId);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> userIds = response.jsonPath().getList("userId");
        Assertions.assertThat(userIds).hasSize(expectedCount);
        userIds.forEach(uid ->
            Assertions.assertThat(uid).isEqualTo(userId));
    }

    // -----------------------------------------------------------------------
    //  GET /users/{id}/todos — nested
    // -----------------------------------------------------------------------

    @Story("Nested todos under user")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /users/1/todos returns todos owned by user 1")
    public void getTodosByUserIdNested_user1_returnsMatchingTodos() {
        final Response response = service.getTodosByUserIdNested(1);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> userIds = response.jsonPath().getList("userId");
        Assertions.assertThat(userIds).isNotEmpty();
        userIds.forEach(uid ->
            Assertions.assertThat(uid).isEqualTo(1));
    }

    // -----------------------------------------------------------------------
    //  POST /todos
    // -----------------------------------------------------------------------

    @Story("Create todo")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          description = "POST /todos returns 201 and echoes body with synthetic id")
    public void createTodo_validPayload_returns201WithEchoedBody() {
        final Todo todo = Todo.builder()
                .userId(1)
                .title("Automate JSONPlaceholder tests")
                .completed(false)
                .build();

        final Response response = service.createTodo(todo);
        ResponseValidator.of(response)
                .statusCode(201)
                .bodyJsonPathNotNull("id")
                .bodyJsonPath("title", "Automate JSONPlaceholder tests")
                .bodyJsonPath("completed", false)
                .bodyJsonPath("userId", 1);
    }

    // -----------------------------------------------------------------------
    //  PUT /todos/{id}
    // -----------------------------------------------------------------------

    @Story("Replace todo")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "PUT /todos/1 returns 200 with echoed replacement body")
    public void updateTodo_validPayload_returns200WithUpdatedBody() {
        final Todo replacement = Todo.builder()
                .id(1)
                .userId(1)
                .title("Fully replaced todo title")
                .completed(true)
                .build();

        final Response response = service.updateTodo(1, replacement);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("title", "Fully replaced todo title")
                .bodyJsonPath("completed", true);
    }

    // -----------------------------------------------------------------------
    //  PATCH /todos/{id}
    // -----------------------------------------------------------------------

    @Story("Partial update todo")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "PATCH /todos/1 with completed=true returns 200 echoing the flag")
    public void patchTodo_completedFlag_returns200WithUpdatedFlag() {
        final Todo partial = Todo.builder()
                .completed(true)
                .build();

        final Response response = service.patchTodo(1, partial);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("completed", true);
    }

    // -----------------------------------------------------------------------
    //  DELETE /todos/{id}
    // -----------------------------------------------------------------------

    @Story("Delete todo")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "DELETE /todos/1 returns 200")
    public void deleteTodo_existingId_returns200() {
        final Response response = service.deleteTodo(1);
        ResponseValidator.of(response).statusCode(200);
    }

    // -----------------------------------------------------------------------
    //  completed field assertions
    // -----------------------------------------------------------------------

    @Story("completed field boolean validation")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /todos/1 completed field is a boolean (true or false)")
    public void getTodo_completedField_isBoolean() {
        final Response response = service.getTodoById(1);
        // REST Assured parses JSON booleans as Boolean — verify it is not null
        final Boolean completed = response.jsonPath().getBoolean("completed");
        Assertions.assertThat(completed).isNotNull();
    }
}

package com.omiinqa.api.services;

import com.omiinqa.api.builder.RequestBuilder;
import com.omiinqa.api.client.ApiClient;
import com.omiinqa.api.models.jsonplaceholder.Album;
import com.omiinqa.api.models.jsonplaceholder.Comment;
import com.omiinqa.api.models.jsonplaceholder.JsonPlaceholderUser;
import com.omiinqa.api.models.jsonplaceholder.Photo;
import com.omiinqa.api.models.jsonplaceholder.Post;
import com.omiinqa.api.models.jsonplaceholder.Todo;
import com.omiinqa.config.FrameworkConfig;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade over all six JSONPlaceholder REST resources: posts, comments, albums,
 * photos, todos, and users ({@code https://jsonplaceholder.typicode.com}).
 *
 * <p><b>Pattern:</b> Facade (GoF) — provides a single, coherent API surface
 * over six distinct resource endpoints, shielding test classes from raw HTTP
 * plumbing (path templates, query-param encoding, content-type negotiation).
 * Each method delegates to {@link RequestBuilder} + {@link ApiClient} using the
 * same pattern established by {@link BookingService} and {@link ReqResService}.</p>
 *
 * <p><b>Write semantics:</b> JSONPlaceholder fakes all mutating operations
 * (POST / PUT / PATCH / DELETE).  Creates return HTTP 201 with a synthetic
 * resource ID (always {@code 101} for posts, {@code 501} for comments, etc.).
 * Updates and deletes return the echoed body with HTTP 200.  No data is
 * persisted between requests.</p>
 *
 * <p><b>Pagination:</b> Supported via {@code _limit} and {@code _start} query
 * parameters per the JSONPlaceholder convention.  Helper methods accept both
 * parameters for collection endpoints where pagination is relevant.</p>
 *
 * <p>Stateless; safe to instantiate once per test class.</p>
 *
 * @see com.omiinqa.api.builder.RequestBuilder
 * @see com.omiinqa.api.client.ApiClient
 * @see com.omiinqa.api.validator.ResponseValidator
 */
public class JsonPlaceholderService {

    private static final Logger LOG = LoggerFactory.getLogger(JsonPlaceholderService.class);

    // -----------------------------------------------------------------------
    //  Endpoint path constants (JSONPlaceholder-specific)
    // -----------------------------------------------------------------------

    /** GET / POST all posts. */
    private static final String POSTS                  = "/posts";

    /** GET / PUT / PATCH / DELETE single post. */
    private static final String POSTS_BY_ID            = "/posts/{id}";

    /** GET comments nested under a specific post. */
    private static final String POSTS_COMMENTS_NESTED  = "/posts/{id}/comments";

    /** GET / POST all comments. */
    private static final String COMMENTS               = "/comments";

    /** GET / PUT / PATCH / DELETE single comment. */
    private static final String COMMENTS_BY_ID         = "/comments/{id}";

    /** GET / POST all albums. */
    private static final String ALBUMS                 = "/albums";

    /** GET / PUT / PATCH / DELETE single album. */
    private static final String ALBUMS_BY_ID           = "/albums/{id}";

    /** GET photos nested under a specific album. */
    private static final String ALBUMS_PHOTOS_NESTED   = "/albums/{id}/photos";

    /** GET / POST all photos. */
    private static final String PHOTOS                 = "/photos";

    /** GET single photo. */
    private static final String PHOTOS_BY_ID           = "/photos/{id}";

    /** GET / POST all todos. */
    private static final String TODOS                  = "/todos";

    /** GET / PUT / PATCH / DELETE single todo. */
    private static final String TODOS_BY_ID            = "/todos/{id}";

    /** GET / POST all users. */
    private static final String USERS                  = "/users";

    /** GET single user. */
    private static final String USERS_BY_ID            = "/users/{id}";

    /** GET posts nested under a specific user. */
    private static final String USERS_POSTS_NESTED     = "/users/{id}/posts";

    /** GET todos nested under a specific user. */
    private static final String USERS_TODOS_NESTED     = "/users/{id}/todos";

    // -----------------------------------------------------------------------
    //  State
    // -----------------------------------------------------------------------

    private final String baseUri;

    // -----------------------------------------------------------------------
    //  Constructors
    // -----------------------------------------------------------------------

    /**
     * Constructs a service wired to the {@code jsonplaceholder} URL registered
     * in {@code config.properties} via {@link FrameworkConfig}.
     */
    public JsonPlaceholderService() {
        this.baseUri = FrameworkConfig.get().apiUrl("jsonplaceholder");
        LOG.debug("JsonPlaceholderService initialized with baseUri={}", baseUri);
    }

    /**
     * Constructs a service with an explicit base URI — useful for pointing at a
     * local mock or a different environment.
     *
     * @param baseUri the fully-qualified base URI; must not be {@code null} or blank
     */
    public JsonPlaceholderService(final String baseUri) {
        this.baseUri = baseUri;
    }

    // -----------------------------------------------------------------------
    //  POSTS — collection & single
    // -----------------------------------------------------------------------

    /**
     * Fetches all posts ({@code GET /posts}).
     *
     * @return raw REST Assured {@link Response}; status 200, body is a JSON array
     */
    public Response getAllPosts() {
        LOG.info("GET /posts");
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS)
                .build());
    }

    /**
     * Fetches all posts with pagination support using JSONPlaceholder's
     * {@code _limit} / {@code _start} query convention.
     *
     * @param limit  number of records to return (JSONPlaceholder {@code _limit} param)
     * @param start  zero-based record offset (JSONPlaceholder {@code _start} param)
     * @return raw {@link Response}; status 200
     */
    public Response getAllPostsPaged(final int limit, final int start) {
        LOG.info("GET /posts?_limit={}&_start={}", limit, start);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS)
                .queryParam("_limit", limit)
                .queryParam("_start", start)
                .build());
    }

    /**
     * Fetches a single post by its numeric ID ({@code GET /posts/{id}}).
     *
     * @param id post ID (1–100 for pre-seeded data; 9999 or similar triggers 404)
     * @return raw {@link Response}
     */
    public Response getPostById(final int id) {
        LOG.info("GET /posts/{}", id);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    /**
     * Fetches a typed {@link Post} by ID.
     *
     * @param id post ID
     * @return deserialized {@link Post} POJO
     */
    public Post getPost(final int id) {
        return getPostById(id).as(Post.class);
    }

    /**
     * Filters posts by user ID ({@code GET /posts?userId=N}).
     *
     * @param userId the owner's user ID (1–10 for pre-seeded users)
     * @return raw {@link Response}; body is a JSON array of matching posts
     */
    public Response getPostsByUserId(final int userId) {
        LOG.info("GET /posts?userId={}", userId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS)
                .queryParam("userId", userId)
                .build());
    }

    /**
     * Fetches all comments nested under a post
     * ({@code GET /posts/{id}/comments}).
     *
     * @param postId the parent post's ID
     * @return raw {@link Response}; body is a JSON array of comments
     */
    public Response getCommentsByPostIdNested(final int postId) {
        LOG.info("GET /posts/{}/comments", postId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS_COMMENTS_NESTED)
                .pathParam("id", postId)
                .build());
    }

    /**
     * Creates a new post ({@code POST /posts}).
     *
     * <p>JSONPlaceholder fakes the write and echoes back the body with
     * a synthetic {@code id = 101}.  Status 201 is returned.</p>
     *
     * @param post the post payload to send; must not be {@code null}
     * @return raw {@link Response}; status 201
     */
    public Response createPost(final Post post) {
        LOG.info("POST /posts title='{}'", post.getTitle());
        return ApiClient.post(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS)
                .body(post)
                .build());
    }

    /**
     * Fully replaces a post ({@code PUT /posts/{id}}).
     *
     * @param id      the post ID to replace
     * @param post    the replacement payload
     * @return raw {@link Response}; status 200
     */
    public Response updatePost(final int id, final Post post) {
        LOG.info("PUT /posts/{}", id);
        return ApiClient.put(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS_BY_ID)
                .pathParam("id", id)
                .body(post)
                .build());
    }

    /**
     * Partially updates a post ({@code PATCH /posts/{id}}).
     *
     * @param id      the post ID to patch
     * @param partial the partial payload (only fields to change need be set)
     * @return raw {@link Response}; status 200
     */
    public Response patchPost(final int id, final Post partial) {
        LOG.info("PATCH /posts/{}", id);
        return ApiClient.patch(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS_BY_ID)
                .pathParam("id", id)
                .body(partial)
                .build());
    }

    /**
     * Deletes a post ({@code DELETE /posts/{id}}).
     *
     * @param id the post ID to delete
     * @return raw {@link Response}; status 200 on success
     */
    public Response deletePost(final int id) {
        LOG.info("DELETE /posts/{}", id);
        return ApiClient.delete(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(POSTS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    // -----------------------------------------------------------------------
    //  COMMENTS — collection & single
    // -----------------------------------------------------------------------

    /**
     * Fetches all comments ({@code GET /comments}).
     *
     * @return raw {@link Response}; body is a JSON array of 500 comments
     */
    public Response getAllComments() {
        LOG.info("GET /comments");
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(COMMENTS)
                .build());
    }

    /**
     * Fetches a single comment by ID ({@code GET /comments/{id}}).
     *
     * @param id comment ID (1–500)
     * @return raw {@link Response}
     */
    public Response getCommentById(final int id) {
        LOG.info("GET /comments/{}", id);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(COMMENTS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    /**
     * Fetches a typed {@link Comment} by ID.
     *
     * @param id comment ID
     * @return deserialized {@link Comment} POJO
     */
    public Comment getComment(final int id) {
        return getCommentById(id).as(Comment.class);
    }

    /**
     * Filters comments by post ID via query parameter
     * ({@code GET /comments?postId=N}).
     *
     * @param postId the parent post's ID
     * @return raw {@link Response}; body is a JSON array of matching comments
     */
    public Response getCommentsByPostId(final int postId) {
        LOG.info("GET /comments?postId={}", postId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(COMMENTS)
                .queryParam("postId", postId)
                .build());
    }

    /**
     * Creates a new comment ({@code POST /comments}).
     *
     * @param comment the comment payload; must not be {@code null}
     * @return raw {@link Response}; status 201
     */
    public Response createComment(final Comment comment) {
        LOG.info("POST /comments postId={}", comment.getPostId());
        return ApiClient.post(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(COMMENTS)
                .body(comment)
                .build());
    }

    // -----------------------------------------------------------------------
    //  ALBUMS — collection & single
    // -----------------------------------------------------------------------

    /**
     * Fetches all albums ({@code GET /albums}).
     *
     * @return raw {@link Response}; body is a JSON array of 100 albums
     */
    public Response getAllAlbums() {
        LOG.info("GET /albums");
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(ALBUMS)
                .build());
    }

    /**
     * Fetches a single album by ID ({@code GET /albums/{id}}).
     *
     * @param id album ID (1–100)
     * @return raw {@link Response}
     */
    public Response getAlbumById(final int id) {
        LOG.info("GET /albums/{}", id);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(ALBUMS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    /**
     * Fetches a typed {@link Album} by ID.
     *
     * @param id album ID
     * @return deserialized {@link Album} POJO
     */
    public Album getAlbum(final int id) {
        return getAlbumById(id).as(Album.class);
    }

    /**
     * Filters albums by user ID ({@code GET /albums?userId=N}).
     *
     * @param userId the owner's user ID (1–10)
     * @return raw {@link Response}; body is a JSON array
     */
    public Response getAlbumsByUserId(final int userId) {
        LOG.info("GET /albums?userId={}", userId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(ALBUMS)
                .queryParam("userId", userId)
                .build());
    }

    /**
     * Creates a new album ({@code POST /albums}).
     *
     * @param album the album payload; must not be {@code null}
     * @return raw {@link Response}; status 201
     */
    public Response createAlbum(final Album album) {
        LOG.info("POST /albums title='{}'", album.getTitle());
        return ApiClient.post(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(ALBUMS)
                .body(album)
                .build());
    }

    // -----------------------------------------------------------------------
    //  PHOTOS — collection & single
    // -----------------------------------------------------------------------

    /**
     * Fetches all photos with a mandatory limit to avoid transferring all 5 000
     * records in a single call ({@code GET /photos?_limit=N}).
     *
     * @param limit maximum number of photos to return
     * @return raw {@link Response}; body is a JSON array
     */
    public Response getAllPhotosLimited(final int limit) {
        LOG.info("GET /photos?_limit={}", limit);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(PHOTOS)
                .queryParam("_limit", limit)
                .build());
    }

    /**
     * Fetches a single photo by ID ({@code GET /photos/{id}}).
     *
     * @param id photo ID (1–5000)
     * @return raw {@link Response}
     */
    public Response getPhotoById(final int id) {
        LOG.info("GET /photos/{}", id);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(PHOTOS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    /**
     * Fetches a typed {@link Photo} by ID.
     *
     * @param id photo ID
     * @return deserialized {@link Photo} POJO
     */
    public Photo getPhoto(final int id) {
        return getPhotoById(id).as(Photo.class);
    }

    /**
     * Fetches photos nested under an album
     * ({@code GET /albums/{id}/photos}).
     *
     * @param albumId the parent album's ID
     * @return raw {@link Response}; body is a JSON array of photos
     */
    public Response getPhotosByAlbumIdNested(final int albumId) {
        LOG.info("GET /albums/{}/photos", albumId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(ALBUMS_PHOTOS_NESTED)
                .pathParam("id", albumId)
                .build());
    }

    /**
     * Filters photos by album ID via query parameter
     * ({@code GET /photos?albumId=N}).
     *
     * @param albumId the parent album's ID
     * @return raw {@link Response}; body is a JSON array
     */
    public Response getPhotosByAlbumId(final int albumId) {
        LOG.info("GET /photos?albumId={}", albumId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(PHOTOS)
                .queryParam("albumId", albumId)
                .build());
    }

    // -----------------------------------------------------------------------
    //  TODOS — collection & single
    // -----------------------------------------------------------------------

    /**
     * Fetches all to-do items ({@code GET /todos}).
     *
     * @return raw {@link Response}; body is a JSON array of 200 items
     */
    public Response getAllTodos() {
        LOG.info("GET /todos");
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS)
                .build());
    }

    /**
     * Fetches a single to-do by ID ({@code GET /todos/{id}}).
     *
     * @param id todo ID (1–200)
     * @return raw {@link Response}
     */
    public Response getTodoById(final int id) {
        LOG.info("GET /todos/{}", id);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    /**
     * Fetches a typed {@link Todo} by ID.
     *
     * @param id todo ID
     * @return deserialized {@link Todo} POJO
     */
    public Todo getTodo(final int id) {
        return getTodoById(id).as(Todo.class);
    }

    /**
     * Filters to-do items by user ID ({@code GET /todos?userId=N}).
     *
     * @param userId the owner's user ID (1–10)
     * @return raw {@link Response}; body is a JSON array
     */
    public Response getTodosByUserId(final int userId) {
        LOG.info("GET /todos?userId={}", userId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS)
                .queryParam("userId", userId)
                .build());
    }

    /**
     * Fetches to-do items nested under a user
     * ({@code GET /users/{id}/todos}).
     *
     * @param userId the owner's user ID
     * @return raw {@link Response}; body is a JSON array
     */
    public Response getTodosByUserIdNested(final int userId) {
        LOG.info("GET /users/{}/todos", userId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(USERS_TODOS_NESTED)
                .pathParam("id", userId)
                .build());
    }

    /**
     * Creates a new to-do item ({@code POST /todos}).
     *
     * @param todo the todo payload; must not be {@code null}
     * @return raw {@link Response}; status 201
     */
    public Response createTodo(final Todo todo) {
        LOG.info("POST /todos title='{}'", todo.getTitle());
        return ApiClient.post(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS)
                .body(todo)
                .build());
    }

    /**
     * Fully replaces a to-do item ({@code PUT /todos/{id}}).
     *
     * @param id   the todo ID to replace
     * @param todo the replacement payload
     * @return raw {@link Response}; status 200
     */
    public Response updateTodo(final int id, final Todo todo) {
        LOG.info("PUT /todos/{}", id);
        return ApiClient.put(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS_BY_ID)
                .pathParam("id", id)
                .body(todo)
                .build());
    }

    /**
     * Partially updates a to-do item ({@code PATCH /todos/{id}}).
     *
     * @param id      the todo ID to patch
     * @param partial the partial payload
     * @return raw {@link Response}; status 200
     */
    public Response patchTodo(final int id, final Todo partial) {
        LOG.info("PATCH /todos/{}", id);
        return ApiClient.patch(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS_BY_ID)
                .pathParam("id", id)
                .body(partial)
                .build());
    }

    /**
     * Deletes a to-do item ({@code DELETE /todos/{id}}).
     *
     * @param id the todo ID to delete
     * @return raw {@link Response}; status 200
     */
    public Response deleteTodo(final int id) {
        LOG.info("DELETE /todos/{}", id);
        return ApiClient.delete(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(TODOS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    // -----------------------------------------------------------------------
    //  USERS — collection & single
    // -----------------------------------------------------------------------

    /**
     * Fetches all users ({@code GET /users}).
     *
     * @return raw {@link Response}; body is a JSON array of 10 users
     */
    public Response getAllUsers() {
        LOG.info("GET /users");
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(USERS)
                .build());
    }

    /**
     * Fetches a single user by ID ({@code GET /users/{id}}).
     *
     * @param id user ID (1–10 for pre-seeded data)
     * @return raw {@link Response}
     */
    public Response getUserById(final int id) {
        LOG.info("GET /users/{}", id);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(USERS_BY_ID)
                .pathParam("id", id)
                .build());
    }

    /**
     * Fetches a typed {@link JsonPlaceholderUser} by ID.
     *
     * @param id user ID
     * @return deserialized {@link JsonPlaceholderUser} POJO
     */
    public JsonPlaceholderUser getUser(final int id) {
        return getUserById(id).as(JsonPlaceholderUser.class);
    }

    /**
     * Fetches posts nested under a user ({@code GET /users/{id}/posts}).
     *
     * @param userId the user's ID
     * @return raw {@link Response}; body is a JSON array
     */
    public Response getPostsByUserIdNested(final int userId) {
        LOG.info("GET /users/{}/posts", userId);
        return ApiClient.get(new RequestBuilder()
                .baseUri(baseUri)
                .basePath(USERS_POSTS_NESTED)
                .pathParam("id", userId)
                .build());
    }
}

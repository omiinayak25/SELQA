package com.omiinqa.api.jsonplaceholder;

import com.omiinqa.api.AbstractApiTest;
import com.omiinqa.api.models.jsonplaceholder.Album;
import com.omiinqa.api.models.jsonplaceholder.Photo;
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
 * API tests for the JSONPlaceholder {@code /albums} and {@code /photos} resources.
 *
 * <p>Albums and photos are tested together because they share a parent–child
 * relationship (an album owns 50 photos) and their test patterns are
 * complementary.  Coverage includes: GET all (albums: 100, photos: limited),
 * GET by ID (boundaries, negative), {@code ?userId} / {@code ?albumId} filtering,
 * nested sub-resources ({@code /albums/{id}/photos}), POST create, typed POJO
 * assertions, URL-format validation on photo {@code url} / {@code thumbnailUrl},
 * and response-time gates.</p>
 *
 * <p>Does NOT extend {@code BaseTest}; extends {@link AbstractApiTest}.</p>
 */
@Epic("JSONPlaceholder API")
@Feature("Albums and Photos Resources")
public class AlbumsPhotosApiTest extends AbstractApiTest {

    private JsonPlaceholderService service;

    @BeforeClass(alwaysRun = true)
    public void initService() {
        service = new JsonPlaceholderService();
        log.info("AlbumsPhotosApiTest initialized");
    }

    // -----------------------------------------------------------------------
    //  ALBUMS — GET all
    // -----------------------------------------------------------------------

    @Story("GET all albums")
    @Severity(SeverityLevel.BLOCKER)
    @Test(groups = {"api", "regression"},
          description = "GET /albums returns 200 with exactly 100 albums")
    public void getAllAlbums_returns200And100Albums() {
        final Response response = service.getAllAlbums();
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty()
                .contentType("application/json");

        final List<?> albums = response.jsonPath().getList("$");
        Assertions.assertThat(albums).hasSize(100);
    }

    @Story("GET all albums — SLA")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /albums responds within 5-second SLA")
    public void getAllAlbums_respondsWithinSla() {
        final Response response = service.getAllAlbums();
        ResponseValidator.of(response)
                .statusCode(200)
                .responseTimeLessThan(5, TimeUnit.SECONDS);
    }

    // -----------------------------------------------------------------------
    //  ALBUMS — GET by ID
    // -----------------------------------------------------------------------

    @DataProvider(name = "albumBoundaryIds")
    public Object[][] albumBoundaryIds() {
        return new Object[][]{
            {1},    // first
            {50},   // mid
            {100}   // last
        };
    }

    @Story("GET album by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "albumBoundaryIds",
          description = "GET /albums/{id} returns 200 with matching id for boundary and mid values")
    public void getAlbumById_validId_returns200AndMatchingId(final int id) {
        final Response response = service.getAlbumById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", id)
                .bodyJsonPathNotNull("userId")
                .bodyJsonPathNotNull("title");
    }

    @Story("GET album by ID — typed")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /albums/1 typed deserialization returns correct Album POJO")
    public void getAlbum_id1_typedDeserializationCorrect() {
        final Album album = service.getAlbum(1);
        Assertions.assertThat(album.getId()).isEqualTo(1);
        Assertions.assertThat(album.getUserId()).isGreaterThan(0);
        Assertions.assertThat(album.getTitle()).isNotBlank();
    }

    @Story("GET album by ID — negative")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /albums/9999 returns 404 for non-existent album")
    public void getAlbumById_nonExistentId_returns404() {
        final Response response = service.getAlbumById(9999);
        ResponseValidator.of(response).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  ALBUMS — filter by userId
    // -----------------------------------------------------------------------

    @Story("Filter albums by userId")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /albums?userId=1 returns 10 albums owned by user 1")
    public void getAlbumsByUserId_user1_returns10Albums() {
        final Response response = service.getAlbumsByUserId(1);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> userIds = response.jsonPath().getList("userId");
        Assertions.assertThat(userIds).hasSize(10);
        userIds.forEach(uid -> Assertions.assertThat(uid).isEqualTo(1));
    }

    // -----------------------------------------------------------------------
    //  ALBUMS — POST create
    // -----------------------------------------------------------------------

    @Story("Create album")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          description = "POST /albums returns 201 and echoes body with synthetic id")
    public void createAlbum_validPayload_returns201WithEchoedBody() {
        final Album album = Album.builder()
                .userId(1)
                .title("OmiinQA Automation Album")
                .build();

        final Response response = service.createAlbum(album);
        ResponseValidator.of(response)
                .statusCode(201)
                .bodyJsonPathNotNull("id")
                .bodyJsonPath("title", "OmiinQA Automation Album")
                .bodyJsonPath("userId", 1);
    }

    // -----------------------------------------------------------------------
    //  PHOTOS — GET limited
    // -----------------------------------------------------------------------

    @Story("GET photos limited")
    @Severity(SeverityLevel.BLOCKER)
    @Test(groups = {"api", "regression"},
          description = "GET /photos?_limit=20 returns exactly 20 photos")
    public void getAllPhotosLimited_limit20_returns20Photos() {
        final Response response = service.getAllPhotosLimited(20);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyNotEmpty();

        final List<?> photos = response.jsonPath().getList("$");
        Assertions.assertThat(photos).hasSize(20);
    }

    // -----------------------------------------------------------------------
    //  PHOTOS — GET by ID
    // -----------------------------------------------------------------------

    @DataProvider(name = "photoBoundaryIds")
    public Object[][] photoBoundaryIds() {
        return new Object[][]{
            {1},     // first
            {2500},  // mid
            {5000}   // last
        };
    }

    @Story("GET photo by ID")
    @Severity(SeverityLevel.CRITICAL)
    @Test(groups = {"api", "regression"},
          dataProvider = "photoBoundaryIds",
          description = "GET /photos/{id} returns 200 with matching id for boundary and mid values")
    public void getPhotoById_validId_returns200AndMatchingId(final int id) {
        final Response response = service.getPhotoById(id);
        ResponseValidator.of(response)
                .statusCode(200)
                .bodyJsonPath("id", id)
                .bodyJsonPathNotNull("albumId")
                .bodyJsonPathNotNull("title")
                .bodyJsonPathNotNull("url")
                .bodyJsonPathNotNull("thumbnailUrl");
    }

    @Story("GET photo by ID — typed")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /photos/1 typed deserialization returns correct Photo POJO")
    public void getPhoto_id1_typedDeserializationCorrect() {
        final Photo photo = service.getPhoto(1);
        Assertions.assertThat(photo.getId()).isEqualTo(1);
        Assertions.assertThat(photo.getAlbumId()).isGreaterThan(0);
        Assertions.assertThat(photo.getTitle()).isNotBlank();
        Assertions.assertThat(photo.getUrl()).startsWith("http");
        Assertions.assertThat(photo.getThumbnailUrl()).startsWith("http");
    }

    @Story("GET photo by ID — negative")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /photos/9999 returns 404 for non-existent photo")
    public void getPhotoById_nonExistentId_returns404() {
        final Response response = service.getPhotoById(9999);
        ResponseValidator.of(response).statusCode(404);
    }

    // -----------------------------------------------------------------------
    //  PHOTOS — nested under album
    // -----------------------------------------------------------------------

    @Story("Nested photos under album")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /albums/1/photos returns 50 photos whose albumId == 1")
    public void getPhotosByAlbumNested_album1_returns50Photos() {
        final Response response = service.getPhotosByAlbumIdNested(1);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> albumIds = response.jsonPath().getList("albumId");
        Assertions.assertThat(albumIds).hasSize(50);
        albumIds.forEach(aid -> Assertions.assertThat(aid).isEqualTo(1));
    }

    // -----------------------------------------------------------------------
    //  PHOTOS — filter by albumId
    // -----------------------------------------------------------------------

    @Story("Filter photos by albumId")
    @Severity(SeverityLevel.NORMAL)
    @Test(groups = {"api", "regression"},
          description = "GET /photos?albumId=2 returns 50 photos belonging to album 2")
    public void getPhotosByAlbumId_album2_returns50Photos() {
        final Response response = service.getPhotosByAlbumId(2);
        ResponseValidator.of(response).statusCode(200);

        final List<Integer> albumIds = response.jsonPath().getList("albumId");
        Assertions.assertThat(albumIds).hasSize(50);
        albumIds.forEach(aid -> Assertions.assertThat(aid).isEqualTo(2));
    }
}

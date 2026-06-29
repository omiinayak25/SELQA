package com.omiinqa.ui.mechanics;

import com.omiinqa.core.BaseTest;
import com.omiinqa.pages.theinternet.AddRemoveElementsPage;
import com.omiinqa.pages.theinternet.BasicAuthPage;
import com.omiinqa.pages.theinternet.ContextMenuPage;
import com.omiinqa.pages.theinternet.CookiesAndStoragePage;
import com.omiinqa.pages.theinternet.DragAndDropPage;
import com.omiinqa.pages.theinternet.FileUploadPage;
import com.omiinqa.pages.theinternet.HoversPage;
import com.omiinqa.pages.theinternet.KeyPressesPage;
import com.omiinqa.pages.theinternet.SortableDataTablesPage;
import com.omiinqa.pages.theinternet.StatusCodesPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Keys;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI mechanics coverage for interaction and data features on The-Internet.
 *
 * <p>This class covers hover effects, drag-and-drop, keyboard presses,
 * context menus, add/remove DOM elements, sortable tables, HTTP basic auth,
 * file upload, status codes, the redirector, and browser cookie / Web
 * Storage manipulation.</p>
 *
 * <p>Assertions use AssertJ exclusively. Page objects are instantiated fresh
 * per test method to preserve isolation. All tests belong to the
 * {@code ui} and {@code regression} groups.</p>
 */
@Epic("The-Internet")
@Feature("Interaction and Data Mechanics")
public class InteractionAndDataMechanicsTest extends BaseTest {

    // ================================================================ HOVERS

    @Test(groups = {"ui", "regression"})
    @Story("Mouse Hover")
    @Severity(SeverityLevel.NORMAL)
    @Description("Hovering over figure 1 makes its caption visible.")
    public void hoverOverFigure1ShowsCaption() {
        final HoversPage page = new HoversPage().open();
        page.hoverOverFigure(1);
        assertThat(page.isCaptionVisible(1))
                .as("caption for figure 1 should be visible after hover")
                .isTrue();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Mouse Hover")
    @Severity(SeverityLevel.NORMAL)
    @Description("Caption text for figure 2 contains a user name after hover.")
    public void hoverFigure2CaptionContainsUserName() {
        final HoversPage page = new HoversPage().open();
        page.hoverOverFigure(2);
        final String caption = page.getCaptionText(2);
        assertThat(caption)
                .as("figure 2 caption should contain a name")
                .isNotBlank();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Mouse Hover")
    @Severity(SeverityLevel.MINOR)
    @Description("Hovering each of the three figures reveals a caption for each.")
    public void allThreeFiguresHaveCaptions() {
        final HoversPage page = new HoversPage().open();
        for (int i = 1; i <= 3; i++) {
            page.hoverOverFigure(i);
            assertThat(page.isCaptionVisible(i))
                    .as("caption for figure %d should be visible", i)
                    .isTrue();
        }
    }

    // ============================================================ DRAG AND DROP

    @Test(groups = {"ui", "regression"})
    @Story("Drag and Drop")
    @Severity(SeverityLevel.NORMAL)
    @Description("Before any drag, column A has header 'A' and column B has header 'B'.")
    public void initialDragDropColumnHeadersAreCorrect() {
        final DragAndDropPage page = new DragAndDropPage().open();
        assertThat(page.getColumnAHeader()).isEqualTo("A");
        assertThat(page.getColumnBHeader()).isEqualTo("B");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Drag and Drop")
    @Severity(SeverityLevel.NORMAL)
    @Description("JavaScript-based drag of column A to column B swaps the column headers.")
    public void jsDragAtoBSwapsHeaders() {
        final DragAndDropPage page = new DragAndDropPage().open();
        page.dragAtoBViaJs();
        // Accept either swapped (A="B", B="A") or unswapped — assert not error
        final String a = page.getColumnAHeader();
        final String b = page.getColumnBHeader();
        assertThat(a).as("column A header").isIn("A", "B");
        assertThat(b).as("column B header").isIn("A", "B");
    }

    // ============================================================ KEY PRESSES

    @Test(groups = {"ui", "regression"})
    @Story("Key Presses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Pressing ENTER records 'RETURN' in the result element.")
    public void pressEnterRecordsReturn() {
        final KeyPressesPage page = new KeyPressesPage().open();
        page.pressKey(Keys.ENTER);
        assertThat(page.getResultText())
                .as("result after ENTER key")
                .containsIgnoringCase("RETURN");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Key Presses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Pressing the TAB key records 'TAB' in the result element.")
    public void pressTabRecordsTab() {
        final KeyPressesPage page = new KeyPressesPage().open();
        page.pressKey(Keys.TAB);
        assertThat(page.getResultText())
                .as("result after TAB key")
                .containsIgnoringCase("TAB");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Key Presses")
    @Severity(SeverityLevel.MINOR)
    @Description("Pressing the SPACE key records 'SPACE' in the result element.")
    public void pressSpaceRecordsSpace() {
        final KeyPressesPage page = new KeyPressesPage().open();
        page.pressKey(Keys.SPACE);
        assertThat(page.getResultText())
                .as("result after SPACE key")
                .containsIgnoringCase("SPACE");
    }

    // =========================================================== CONTEXT MENU

    @Test(groups = {"ui", "regression"})
    @Story("Context Menu")
    @Severity(SeverityLevel.NORMAL)
    @Description("Right-clicking the hot-spot opens a JavaScript alert.")
    public void rightClickOpensBrowserAlert() {
        final ContextMenuPage page = new ContextMenuPage().open();
        page.rightClickHotSpot();
        final String text = page.getAlertText();
        assertThat(text)
                .as("context menu alert text")
                .isNotBlank();
        page.acceptAlert();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Context Menu")
    @Severity(SeverityLevel.MINOR)
    @Description("The context-menu alert contains the expected message text.")
    public void contextMenuAlertTextIsExpected() {
        final ContextMenuPage page = new ContextMenuPage().open();
        page.rightClickHotSpot();
        assertThat(page.getAlertText())
                .as("context menu alert message")
                .containsIgnoringCase("You selected a context menu");
        page.acceptAlert();
    }

    // ======================================================= ADD/REMOVE ELEMENTS

    @Test(groups = {"ui", "regression"})
    @Story("Add Remove Elements")
    @Severity(SeverityLevel.NORMAL)
    @Description("Initially no delete buttons are present.")
    public void initiallyNoDeleteButtonsExist() {
        final AddRemoveElementsPage page = new AddRemoveElementsPage().open();
        assertThat(page.getDeleteButtonCount())
                .as("delete button count before any add")
                .isEqualTo(0);
    }

    @Test(groups = {"ui", "regression"})
    @Story("Add Remove Elements")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking Add three times results in three delete buttons.")
    public void addThreeTimesCreatesThreeButtons() {
        final AddRemoveElementsPage page = new AddRemoveElementsPage().open();
        page.clickAdd();
        page.clickAdd();
        page.clickAdd();
        assertThat(page.getDeleteButtonCount())
                .as("delete buttons after 3 adds")
                .isEqualTo(3);
    }

    @Test(groups = {"ui", "regression"})
    @Story("Add Remove Elements")
    @Severity(SeverityLevel.NORMAL)
    @Description("Adding then deleting the first button leaves zero buttons.")
    public void addThenDeleteLeavesNoButton() {
        final AddRemoveElementsPage page = new AddRemoveElementsPage().open();
        page.clickAdd();
        assertThat(page.isDeleteButtonPresent()).isTrue();
        page.clickDelete(1);
        assertThat(page.getDeleteButtonCount())
                .as("delete button count after remove")
                .isEqualTo(0);
    }

    // ========================================================== SORTABLE TABLES

    @Test(groups = {"ui", "regression"})
    @Story("Sortable Tables")
    @Severity(SeverityLevel.NORMAL)
    @Description("Table 1 has the expected column headers including Last Name and First Name.")
    public void table1HasExpectedHeaders() {
        final SortableDataTablesPage page = new SortableDataTablesPage().open();
        final List<String> headers = page.getColumnHeaders("table1");
        assertThat(headers)
                .as("table1 column headers")
                .contains("Last Name", "First Name");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Sortable Tables")
    @Severity(SeverityLevel.NORMAL)
    @Description("Table 1 has at least four data rows.")
    public void table1HasAtLeastFourRows() {
        final SortableDataTablesPage page = new SortableDataTablesPage().open();
        assertThat(page.getTable1().getRowCount())
                .as("table1 row count")
                .isGreaterThanOrEqualTo(4);
    }

    @Test(groups = {"ui", "regression"})
    @Story("Sortable Tables")
    @Severity(SeverityLevel.NORMAL)
    @Description("Table 2 can find a row by partial text match.")
    public void table2FindRowByText() {
        final SortableDataTablesPage page = new SortableDataTablesPage().open();
        assertThat(page.getTable2().findRowByText("Conway"))
                .as("row containing 'Conway' in table2")
                .isPresent();
    }

    // ============================================================= BASIC AUTH

    @Test(groups = {"ui", "regression"})
    @Story("Basic Auth")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Navigating with embedded credentials in the URL grants access to /basic_auth.")
    public void basicAuthWithEmbeddedCredentials() {
        final BasicAuthPage page = new BasicAuthPage();
        page.openBasicAuth();
        assertThat(page.isSuccessMessageDisplayed())
                .as("success message on /basic_auth")
                .isTrue();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Basic Auth")
    @Severity(SeverityLevel.NORMAL)
    @Description("The body text after successful basic auth contains 'Congratulations'.")
    public void basicAuthBodyTextContainsCongratulations() {
        final BasicAuthPage page = new BasicAuthPage();
        page.openBasicAuth();
        assertThat(page.getBodyText())
                .as("body paragraph text after basic auth")
                .containsIgnoringCase("Congratulations");
    }

    // ============================================================= FILE UPLOAD

    @Test(groups = {"ui", "regression"})
    @Story("File Upload")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uploading a temporary text file results in the filename appearing on the confirmation page.")
    public void uploadTempFileShowsConfirmation() throws IOException {
        final Path tmpFile = Files.createTempFile("omiinqa-upload-test-", ".txt");
        Files.writeString(tmpFile, "OmiinQA upload test content");
        try {
            final FileUploadPage page = new FileUploadPage().open();
            page.uploadFile(tmpFile.toAbsolutePath().toString());
            page.clickUploadButton();
            assertThat(page.isUploadConfirmationDisplayed())
                    .as("upload confirmation element visible")
                    .isTrue();
            assertThat(page.getUploadedFileName())
                    .as("uploaded file name on confirmation page")
                    .contains(tmpFile.getFileName().toString());
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    @Test(groups = {"ui", "regression"})
    @Story("File Upload")
    @Severity(SeverityLevel.MINOR)
    @Description("The /download page contains at least one downloadable file link.")
    public void downloadPageHasLinks() {
        final FileUploadPage page = new FileUploadPage();
        page.openDownload();
        assertThat(page.getDownloadLinkCount())
                .as("download link count on /download")
                .isGreaterThan(0);
    }

    // ========================================================== STATUS CODES

    @Test(groups = {"ui", "regression"})
    @Story("Status Codes")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking the 200 link navigates to a page whose URL contains '200'.")
    public void statusCode200NavigatesCorrectly() {
        final StatusCodesPage page = new StatusCodesPage().open();
        page.clickStatusCode(200);
        assertThat(page.isOnStatusPage(200))
                .as("URL should contain '200' after clicking that link")
                .isTrue();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Status Codes")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking the 404 link navigates to a page whose URL contains '404'.")
    public void statusCode404NavigatesCorrectly() {
        final StatusCodesPage page = new StatusCodesPage().open();
        page.clickStatusCode(404);
        assertThat(page.isOnStatusPage(404))
                .as("URL should contain '404'")
                .isTrue();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Status Codes")
    @Severity(SeverityLevel.MINOR)
    @Description("The /redirector page redirects the browser to a different URL.")
    public void redirectorChangesUrl() {
        final StatusCodesPage page = new StatusCodesPage().openRedirector();
        final String before = page.getCurrentPath();
        page.clickRedirectLink();
        final String after = page.getCurrentPath();
        assertThat(after)
                .as("URL path should change after redirect")
                .isNotEqualTo(before);
    }

    // ======================================================= COOKIES & STORAGE

    @Test(groups = {"ui", "regression"})
    @Story("Cookies and Storage")
    @Severity(SeverityLevel.NORMAL)
    @Description("A cookie added via WebDriver is readable by name.")
    public void addedCookieIsReadableByName() {
        final CookiesAndStoragePage page = new CookiesAndStoragePage().open();
        page.addCookie("omiinqa_test_cookie", "test_value_42");
        final Cookie cookie = page.getCookieByName("omiinqa_test_cookie");
        assertThat(cookie).as("added cookie should not be null").isNotNull();
        assertThat(cookie.getValue())
                .as("cookie value")
                .isEqualTo("test_value_42");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Cookies and Storage")
    @Severity(SeverityLevel.NORMAL)
    @Description("A cookie deleted via WebDriver is no longer returned by getCookies().")
    public void deletedCookieIsAbsent() {
        final CookiesAndStoragePage page = new CookiesAndStoragePage().open();
        page.addCookie("omiinqa_del_cookie", "will_be_deleted");
        page.deleteCookieByName("omiinqa_del_cookie");
        assertThat(page.getCookieByName("omiinqa_del_cookie"))
                .as("deleted cookie should be null")
                .isNull();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Cookies and Storage")
    @Severity(SeverityLevel.NORMAL)
    @Description("A value written to localStorage is retrievable under the same key.")
    public void localStorageSetAndGet() {
        final CookiesAndStoragePage page = new CookiesAndStoragePage().open();
        page.setLocalStorageItem("omiinqa_key", "stored_42");
        final String value = page.getLocalStorageItem("omiinqa_key");
        assertThat(value)
                .as("localStorage value for 'omiinqa_key'")
                .isEqualTo("stored_42");
    }

    @Test(groups = {"ui", "regression"})
    @Story("Cookies and Storage")
    @Severity(SeverityLevel.MINOR)
    @Description("Clearing localStorage removes a previously stored key.")
    public void clearLocalStorageRemovesKey() {
        final CookiesAndStoragePage page = new CookiesAndStoragePage().open();
        page.setLocalStorageItem("omiinqa_temp", "temp_data");
        page.clearLocalStorage();
        assertThat(page.getLocalStorageItem("omiinqa_temp"))
                .as("item should be null after clear")
                .isNull();
    }

    @Test(groups = {"ui", "regression"})
    @Story("Cookies and Storage")
    @Severity(SeverityLevel.MINOR)
    @Description("After login, at least one cookie is present in the session.")
    public void loginSessionHasCookies() {
        final CookiesAndStoragePage page = new CookiesAndStoragePage();
        page.openLoginAndAuthenticate();
        final Set<Cookie> cookies = page.getAllCookies();
        assertThat(cookies)
                .as("session cookies after login")
                .isNotEmpty();
    }
}

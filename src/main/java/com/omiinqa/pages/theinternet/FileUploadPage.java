package com.omiinqa.pages.theinternet;

import com.omiinqa.config.FrameworkConfig;
import com.omiinqa.core.BasePage;
import com.omiinqa.utils.WaitUtils;
import org.openqa.selenium.By;

/**
 * Page Object for the File Upload ({@code /upload}) and File Download
 * ({@code /download}) exercises on The-Internet.
 *
 * <h2>Upload flow</h2>
 * <ol>
 *   <li>Navigate with {@link #open()}.</li>
 *   <li>Supply an absolute file path to {@link #uploadFile(String)}; the file
 *       input accepts {@code sendKeys} directly — no click is needed.</li>
 *   <li>Confirm the selection and trigger the upload with
 *       {@link #clickUploadButton()}.</li>
 *   <li>Verify the result via {@link #isUploadConfirmationDisplayed()} or
 *       {@link #getUploadedFileName()}.</li>
 * </ol>
 *
 * <h2>Download flow</h2>
 * <ol>
 *   <li>Navigate with {@link #openDownload()}.</li>
 *   <li>Count available links with {@link #getDownloadLinkCount()}.</li>
 * </ol>
 *
 * <p><b>Page Object Model contract:</b></p>
 * <ul>
 *   <li>No assertions — all state is surfaced through getter methods.</li>
 *   <li>Locators are {@code private static final} {@link By} fields — declared
 *       once, named for intent, impossible to scatter or duplicate.</li>
 *   <li>Synchronisation is delegated to {@link WaitUtils}; no raw
 *       {@code Thread.sleep} calls appear here.</li>
 * </ul>
 */
public class FileUploadPage extends BasePage {

    // ---------------------------------------------------------------- locators

    /** Hidden {@code <input type="file">} element that accepts the file path. */
    private static final By FILE_INPUT = By.cssSelector("#file-upload");

    /** «Upload» submit button that initiates the file transfer to the server. */
    private static final By UPLOAD_BUTTON = By.cssSelector("#file-submit");

    /**
     * Element that displays the name of the uploaded file after a successful
     * upload ({@code #uploaded-files}).
     */
    private static final By UPLOADED_FILES = By.cssSelector("#uploaded-files");

    /** All download links listed on the {@code /download} page. */
    private static final By DOWNLOAD_LINKS = By.cssSelector(".example a");

    // ----------------------------------------------------------------- actions

    /**
     * Navigates the browser to the File Upload page ({@code /upload}) as
     * configured in {@link FrameworkConfig#appUrl(String)}.
     *
     * @return this {@link FileUploadPage} for method chaining
     */
    public FileUploadPage open() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/upload";
        log.info("Opening File Upload page: {}", url);
        driver().get(url);
        waitForUrlContains("/upload");
        return this;
    }

    /**
     * Navigates the browser to the File Download page ({@code /download}) as
     * configured in {@link FrameworkConfig#appUrl(String)}.
     *
     * @return this {@link FileUploadPage} for method chaining
     */
    public FileUploadPage openDownload() {
        final String url = FrameworkConfig.get().appUrl("theinternet") + "/download";
        log.info("Opening File Download page: {}", url);
        driver().get(url);
        waitForUrlContains("/download");
        return this;
    }

    /**
     * Supplies the absolute path of a local file to the hidden
     * {@code <input type="file">} element via {@code sendKeys}.
     *
     * <p>The file input on this page accepts a path through keyboard simulation
     * directly — no click on the element is required (and no OS file-picker
     * dialog is opened). The path must be the <em>absolute</em> path on the
     * machine running the browser.</p>
     *
     * @param absoluteFilePath the absolute filesystem path of the file to upload
     *                         (e.g. {@code "/tmp/test-file.txt"})
     * @return this {@link FileUploadPage} for method chaining
     */
    public FileUploadPage uploadFile(final String absoluteFilePath) {
        log.info("Sending file path '{}' to file input", absoluteFilePath);
        WaitUtils.present(driver(), FILE_INPUT).sendKeys(absoluteFilePath);
        return this;
    }

    /**
     * Clicks the {@code #file-submit} upload button to submit the selected file
     * to the server.
     *
     * <p>After clicking, wait for {@link #isUploadConfirmationDisplayed()} or
     * {@link #getUploadedFileName()} to verify the result.</p>
     *
     * @return this {@link FileUploadPage} for method chaining
     */
    public FileUploadPage clickUploadButton() {
        log.info("Clicking Upload button");
        click(UPLOAD_BUTTON);
        return this;
    }

    // ----------------------------------------------------------------- queries

    /**
     * Returns the trimmed text content of the {@code #uploaded-files} element,
     * which contains the filename of the most recently uploaded file.
     *
     * <p>Call this method after {@link #clickUploadButton()} has completed and
     * the confirmation element is visible.</p>
     *
     * @return the uploaded file name text; never {@code null}
     */
    public String getUploadedFileName() {
        return getText(UPLOADED_FILES);
    }

    /**
     * Returns {@code true} when the {@code #uploaded-files} confirmation element
     * is visible in the viewport.
     *
     * <p>Uses the framework's default 3-second probe window for optional-element
     * visibility checks.</p>
     *
     * @return {@code true} if the upload confirmation is visible,
     *         {@code false} otherwise
     */
    public boolean isUploadConfirmationDisplayed() {
        return isDisplayed(UPLOADED_FILES);
    }

    /**
     * Returns the number of downloadable file links present on the
     * {@code /download} page.
     *
     * <p>Navigate to the download page first by calling {@link #openDownload()};
     * this method does not navigate automatically.</p>
     *
     * @return non-negative count of {@code .example a} anchor elements
     */
    public int getDownloadLinkCount() {
        return findAll(DOWNLOAD_LINKS).size();
    }
}

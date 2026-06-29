package com.omiinqa.components;

import com.omiinqa.core.BaseComponent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * SauceDemo footer component, encapsulating social links and the copyright
 * text that appear at the bottom of every authenticated page.
 *
 * <p><b>Design rationale:</b> Footer content is identical across all SauceDemo
 * pages. Wrapping it in a {@link BaseComponent} means verifications like
 * {@link #getCopyrightText()} are written once and reused by any page test that
 * needs them, without duplicating locators (DRY). The component is scoped to the
 * {@code <footer>} element, so its locators cannot bleed into the body.</p>
 */
public class FooterComponent extends BaseComponent {

    private static final By COPY_TEXT       = By.className("footer_copy");
    private static final By TWITTER_LINK    = By.cssSelector(".social_twitter a");
    private static final By FACEBOOK_LINK   = By.cssSelector(".social_facebook a");
    private static final By LINKEDIN_LINK   = By.cssSelector(".social_linkedin a");

    /**
     * @param root the {@code <footer>} element supplied by the hosting page
     */
    public FooterComponent(final WebElement root) {
        super(root);
    }

    // ----------------------------------------------------------------- queries

    /**
     * @return the copyright string displayed in the footer, trimmed of whitespace
     */
    public String getCopyrightText() {
        return findInRoot(COPY_TEXT).getText().trim();
    }

    /**
     * @return the href value of the Twitter social link
     */
    public String getTwitterHref() {
        return findInRoot(TWITTER_LINK).getAttribute("href");
    }

    /**
     * @return the href value of the Facebook social link
     */
    public String getFacebookHref() {
        return findInRoot(FACEBOOK_LINK).getAttribute("href");
    }

    /**
     * @return the href value of the LinkedIn social link
     */
    public String getLinkedInHref() {
        return findInRoot(LINKEDIN_LINK).getAttribute("href");
    }
}

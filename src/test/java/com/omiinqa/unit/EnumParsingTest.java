package com.omiinqa.unit;

import com.omiinqa.driver.options.BrowserOptionsStrategy;
import com.omiinqa.driver.options.ChromeOptionsStrategy;
import com.omiinqa.driver.options.EdgeOptionsStrategy;
import com.omiinqa.driver.options.FirefoxOptionsStrategy;
import com.omiinqa.driver.options.OptionsStrategyFactory;
import com.omiinqa.enums.BrowserType;
import com.omiinqa.enums.Environment;
import com.omiinqa.enums.ExecutionMode;
import org.openqa.selenium.MutableCapabilities;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Offline unit tests for enums and the options-strategy factory. */
public class EnumParsingTest {

    @Test(groups = {"unit", "smoke"})
    public void browserTypeParsesAliasesAndSeparators() {
        assertThat(BrowserType.from("chrome")).isEqualTo(BrowserType.CHROME);
        assertThat(BrowserType.from("REMOTE_CHROME")).isEqualTo(BrowserType.REMOTE_CHROME);
        assertThat(BrowserType.from("remote-firefox")).isEqualTo(BrowserType.REMOTE_FIREFOX);
        assertThat(BrowserType.from(null)).isEqualTo(BrowserType.CHROME);
        assertThat(BrowserType.REMOTE_EDGE.isRemote()).isTrue();
        assertThat(BrowserType.CHROME.isRemote()).isFalse();
    }

    @Test(groups = {"unit", "smoke"})
    public void browserTypeRejectsUnknown() {
        assertThatThrownBy(() -> BrowserType.from("safari"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test(groups = {"unit", "smoke"})
    public void environmentAndModeParse() {
        assertThat(Environment.from("qa")).isEqualTo(Environment.QA);
        assertThat(Environment.from(null)).isEqualTo(Environment.QA);
        assertThat(Environment.QA.fileName()).isEqualTo("qa.properties");
        assertThat(ExecutionMode.from("grid")).isEqualTo(ExecutionMode.GRID);
        assertThat(ExecutionMode.from(null)).isEqualTo(ExecutionMode.LOCAL);
    }

    @Test(groups = {"unit", "smoke"})
    public void factoryReturnsMatchingStrategyAndBuildsCapabilities() {
        assertThat(OptionsStrategyFactory.forBrowser(BrowserType.CHROME))
                .isInstanceOf(ChromeOptionsStrategy.class);
        assertThat(OptionsStrategyFactory.forBrowser(BrowserType.FIREFOX))
                .isInstanceOf(FirefoxOptionsStrategy.class);
        assertThat(OptionsStrategyFactory.forBrowser(BrowserType.REMOTE_EDGE))
                .isInstanceOf(EdgeOptionsStrategy.class);

        final BrowserOptionsStrategy strategy =
                OptionsStrategyFactory.forBrowser(BrowserType.CHROME);
        final MutableCapabilities caps = strategy.build(true);
        assertThat(caps.getBrowserName()).isEqualTo("chrome");
    }
}

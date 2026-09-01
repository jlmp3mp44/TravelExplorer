package com.travel.explorer.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.*;
import com.travel.explorer.config.TestConfig;
import com.travel.explorer.ui.login.LoginPage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.event.annotation.BeforeTestClass;

public class BaseTest {
  protected static Playwright playwright;
  protected static Browser browser;
  protected BrowserContext context;
  protected Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
  }

  @BeforeEach
  void createContextAndPage() {
    context = browser.newContext();
    page = context.newPage();
  }

  @AfterEach
  void closeContext() {
    context.close();
  }

  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  public void navigate(String url) {
    page.navigate(url);
  }

  protected void loginSuccessfully() {
    LoginPage loginPage = new LoginPage(page);

    loginPage.navigate();

    loginPage.loginUser(ExistingUser.USERNAME, ExistingUser.PASSWORD);

    loginPage.waitRedirectToMainPage();

    assertThat(page).hasURL(TestConfig.getBaseUrl() + "/");
  }
}
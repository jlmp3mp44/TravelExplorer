package com.travel.explorer.ui;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

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
}
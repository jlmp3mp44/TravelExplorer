package com.travel.explorer.ui.login;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class LoginPage {
  private final Page page;

  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator loginButton;
  private final Locator errorMessage;

  public LoginPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByLabel("Username");
    this.passwordInput = page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true));
    this.loginButton = page.locator("button[type='submit']");
    this.errorMessage =  page.getByRole(AriaRole.ALERT);
  }

  public void navigate() {
    page.navigate("http://localhost:3000/login");
  }

  public void loginUser(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    loginButton.click();
  }

  public void waitRedirectToMainPage(){
    page.waitForURL("http://localhost:3000/", new Page.WaitForURLOptions().setTimeout(15000));
  }

  public Locator getErrorMessage(){
    return errorMessage;
  }


}

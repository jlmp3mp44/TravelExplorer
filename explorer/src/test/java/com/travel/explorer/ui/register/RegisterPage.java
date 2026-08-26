package com.travel.explorer.ui.register;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class RegisterPage {
  private final Page page;

  private final Locator usernameInput;
  private final Locator emailInput;
  private final Locator phoneInput;
  private final Locator passwordInput;
  private final Locator confirmPasswordInput;
  private final Locator createAccountButton;

  public RegisterPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByLabel("Username");
    this.emailInput = page.getByLabel("Email");
    this.phoneInput = page.locator("#register-phone");
    this.passwordInput = page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true));
    this.confirmPasswordInput = page.getByLabel("Confirm password", new Page.GetByLabelOptions().setExact(true));
    this.createAccountButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create account"));
  }

  public void navigate() {
    page.navigate("http://localhost:3000/register");
  }

  public void registerUser(String username, String email, String phone, String password) {
    usernameInput.fill(username);
    emailInput.fill(email);
    phoneInput.fill(phone);
    passwordInput.fill(password);
    confirmPasswordInput.fill(password);
    createAccountButton.click();
  }

  public void waitRedirectToLogin(){
    page.waitForURL("**/login", new Page.WaitForURLOptions().setTimeout(15000));
  }
}

package com.travel.explorer;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import com.travel.explorer.ui.ExistingUser;
import com.travel.explorer.ui.login.LoginPage;

public class BasePage {

  private final Page page;

  public BasePage(Page page) {
    this.page = page;
  }

  void loginSuccessfully(){
    LoginPage loginPage = new LoginPage(page);


    loginPage.navigate();
    loginPage.loginUser(ExistingUser.USERNAME, ExistingUser.PASSWORD);

    loginPage.waitRedirectToMainPage();
    assertThat(page).hasURL("http://localhost:3000/");
  }

}

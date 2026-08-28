package com.travel.explorer.ui.login;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.travel.explorer.ui.BaseTest;
import com.travel.explorer.ui.ExistingUser;
import com.travel.explorer.ui.register.RegisterPage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class LoginTest extends BaseTest {

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.ui.login.LoginDataProvider#existingLoginData")
  void loginSuccessfully(String testName, String usernameOrEmail, String password) {
    LoginPage loginPage = new LoginPage(page);


    loginPage.navigate();
    loginPage.loginUser(usernameOrEmail, password);

    loginPage.waitRedirectToMainPage();
    assertThat(page).hasURL("http://localhost:3000/");
  }

}

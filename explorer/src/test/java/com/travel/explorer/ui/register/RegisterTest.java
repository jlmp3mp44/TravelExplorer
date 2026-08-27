package com.travel.explorer.ui.register;

import com.travel.explorer.ui.BaseTest;
import com.travel.explorer.ui.register.RegisterPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RegisterTest extends BaseTest {

  @Test
  void shouldRegisterSuccessfully() {
    RegisterPage registerPage = new RegisterPage(page);

    String uniqueEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

    registerPage.navigate();
    String uniqueId = String.valueOf(System.currentTimeMillis());
    registerPage.registerUser("TestUser" + uniqueId, uniqueEmail, "+380670000000", "SecurePass123!", "SecurePass123!");

    registerPage.waitRedirectToLogin();
    assertThat(page).hasURL("http://localhost:3000/login");
  }


  @Test
  void registrationExistingEmail() {
    RegisterPage registerPage = new RegisterPage(page);
    String existingEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    String uniqueId = String.valueOf(System.currentTimeMillis());

    registerPage.navigate();
    registerPage.registerUser("TestUser1" + uniqueId, existingEmail, "+380670000000", "SecurePass123!", "SecurePass123!");
    registerPage.waitRedirectToLogin();

    registerPage.navigate();
    registerPage.registerUser("TestUser2" + uniqueId, existingEmail, "+380670000001", "SecurePass123!", "SecurePass123!");

    assertThat(registerPage.getErrorMessage()).hasText("Email already taken!");
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource("com.travel.explorer.ui.register.RegisterDataProvider#invalidRegistrationData")
  void registrationIncorrectInputData(
      String testName,
      String username,
      String email,
      String phone,
      String password,
      String confirmPassword,
      String expectedError
  ) {
    RegisterPage registerPage = new RegisterPage(page);

    registerPage.navigate();
    registerPage.registerUser(username, email, phone, password, confirmPassword);

    assertThat(registerPage.getErrorMessage()).hasText(expectedError);
  }

}

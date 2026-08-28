package com.travel.explorer.ui.register;

import com.travel.explorer.ui.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RegisterTest extends BaseTest {

  RegisterExpectedData expectedData =  new RegisterExpectedData();

  @Test
  void shouldRegisterSuccessfully() {
    RegisterPage registerPage = new RegisterPage(page);


    registerPage.navigate();
    registerPage.registerUser("Test123", "Test123@test.com", "+380670000000", "SecurePass123!", "SecurePass123!");

    registerPage.waitRedirectToLogin();
    assertThat(page).hasURL("http://localhost:3000/login");
  }

  @ParameterizedTest(name = "{index} - {0}")
  @MethodSource(
      "com.travel.explorer.ui.register.RegisterDataProvider#existingRegistrationData"
  )
  void registrationExistingData(
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

    registerPage.registerUser(
        username,
        email,
        phone,
        password,
        confirmPassword
    );

    assertThat(registerPage.getErrorMessage())
        .hasText(expectedError);
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

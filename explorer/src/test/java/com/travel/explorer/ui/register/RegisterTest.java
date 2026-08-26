package com.travel.explorer.ui.register;

import com.travel.explorer.ui.BaseTest;
import com.travel.explorer.ui.register.RegisterPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RegisterTest extends BaseTest {

  @Test
  @DisplayName("Успішна реєстрація нового користувача з унікальними даними")
  void shouldRegisterSuccessfully() {
    RegisterPage registerPage = new RegisterPage(page);

    String uniqueEmail = "user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

    registerPage.navigate();
    registerPage.registerUser("TestUser2", uniqueEmail, "+380670000000", "SecurePass123!");

    registerPage.waitRedirectToLogin();
    assertThat(page).hasURL("http://localhost:3000/login");
  }
}

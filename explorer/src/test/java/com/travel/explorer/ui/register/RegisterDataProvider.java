package com.travel.explorer.ui.register;


import com.travel.explorer.ui.ExistingUser;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

public class RegisterDataProvider {

  static RegisterExpectedData expectedData =  new RegisterExpectedData();

  public static Stream<Arguments> invalidRegistrationData() {
    String uniqueId = String.valueOf(System.currentTimeMillis());
    String username = "TestUser" + uniqueId;
    return Stream.of(
        Arguments.of(
            "Invalid Email Format",
            username,
            "invalid-email",
            "+380670000000",
            "SecurePass123!",
            "SecurePass123!",
            "Enter a valid email address."
        ),
        Arguments.of(
            "Password Too Short",
            username,
            "valid@test.com",
            "+380670000000",
            "123",
            "123",
            "Password must be at least 8 characters."
        ),
        Arguments.of(
            "Passwords Do Not Match",
            username,
            "valid2@test.com",
            "+380670000000",
            "SecurePass123!",
            "DifferentPass123!",
            "Passwords do not match. Type the same password twice."
        )
    );
  }

  public static Stream<Arguments> existingRegistrationData() {
    String username = expectedData.generateUsername();
    String email = expectedData.generateEmail();

    return Stream.of(

        Arguments.of(
            "Existing email",
            username,
            ExistingUser.EMAIL,
            "+380000000000",
            "SecurePass!123",
            "SecurePass!123",
            "Email already taken!"
        ),

        Arguments.of(
            "Existing email",
            ExistingUser.USERNAME,
            email,
            "+380000000000",
            "SecurePass!123",
            "SecurePass!123",
            "Username already taken!"
        )
    );
  }
}

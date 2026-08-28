package com.travel.explorer.ui.login;

import com.travel.explorer.ui.ExistingUser;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class LoginDataProvider {

  public static Stream<Arguments> existingLoginData() {

    return Stream.of(

        Arguments.of(
            "with username",
            ExistingUser.USERNAME,
            ExistingUser.PASSWORD
        ),

        Arguments.of(
            "with email",
            ExistingUser.EMAIL,
            ExistingUser.PASSWORD
        )
    );
  }

  public static Stream<Arguments> incorrectLoginData() {

    return Stream.of(

        Arguments.of(
            "Empty username",
            "",
            ExistingUser.PASSWORD,
            "Enter your email or username."
        ),

        Arguments.of(
            "Empty password",
            ExistingUser.EMAIL,
            "",
            "Enter your password."
        ),
        Arguments.of(
            "Incorrect password",
            ExistingUser.EMAIL,
            ExistingUser.PASSWORD + "1",
            "Incorrect username or password. Check for typos and that Caps Lock is off, then try again."
        ),
        Arguments.of(
            "non existent user",
            ExistingUser.USERNAME+ UUID.randomUUID().toString().substring(0, 8),
            ExistingUser.PASSWORD,
            "Incorrect username or password. Check for typos and that Caps Lock is off, then try again."
        )
    );
  }
}

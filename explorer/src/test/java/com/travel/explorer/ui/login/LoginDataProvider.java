package com.travel.explorer.ui.login;

import com.travel.explorer.ui.ExistingUser;
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
}

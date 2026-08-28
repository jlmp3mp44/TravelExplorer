package com.travel.explorer.ui.register;

import java.util.UUID;

public class RegisterExpectedData {

  public String generateEmail() {
    return "user_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
  }

  public String generateUsername() {
    return "User_" + UUID.randomUUID().toString().substring(0, 8);
  }

}

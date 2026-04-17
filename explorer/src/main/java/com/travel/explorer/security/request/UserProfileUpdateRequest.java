package com.travel.explorer.security.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

  /** Optional; omit or empty string clears the phone. E.164-style digits/plus allowed. */
  @Size(max = 32)
  @Pattern(regexp = "^$|^\\+?[0-9\\s().-]{6,32}$")
  private String phoneNumber;
}

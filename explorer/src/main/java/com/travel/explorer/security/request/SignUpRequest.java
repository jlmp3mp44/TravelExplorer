package com.travel.explorer.security.request;

import com.travel.explorer.entities.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;

@Data
public class SignUpRequest {
  @NotBlank
  @Size(min = 3, max = 22, message = "Username should contains between 3 and 10 characters")
  private String username;
  @NotBlank
  @Email
  private String email;
  @NotBlank
  @Size(min = 8, max = 126, message = "Password should contains between 8 and 126 characters")
  private String password;
  private Set<String> roles;

}

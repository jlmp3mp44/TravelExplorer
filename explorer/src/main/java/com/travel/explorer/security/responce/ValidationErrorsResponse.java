package com.travel.explorer.security.responce;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorsResponse {

  private List<String> errors = new ArrayList<>();
}

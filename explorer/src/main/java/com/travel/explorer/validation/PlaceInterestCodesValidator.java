package com.travel.explorer.validation;

import com.travel.explorer.place.PlaceInterestType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class PlaceInterestCodesValidator
    implements ConstraintValidator<ValidPlaceInterestCodes, List<String>> {

  @Override
  public boolean isValid(List<String> value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    for (String s : value) {
      if (s == null || s.isBlank()) {
        return false;
      }
      if (!PlaceInterestType.isAllowedCode(s.trim())) {
        return false;
      }
    }
    return true;
  }
}

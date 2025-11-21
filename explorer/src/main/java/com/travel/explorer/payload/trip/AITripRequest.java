package com.travel.explorer.payload.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AITripRequest {
  private Date startDate;
  private Date endDate;
  private Long budget;
  private Long numberOfDays;
  private List<String> desiredCountries;
  private List<String> interests;
}


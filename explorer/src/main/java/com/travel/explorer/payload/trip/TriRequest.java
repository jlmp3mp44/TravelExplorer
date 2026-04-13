package com.travel.explorer.payload.trip;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriRequest {
  @NotNull
  @JsonFormat(pattern="yyyy-MM-dd")
  private LocalDate startDate;

  @NotNull
  @JsonFormat(pattern="yyyy-MM-dd")
  private LocalDate endDate;
  private String country;
  private String city;
  private List<Long> cityIds;
  @NotNull
  private Integer budget;
  private List<String> interests;
}


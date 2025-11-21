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
public class TriRequest {
  @NotBlank
  private String title;
  private String desc;
  @NotNull
  private Date startDate;
  @NotNull
  private Date endDate;
  private List<Long> placeIds;
}


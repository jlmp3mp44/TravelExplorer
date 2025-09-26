package com.travel.explorer.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.sql.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripDTO {
  @NotBlank
  private String title;
  private String desc;
  @NotNull
  private Date startDate;
  @NotNull
  private Date endDate;
  private List<Long> placeIds;
}


package com.travel.explorer.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.travel.explorer.entities.Activity;
import com.travel.explorer.entities.Trip;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayResponse {
  private LocalDate date;
  private List<ActivityResponse> activities = new ArrayList<>();

}

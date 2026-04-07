package com.travel.explorer.payload;

import com.travel.explorer.entities.Day;
import com.travel.explorer.entities.Place;
import com.travel.explorer.payload.place.PlaceResponse;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityResponse {
  private List<PlaceResponse> places =  new ArrayList<>();
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}

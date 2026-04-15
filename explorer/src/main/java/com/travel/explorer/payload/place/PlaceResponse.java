package com.travel.explorer.payload.place;

import com.travel.explorer.entities.Location;
import com.travel.explorer.entities.Trip;
import jakarta.persistence.ManyToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {
  private String title;
  private Location location;

}

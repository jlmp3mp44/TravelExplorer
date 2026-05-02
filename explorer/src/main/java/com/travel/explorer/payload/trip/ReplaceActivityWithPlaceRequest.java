package com.travel.explorer.payload.trip;

import com.travel.explorer.entities.ActivityChangeReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplaceActivityWithPlaceRequest {

  @NotNull private Long placeId;

  private ActivityChangeReason reason;
}

package com.travel.explorer.payload.trip;

import com.travel.explorer.entities.ActivityChangeReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplaceActivitySmartRequest {

  /** Optional audit reason; defaults when null. */
  private ActivityChangeReason reason;
}

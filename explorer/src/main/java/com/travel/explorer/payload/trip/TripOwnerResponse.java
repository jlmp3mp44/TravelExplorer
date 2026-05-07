package com.travel.explorer.payload.trip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripOwnerResponse {

  private Long id;
  private String username;
  private String email;
  private String phoneNumber;
}

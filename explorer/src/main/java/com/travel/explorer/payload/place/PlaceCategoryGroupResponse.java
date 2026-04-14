package com.travel.explorer.payload.place;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceCategoryGroupResponse {

  /** Stable id, e.g. {@code culture} — use with {@code GET .../groups/{groupId}}. */
  private String groupId;
  private String title;
  private List<PlaceCategoryItemResponse> interests;
}

package com.travel.explorer.controller;

import com.travel.explorer.excpetions.ResourceNotFoundException;
import com.travel.explorer.payload.place.PlaceCategoryGroupResponse;
import com.travel.explorer.payload.place.PlaceCategoryItemResponse;
import com.travel.explorer.place.PlaceInterestGroup;
import com.travel.explorer.place.PlaceInterestType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/public/place-categories")
public class PlaceCategoryController {

  /** Full catalog: grouped by category (culture, nature, …). */
  @GetMapping
  public ResponseEntity<List<PlaceCategoryGroupResponse>> listCatalog() {
    Map<PlaceInterestGroup, List<PlaceInterestType>> grouped =
        PlaceInterestType.groupedByGroup();
    List<PlaceCategoryGroupResponse> out = new ArrayList<>();
    for (PlaceInterestGroup g : PlaceInterestType.groupsInOrder()) {
      List<PlaceInterestType> types = grouped.get(g);
      if (types == null || types.isEmpty()) {
        continue;
      }
      List<PlaceCategoryItemResponse> items =
          types.stream()
              .map(t -> new PlaceCategoryItemResponse(t.getCode(), t.getLabel()))
              .toList();
      out.add(new PlaceCategoryGroupResponse(g.getId(), g.getTitle(), items));
    }
    return ResponseEntity.ok(out);
  }

  /** Interests for one group only (e.g. after user picks “Culture”). */
  @GetMapping("/groups/{groupId}")
  public ResponseEntity<List<PlaceCategoryItemResponse>> listGroupInterests(
      @PathVariable String groupId) {
    PlaceInterestGroup group = PlaceInterestGroup.fromId(groupId);
    if (group == null) {
      throw new ResourceNotFoundException("Place category group", "groupId", groupId);
    }
    List<PlaceCategoryItemResponse> items =
        PlaceInterestType.interestsInGroup(group).stream()
            .map(t -> new PlaceCategoryItemResponse(t.getCode(), t.getLabel()))
            .toList();
    return ResponseEntity.ok(items);
  }
}

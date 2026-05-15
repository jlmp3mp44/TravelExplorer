package com.travel.explorer.controller;

import com.travel.explorer.excpetions.APIException;
import com.travel.explorer.payload.place.InterestingPlaceResponse;
import com.travel.explorer.payload.place.SaveInterestingPlaceRequest;
import com.travel.explorer.security.service.UserDetailsImpl;
import com.travel.explorer.service.InterestingPlaceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/interesting-places")
public class InterestingPlaceController {

  @Autowired private InterestingPlaceService service;

  @GetMapping
  public ResponseEntity<List<InterestingPlaceResponse>> list(Authentication authentication) {
    return ResponseEntity.ok(service.list(currentUserId(authentication)));
  }

  @PostMapping
  public ResponseEntity<InterestingPlaceResponse> save(
      @Valid @RequestBody SaveInterestingPlaceRequest request, Authentication authentication) {
    InterestingPlaceResponse response = service.save(currentUserId(authentication), request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
    service.delete(currentUserId(authentication), id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/match")
  public ResponseEntity<List<InterestingPlaceResponse>> match(
      @RequestParam(required = false) Long cityId,
      @RequestParam(required = false) Long countryId,
      Authentication authentication) {
    return ResponseEntity.ok(
        service.findMatching(currentUserId(authentication), cityId, countryId));
  }

  private static Long currentUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
      throw new APIException("Sign in is required");
    }
    return ((UserDetailsImpl) authentication.getPrincipal()).getId();
  }
}

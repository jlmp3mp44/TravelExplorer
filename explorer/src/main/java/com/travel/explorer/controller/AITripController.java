package com.travel.explorer.controller;

import com.travel.explorer.payload.trip.AITripRequest;
import com.travel.explorer.payload.trip.AITripResponce;
import com.travel.explorer.service.AITripService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/premium/ai/trips")
public class AITripController {

  @Autowired
  private AITripService aiTripService;

  @PostMapping()
  public ResponseEntity<AITripResponce> planTrip(@RequestBody AITripRequest request) {
    String plan = aiTripService.generateTripPlan(aiTripService.buildPrompt(request));
    AITripResponce responce = aiTripService.parseTripPlan(plan);
    return new ResponseEntity<>(responce, HttpStatus.CREATED);
  }

  @GetMapping()
  public ResponseEntity<?> planTkrip() {

    return ResponseEntity.ok("");
  }
}

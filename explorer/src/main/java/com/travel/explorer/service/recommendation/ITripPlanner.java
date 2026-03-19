package com.travel.explorer.service.recommendation;

import com.travel.explorer.entities.Trip;
import com.travel.explorer.payload.trip.TripCreationRequest;

public interface ITripPlanner {
    public Trip plan(TripCreationRequest request);
}

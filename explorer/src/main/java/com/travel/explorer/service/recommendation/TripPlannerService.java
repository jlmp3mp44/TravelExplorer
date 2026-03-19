package com.travel.explorer.service.recommendation;

import com.travel.explorer.entities.Trip;
import com.travel.explorer.entities.VectorScoredPlace;
import com.travel.explorer.payload.trip.TripCreationRequest;


import java.util.List;


public class TripPlannerService implements ITripPlanner {

    public Trip plan(TripCreationRequest request)
    {
        List<VectorScoredPlace> vectorScoredPlaces = getVectorScoredPlaces(request);
        
        return new Trip();
    }

    private List<VectorScoredPlace> getVectorScoredPlaces(TripCreationRequest request)
    {
        // get from db
        List<VectorScoredPlace> vectorScoredPlaces = null;
        return vectorScoredPlaces;
    }
}

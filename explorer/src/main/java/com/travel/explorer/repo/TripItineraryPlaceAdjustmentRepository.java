package com.travel.explorer.repo;

import com.travel.explorer.entities.TripItineraryPlaceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripItineraryPlaceAdjustmentRepository
    extends JpaRepository<TripItineraryPlaceAdjustment, Long> {}

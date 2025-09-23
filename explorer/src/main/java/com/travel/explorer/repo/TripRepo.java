package com.travel.explorer.repo;

import com.travel.explorer.entities.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepo  extends JpaRepository<Trip, Long> {

}

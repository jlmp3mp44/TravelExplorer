package com.travel.explorer.repo;

import com.travel.explorer.entities.TripRating;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRatingRepository extends JpaRepository<TripRating, Long> {

  Optional<TripRating> findByUser_UserIdAndTrip_Id(Long userId, Long tripId);

  @Query("SELECT AVG(r.stars) FROM TripRating r WHERE r.trip.id = :tripId")
  Optional<Double> averageStarsByTripId(@Param("tripId") Long tripId);

  long countByTrip_Id(Long tripId);
}

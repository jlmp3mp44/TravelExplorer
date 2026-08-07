package com.travel.explorer.repo;

import com.travel.explorer.entities.TripRating;
import java.util.Collection;
import java.util.List;
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

  @Query(
      """
      SELECT r.trip.id, AVG(r.stars), COUNT(r)
      FROM TripRating r
      GROUP BY r.trip.id
      """)
  List<Object[]> aggregateRatingStatsByTripId();

  @Query(
      """
      SELECT r.trip.id, AVG(r.stars), COUNT(r)
      FROM TripRating r
      WHERE r.trip.id IN :tripIds
      GROUP BY r.trip.id
      """)
  List<Object[]> aggregateRatingStatsByTripIds(@Param("tripIds") Collection<Long> tripIds);
}

package com.travel.explorer.repo;

import com.travel.explorer.entities.PlaceRating;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceRatingRepository extends JpaRepository<PlaceRating, Long> {

  Optional<PlaceRating> findByUser_UserIdAndPlace_Id(Long userId, Long placeId);

  @Query("SELECT AVG(r.stars) FROM PlaceRating r WHERE r.place.id = :placeId")
  Optional<Double> averageStarsByPlaceId(@Param("placeId") Long placeId);

  long countByPlace_Id(Long placeId);

  @Query("SELECT DISTINCT r FROM PlaceRating r JOIN FETCH r.user JOIN FETCH r.place")
  List<PlaceRating> findAllWithUserAndPlace();
}

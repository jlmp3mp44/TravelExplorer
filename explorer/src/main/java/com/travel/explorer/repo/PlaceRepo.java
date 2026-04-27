package com.travel.explorer.repo;

import com.travel.explorer.entities.Place;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceRepo extends JpaRepository<Place, Long> {

  Optional<Place> findByTitle(String title);

  Optional<Place> findByGooglePlaceId(String googlePlaceId);

  List<Place> findAllByGooglePlaceIdIn(Collection<String> googlePlaceIds);

  @Query("SELECT DISTINCT p FROM Place p " +
      "JOIN p.categories c " +
      "JOIN p.location l " +
      "WHERE c.name IN :categoryNames " +
      "AND l.lat BETWEEN :minLat AND :maxLat " +
      "AND l.lng BETWEEN :minLng AND :maxLng")
  List<Place> findByCategoryNamesAndLocationBounds(
      @Param("categoryNames") List<String> categoryNames,
      @Param("minLat") double minLat,
      @Param("maxLat") double maxLat,
      @Param("minLng") double minLng,
      @Param("maxLng") double maxLng);
}
